package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ChessAi
import com.example.engine.GameState
import com.example.model.GameStatus
import com.example.model.PlayerSide
import java.util.Locale
import kotlin.math.exp

/**
 * Stockfish-style live evaluation bar for Diagonal Chess.
 * Displays real-time positional and tactical balance between White and Black.
 * 
 * Fits seamlessly docked inside the board corner negative space without shrinking the board.
 */
@Composable
fun EvaluationBar(
    state: GameState,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false
) {
    // Score from White's perspective in centipawns
    val evalCp = when (state.status) {
        GameStatus.WHITE_WON -> 10000
        GameStatus.BLACK_WON -> -10000
        else -> ChessAi.evaluateStatic(state, PlayerSide.WHITE)
    }

    // Convert centipawns to probability fraction for White [0.0..1.0] using smooth logistic curve
    val rawFraction = when {
        evalCp >= 9000 -> 1.0f
        evalCp <= -9000 -> 0.0f
        else -> {
            val exponent = (-evalCp.toDouble() / 160.0).coerceIn(-15.0, 15.0)
            (1.0 / (1.0 + exp(exponent))).toFloat().coerceIn(0.04f, 0.96f)
        }
    }

    // If board is flipped, invert visual perspective so player on bottom matches bottom bar
    val bottomFraction = if (isFlipped) (1.0f - rawFraction) else rawFraction

    val animatedBottomFraction by animateFloatAsState(
        targetValue = bottomFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "eval_bar_anim"
    )

    // Formatted evaluation text (e.g. +1.5, -0.8, M, 0.0)
    val evalText = when {
        evalCp >= 9000 -> if (isFlipped) "-M" else "+M"
        evalCp <= -9000 -> if (isFlipped) "+M" else "-M"
        else -> {
            val pawnUnits = evalCp / 100.0
            val displayUnits = if (isFlipped) -pawnUnits else pawnUnits
            if (displayUnits >= 0) {
                String.format(Locale.US, "+%.1f", displayUnits)
            } else {
                String.format(Locale.US, "%.1f", displayUnits)
            }
        }
    }

    val topColor1 = if (isFlipped) Color(0xFFEDEDF5) else Color(0xFF14141E)
    val topColor2 = if (isFlipped) Color(0xFFFFFFFF) else Color(0xFF28283C)

    val bottomColor1 = if (isFlipped) Color(0xFF28283C) else Color(0xFFEDEDF5)
    val bottomColor2 = if (isFlipped) Color(0xFF14141E) else Color(0xFFFFFFFF)

    val isBottomAdvantage = bottomFraction >= 0.5f

    Box(
        modifier = modifier
            .width(22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0C14))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.55f),
                        Color(0xFFFFD700).copy(alpha = 0.55f)
                    )
                ),
                RoundedCornerShape(8.dp)
            )
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .testTag("evaluation_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Vertical Split Bar
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // Top Section (Black default / White if flipped)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight((1f - animatedBottomFraction).coerceAtLeast(0.01f))
                    .background(
                        Brush.verticalGradient(
                            listOf(topColor1, topColor2)
                        )
                    )
            )

            // Bottom Section (White default / Black if flipped)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(animatedBottomFraction.coerceAtLeast(0.01f))
                    .background(
                        Brush.verticalGradient(
                            listOf(bottomColor1, bottomColor2)
                        )
                    )
            )
        }

        // Center 0.0 Equilibrium Divider Tick
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFFFD700).copy(alpha = 0.6f))
        )

        // Overlay Score Label positioned on dominant half with strict single-line constraints
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBottomAdvantage) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = evalText,
                    color = if (bottomColor2 == Color(0xFFFFFFFF)) Color(0xFF0C0C14) else Color(0xFFEDEDF5),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            } else {
                Text(
                    text = evalText,
                    color = if (topColor1 == Color(0xFFEDEDF5)) Color(0xFF0C0C14) else Color(0xFFEDEDF5),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

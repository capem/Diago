package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Piece
import com.example.model.PlayerSide

@Composable
fun PieceView(
    piece: Piece,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "piece_anim")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val isWhite = piece.player == PlayerSide.WHITE

    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 2.dp.toPx()

            // 1. Selection aura / glow
            if (isSelected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.65f), Color.Transparent),
                        center = center,
                        radius = radius * 1.45f * pulseGlow
                    ),
                    radius = radius * 1.35f,
                    center = center
                )
            }

            // 2. Drop Shadow for 3D Checker piece
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = radius,
                center = Offset(center.x, center.y + 4.dp.toPx())
            )

            // 3. Base Checker Gradient (Ivory White or Obsidian Black)
            val baseColors = if (isWhite) {
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF5EBE1),
                    Color(0xFFD6C3B2),
                    Color(0xFF8F7662)
                )
            } else {
                listOf(
                    Color(0xFF4A4458),
                    Color(0xFF2A2338),
                    Color(0xFF140F20),
                    Color(0xFF07040C)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = baseColors,
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                    radius = radius * 1.1f
                ),
                radius = radius,
                center = center
            )

            // 4. Outer Rim Groove
            drawCircle(
                color = if (isWhite) Color(0xFFC7B19C) else Color(0xFF5A4D73),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // 5. Inner Concentric Ring (Checker motif)
            val innerRingColor = if (isWhite) Color(0xFFDCC8B4) else Color(0xFF221B30)
            drawCircle(
                color = innerRingColor,
                radius = radius * 0.65f,
                center = center
            )

            drawCircle(
                color = if (isWhite) Color(0xFFB89F88) else Color(0xFF6B588B),
                radius = radius * 0.65f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 6. Center Bullseye Core
            val coreColor = if (isWhite) Color(0xFFF2E6D8) else Color(0xFF181324)
            drawCircle(
                color = coreColor,
                radius = radius * 0.38f,
                center = center
            )

            // 7. Subtle top specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = if (isWhite) 0.5f else 0.25f), Color.Transparent),
                    center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
                    radius = radius * 0.55f
                ),
                radius = radius * 0.5f,
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f)
            )

            // 8. Revived piece badge border if applicable
            if (piece.isRevived) {
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = radius * 0.98f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 9. Queen Crown Insignia
        if (piece.isQueen) {
            Text(
                text = "♛",
                color = if (isWhite) Color(0xFF996515) else Color(0xFFFFD700),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Renders an ethereal, glowing translucent ghost/corpse of the latest captured piece
 * on the exact square where it fell, visible when the player has the Pawn (Revive) superpower.
 */
@Composable
fun GhostPieceCorpseView(
    piece: Piece,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_corpse_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corpse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corpse_scale"
    )

    val isWhite = piece.player == PlayerSide.WHITE

    Box(
        modifier = modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f - 2.dp.toPx()) * pulseScale

            // 1. Ethereal Emerald Soul Halo (Pawn revival color)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = pulseAlpha * 0.85f),
                        Color(0xFF00E676).copy(alpha = pulseAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )

            // 2. Faint Ghostly Checker Body
            val baseColors = if (isWhite) {
                listOf(
                    Color(0xFFFAF0E6).copy(alpha = pulseAlpha * 0.95f),
                    Color(0xFFE0D0C0).copy(alpha = pulseAlpha * 0.80f),
                    Color(0xFF8F7662).copy(alpha = pulseAlpha * 0.50f)
                )
            } else {
                listOf(
                    Color(0xFF5A4D73).copy(alpha = pulseAlpha * 0.95f),
                    Color(0xFF2E2342).copy(alpha = pulseAlpha * 0.80f),
                    Color(0xFF140F20).copy(alpha = pulseAlpha * 0.50f)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = baseColors,
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius * 1.1f
                ),
                radius = radius,
                center = center
            )

            // 3. Glowing Emerald Revival Border
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = (pulseAlpha * 1.3f).coerceAtMost(0.9f)),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        // 4. Ghost Insignia (Crown for Queened piece or Pawn Soul symbol)
        if (piece.isQueen) {
            Text(
                text = "♛",
                color = Color(0xFFFFD700).copy(alpha = (pulseAlpha * 1.8f).coerceAtMost(0.95f)),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(
                text = "♟",
                color = Color(0xFF00E676).copy(alpha = (pulseAlpha * 1.8f).coerceAtMost(0.95f)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

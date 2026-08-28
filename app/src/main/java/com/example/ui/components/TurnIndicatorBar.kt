package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameState
import com.example.model.PlayerSide
import com.example.model.TimeControl

/**
 * Compact, player-oriented Turn and Match Status Indicator Bar.
 * Can be mirrored (180° rotation) for head-to-head tabletop play so Player 2 can read it upright.
 */
@Composable
fun TurnIndicatorBar(
    state: GameState,
    player: PlayerSide,
    isCurrentTurn: Boolean,
    isTimerPaused: Boolean,
    onResumeTimer: () -> Unit,
    currentTimeControl: TimeControl,
    isAiThinking: Boolean = false,
    mirrored: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isWhite = player == PlayerSide.WHITE
    val activeColor = if (isWhite) Color(0xFFFFD700) else Color(0xFF00E5FF)

    val surfaceColor = when {
        isTimerPaused -> Color(0xFFFF9800).copy(alpha = 0.12f)
        isCurrentTurn -> activeColor.copy(alpha = 0.10f)
        else -> Color(0xFF131020).copy(alpha = 0.45f)
    }

    val borderStroke = when {
        isTimerPaused -> BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
        isCurrentTurn -> BorderStroke(1.dp, activeColor.copy(alpha = 0.35f))
        else -> BorderStroke(0.8.dp, Color.White.copy(alpha = 0.08f))
    }

    val transformModifier = if (mirrored) {
        Modifier.graphicsLayer(rotationZ = 180f)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(transformModifier)
            .testTag("turn_indicator_${player.name.lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        color = surfaceColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Turn state badge & Announcement
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Status dot indicator
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isTimerPaused -> Color(0xFFFF9800)
                                isCurrentTurn -> activeColor
                                else -> Color.White.copy(alpha = 0.25f)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))

                val statusText = when {
                    isTimerPaused -> "⏸ MATCH PAUSED"
                    isAiThinking -> "🤖 AI thinking..."
                    isCurrentTurn -> "👉 ${player.displayName}'s Turn"
                    else -> "⏳ ${state.currentTurn.displayName}'s Turn"
                }

                val statusColor = when {
                    isTimerPaused -> Color(0xFFFFB74D)
                    isCurrentTurn -> Color.White
                    else -> Color.White.copy(alpha = 0.55f)
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.5.sp,
                    fontWeight = if (isCurrentTurn || isTimerPaused) FontWeight.Bold else FontWeight.Medium
                )

                if (state.announcement != null && (isCurrentTurn || isTimerPaused)) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${state.announcement}",
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Action pill or Time control status hint
            if (isTimerPaused) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onResumeTimer() }
                        .testTag("resume_timer_btn_${player.name.lowercase()}"),
                    color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "▶ Resume",
                        color = Color(0xFF00E5FF),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                val hintText = when {
                    currentTimeControl.isTimed && state.moveHistory.isEmpty() -> "⏱ Starts on 1st move"
                    currentTimeControl.isTimed -> "⏱ ${currentTimeControl.title}"
                    isCurrentTurn -> "Tap power for info"
                    else -> player.displayName
                }
                Text(
                    text = hintText,
                    color = if (isCurrentTurn) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.30f),
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

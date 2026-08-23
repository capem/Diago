package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameState
import com.example.model.PlayerSide
import com.example.model.Superpower

@Composable
fun SuperpowerBar(
    state: GameState,
    player: PlayerSide,
    isCurrentTurn: Boolean,
    onPowerClick: (Superpower) -> Unit,
    onCancelPower: () -> Unit,
    onShowPowerInfo: (Superpower) -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingPowers = state.remainingPowers(player)
    val isWhite = player == PlayerSide.WHITE

    val cardBg = if (isWhite) Color(0xFF1E1A2E) else Color(0xFF151221)
    val borderColor = if (isCurrentTurn) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Player Title & Active Superpower Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isWhite) Color(0xFFFAF0E6) else Color(0xFF2C243B))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${player.displayName}'s Superpowers",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Remaining Count Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "${remainingPowers.size}/6 Ready",
                        color = if (remainingPowers.isNotEmpty()) Color(0xFFFFD700) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6 Superpowers Horizontal Grid / Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Superpower.entries.forEach { power ->
                    val isAvailable = remainingPowers.contains(power)
                    val isActive = state.activePower == power && isCurrentTurn
                    val isPawnWithEmptyGraveyard = power == Superpower.PAWN && state.graveyard(player).isEmpty()

                    PowerBadgeItem(
                        power = power,
                        isAvailable = isAvailable && !isPawnWithEmptyGraveyard,
                        isUsed = !isAvailable,
                        isActive = isActive,
                        isCurrentTurn = isCurrentTurn,
                        onClick = {
                            if (isAvailable && isCurrentTurn) {
                                onPowerClick(power)
                            } else {
                                onShowPowerInfo(power)
                            }
                        },
                        onLongClick = { onShowPowerInfo(power) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Active Power Notification / Cancel Action Row
            AnimatedVisibility(
                visible = isCurrentTurn && (state.activePower != null || state.kingMoveCount == 1 || state.isPromotingQueenMode || state.isRevivingPawnMode || state.isBishopTeleportMode),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val activePowerText = when {
                    state.kingMoveCount == 1 -> "👑 KING Double Move: Make move 2/2"
                    state.isPromotingQueenMode -> "👸 QUEEN: Tap any piece to promote"
                    state.isRevivingPawnMode -> "♟ PAWN: Tap empty baseline square to revive"
                    state.isBishopTeleportMode -> "🏹 BISHOP: Tap piece then any empty square"
                    state.activePower == Superpower.ROOK -> "🏰 ROOK: Unlimited diagonal slide active"
                    state.activePower == Superpower.KNIGHT -> "🐴 KNIGHT: Backward moves enabled"
                    state.activePower != null -> "${state.activePower.emoji} ${state.activePower.title} Active"
                    else -> "Power Active"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activePowerText,
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (state.activePower != null || state.isPromotingQueenMode || state.isRevivingPawnMode || state.isBishopTeleportMode) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCancelPower() }
                                .testTag("cancel_power_button"),
                            color = Color(0xFFFF5252).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFFF8A80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerBadgeItem(
    power: Superpower,
    isAvailable: Boolean,
    isUsed: Boolean,
    isActive: Boolean,
    isCurrentTurn: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = when {
        isUsed -> 0.3f
        !isCurrentTurn -> 0.65f
        else -> 1.0f
    }

    val borderModifier = when {
        isActive -> Modifier.border(2.dp, power.accentColor, RoundedCornerShape(10.dp))
        isAvailable && isCurrentTurn -> Modifier.border(1.dp, power.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        else -> Modifier.border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
    }

    val backgroundBrush = when {
        isActive -> Brush.verticalGradient(listOf(power.accentColor.copy(alpha = 0.35f), Color.Transparent))
        isAvailable && isCurrentTurn -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f)))
        else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.Transparent))
    }

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(borderModifier)
            .background(backgroundBrush)
            .clickable(enabled = true) { onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .testTag("power_btn_${power.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = power.emoji,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Text(
            text = power.title,
            color = if (isUsed) Color.Gray else if (isActive) power.accentColor else Color.White.copy(alpha = alpha),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )

        Text(
            text = if (isUsed) "USED" else if (isActive) "ON" else "1x",
            color = if (isUsed) Color.DarkGray else if (isActive) power.accentColor else power.accentColor.copy(alpha = 0.8f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

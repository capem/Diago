package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.engine.GameState
import com.example.model.PlayerSide
import com.example.model.Superpower
import com.example.model.TimeControl

/**
 * Sleek, ultra-compact Opponent Status Ribbon (Height: ~28dp).
 * Shows opponent name, ready superpower count, mini-power pips, and fallen pieces.
 * Frees up 80+ dp of vertical space compared to full superpower boxes.
 */
@Composable
fun OpponentStatusRibbon(
    state: GameState,
    player: PlayerSide,
    onInspectPowers: () -> Unit,
    onOpenGraveyard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingPowers = state.remainingPowers(player)
    val isWhite = player == PlayerSide.WHITE
    val graveyard = state.graveyard(player)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onInspectPowers() }
            .testTag("opponent_ribbon"),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131020),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player side indicator & title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isWhite) Color(0xFFFAF0E6) else Color(0xFF2C243B))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${player.displayName}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${remainingPowers.size}/6 Powers",
                    color = Color(0xFFFFD700).copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }

            // Mini Superpower Pips Row + Fallen Piece summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Superpower.entries.forEach { power ->
                    val isAvailable = remainingPowers.contains(power)
                    val isPowerActive = state.activePower == power && state.currentTurn == player
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isPowerActive -> power.accentColor.copy(alpha = 0.35f)
                                    isAvailable -> Color.White.copy(alpha = 0.10f)
                                    else -> Color.White.copy(alpha = 0.02f)
                                }
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (isPowerActive) power.accentColor else if (isAvailable) power.accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = power.emoji,
                            fontSize = 9.sp,
                            color = if (isAvailable) Color.White else Color.Gray.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Lost pieces mini counter
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenGraveyard() },
                    shape = RoundedCornerShape(6.dp),
                    color = if (graveyard.isNotEmpty()) Color(0xFFFF5252).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💀", fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${graveyard.size}",
                            color = if (graveyard.isNotEmpty()) Color(0xFFFF8A80) else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact, vertically optimized Superpower Bar for players.
 * Can be mirrored (180-degree rotation) for head-to-head tabletop play.
 * Displays superpower action chips with tap to activate / info, long press for tooltip/details.
 */
@Composable
fun SuperpowerBar(
    state: GameState,
    player: PlayerSide,
    isCurrentTurn: Boolean,
    onPowerClick: (Superpower) -> Unit,
    onCancelPower: () -> Unit,
    onShowPowerInfo: (Superpower) -> Unit,
    onOpenGraveyard: () -> Unit = {},
    timeMillis: Long? = null,
    isTimed: Boolean = false,
    isClockTicking: Boolean = false,
    mirrored: Boolean = false,
    modifier: Modifier = Modifier
) {
    val remainingPowers = state.remainingPowers(player)
    val isWhite = player == PlayerSide.WHITE
    val graveyard = state.graveyard(player)

    val cardBg = if (isCurrentTurn) {
        if (isWhite) Color(0xFF1E1A2E) else Color(0xFF15192C)
    } else {
        if (isWhite) Color(0xFF171424) else Color(0xFF121524)
    }
    val borderColor = if (isCurrentTurn) {
        if (isWhite) Color(0xFFFFD700).copy(alpha = 0.90f) else Color(0xFF00E5FF).copy(alpha = 0.90f)
    } else {
        if (isWhite) Color(0xFFFFD700).copy(alpha = 0.25f) else Color(0xFF00E5FF).copy(alpha = 0.25f)
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
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .testTag("superpower_bar_${player.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Header: Player Title & Ready Count + Graveyard info + Turn badge + Chess Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isWhite) Color(0xFFFAF0E6) else Color(0xFF2C243B))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${player.displayName}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    if (isCurrentTurn) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isWhite) Color(0xFFFFD700).copy(alpha = 0.25f) else Color(0xFF00E5FF).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "👉 TURN",
                                color = if (isWhite) Color(0xFFFFD700) else Color(0xFF00E5FF),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Digital Chess Clock Pill
                    if (isTimed && timeMillis != null) {
                        val isLowTime = timeMillis < 15_000 && isClockTicking
                        val clockBg = when {
                            isLowTime && isClockTicking -> Color(0xFFFF1744).copy(alpha = 0.35f)
                            isClockTicking -> (if (isWhite) Color(0xFFFFD700) else Color(0xFF00E5FF)).copy(alpha = 0.22f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                        val clockTextColor = when {
                            isLowTime && isClockTicking -> Color(0xFFFF5252)
                            isClockTicking -> if (isWhite) Color(0xFFFFD700) else Color(0xFF00E5FF)
                            else -> Color.White.copy(alpha = 0.85f)
                        }
                        val clockBorder = when {
                            isLowTime && isClockTicking -> Color(0xFFFF1744)
                            isClockTicking -> if (isWhite) Color(0xFFFFD700).copy(alpha = 0.7f) else Color(0xFF00E5FF).copy(alpha = 0.7f)
                            else -> Color.White.copy(alpha = 0.15f)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = clockBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, clockBorder),
                            modifier = Modifier.testTag("player_clock_${player.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isLowTime) "⚡" else "⏱",
                                    fontSize = 8.5.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = TimeControl.formatTimePrecise(timeMillis),
                                    color = clockTextColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    // Graveyard counter pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenGraveyard() },
                        shape = RoundedCornerShape(6.dp),
                        color = if (graveyard.isNotEmpty()) Color(0xFFFF5252).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💀", fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${graveyard.size}",
                                color = if (graveyard.isNotEmpty()) Color(0xFFFF8A80) else Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "${remainingPowers.size}/6",
                            color = if (remainingPowers.isNotEmpty()) Color(0xFFFFD700) else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Cancel button if this player has active power mode
                    if (isCurrentTurn && (state.activePower != null || state.isPromotingQueenMode || state.isRevivingPawnMode || state.isBishopTeleportMode)) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onCancelPower() }
                                .testTag("cancel_power_${player.name.lowercase()}"),
                            color = Color(0xFFFF5252).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFFF8A80),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 6 Superpowers Horizontal Grid (Sleek 32dp chip height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Superpower.entries.forEach { power ->
                    val isPowerInRemaining = remainingPowers.contains(power)
                    val isActive = state.activePower == power && isCurrentTurn
                    val isPawnReviveValid = power != Superpower.PAWN || GameEngine.canRevivePawn(state, player)
                    val isPowerReady = isPowerInRemaining && (power != Superpower.PAWN || isPawnReviveValid)

                    CompactPowerBadgeItem(
                        power = power,
                        isAvailable = isPowerReady,
                        isUsed = !isPowerInRemaining,
                        isActive = isActive,
                        isCurrentTurn = isCurrentTurn,
                        onClick = {
                            if (isPowerInRemaining && isCurrentTurn) {
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactPowerBadgeItem(
    power: Superpower,
    isAvailable: Boolean,
    isUsed: Boolean,
    isActive: Boolean,
    isCurrentTurn: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = isAvailable && isCurrentTurn

    val borderModifier = when {
        isActive -> Modifier.border(1.5.dp, power.accentColor, RoundedCornerShape(8.dp))
        isEnabled -> Modifier.border(1.dp, power.accentColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
        isUsed -> Modifier.border(0.6.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
        else -> Modifier.border(0.8.dp, power.accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)) // Available but inactive turn
    }

    val backgroundBrush = when {
        isActive -> Brush.verticalGradient(listOf(power.accentColor.copy(alpha = 0.38f), Color.Transparent))
        isEnabled -> Brush.verticalGradient(listOf(power.accentColor.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)))
        isUsed -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Transparent))
        else -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.02f))) // Available/standby
    }

    val emojiAlpha = when {
        isActive -> 1.0f
        isEnabled -> 1.0f
        isUsed -> 0.25f
        else -> 0.85f // Clear and readable on standby
    }

    val titleColor = when {
        isUsed -> Color.White.copy(alpha = 0.35f)
        isActive -> power.accentColor
        isEnabled -> Color.White
        else -> Color.White.copy(alpha = 0.85f)
    }

    val badgeColor = when {
        isUsed -> Color.White.copy(alpha = 0.30f)
        isActive -> power.accentColor
        isEnabled -> power.accentColor
        else -> power.accentColor.copy(alpha = 0.75f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(borderModifier)
            .background(backgroundBrush)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .testTag("power_btn_${power.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = power.emoji,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(bottom = 1.dp)
                .graphicsLayer(alpha = emojiAlpha)
        )

        Text(
            text = power.title,
            color = titleColor,
            fontSize = 9.sp,
            fontWeight = if (isActive || isEnabled) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )

        Text(
            text = if (isUsed) "USED" else if (isActive) "ON" else "1x",
            color = badgeColor,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


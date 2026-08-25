package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.example.ui.theme.KeyboardArrowDown
import com.example.ui.theme.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Superpower

/**
 * Visual Graveyard panel and sidebar tracker that displays pieces captured by each player,
 * their capture coordinates, Queen status, and the designated target for Pawn revival superpowers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GraveyardView(
    capturedWhitePieces: List<Piece>,
    capturedBlackPieces: List<Piece>,
    activeTurn: PlayerSide,
    hasWhitePawnPower: Boolean = true,
    hasBlackPawnPower: Boolean = true,
    onOpenFullModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "graveyard_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .testTag("graveyard_panel"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF140F24),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0xFFFFD700).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.12f),
                    Color(0xFF00E5FF).copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Header with quick overview & toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💀 Graveyard Chamber",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "♟ Revive Tracker",
                            color = Color(0xFF00E676),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Hide Details" else "Expand Log",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dual Player Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White Lost Summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "⚪ Lost (${capturedWhitePieces.size}): ",
                        color = Color(0xFFFAF0E6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (capturedWhitePieces.isEmpty()) {
                        Text(text = "None", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            capturedWhitePieces.forEachIndexed { index, piece ->
                                val isLatest = index == capturedWhitePieces.lastIndex
                                GraveyardPieceToken(
                                    piece = piece,
                                    isLatest = isLatest,
                                    hasPawnPower = hasWhitePawnPower,
                                    pulseGlow = pulseGlow
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Black Lost Summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "⚫ Lost (${capturedBlackPieces.size}): ",
                        color = Color(0xFFB39DDB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (capturedBlackPieces.isEmpty()) {
                        Text(text = "None", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            capturedBlackPieces.forEachIndexed { index, piece ->
                                val isLatest = index == capturedBlackPieces.lastIndex
                                GraveyardPieceToken(
                                    piece = piece,
                                    isLatest = isLatest,
                                    hasPawnPower = hasBlackPawnPower,
                                    pulseGlow = pulseGlow
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Detailed Breakdown Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // White's detailed graveyard card
                    PlayerGraveyardDetailCard(
                        player = PlayerSide.WHITE,
                        capturedPieces = capturedWhitePieces,
                        hasPawnPower = hasWhitePawnPower,
                        pulseGlow = pulseGlow
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Black's detailed graveyard card
                    PlayerGraveyardDetailCard(
                        player = PlayerSide.BLACK,
                        capturedPieces = capturedBlackPieces,
                        hasPawnPower = hasBlackPawnPower,
                        pulseGlow = pulseGlow
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Revival Mechanics Hint
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💡", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Activating Pawn (♟ Revive) resurrects your topmost fallen piece directly onto its exact old board coordinate. Queen status is preserved!",
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed card for one player's graveyard stack with exact capture coordinates and revive status.
 */
@Composable
private fun PlayerGraveyardDetailCard(
    player: PlayerSide,
    capturedPieces: List<Piece>,
    hasPawnPower: Boolean,
    pulseGlow: Float
) {
    val isWhite = player == PlayerSide.WHITE
    val accentColor = if (isWhite) Color(0xFFFFD700) else Color(0xFF00E5FF)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isWhite) "⚪ White's Fallen Pieces" else "⚫ Black's Fallen Pieces",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (hasPawnPower) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = if (hasPawnPower) "♟ Power Ready" else "♟ Power Used",
                        color = if (hasPawnPower) Color(0xFF00E676) else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (capturedPieces.isEmpty()) {
                Text(
                    text = "No pieces lost yet. All units active on the board.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    capturedPieces.forEachIndexed { index, piece ->
                        val isLatest = index == capturedPieces.lastIndex
                        val capturePosStr = piece.capturedAt?.notation() ?: "Unknown"

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLatest && hasPawnPower) Color(0xFF00E676).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.02f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isLatest && hasPawnPower) Color(0xFF00E676).copy(alpha = 0.5f * pulseGlow) else Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GraveyardPieceToken(
                                        piece = piece,
                                        isLatest = isLatest,
                                        hasPawnPower = hasPawnPower,
                                        pulseGlow = pulseGlow
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = if (piece.isQueen) "${if (isWhite) "White" else "Black"} Queen 👑" else "${if (isWhite) "White" else "Black"} Checker",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "Fell at square $capturePosStr",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                if (isLatest && hasPawnPower) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF00E676)
                                    ) {
                                        Text(
                                            text = "🎯 NEXT REVIVAL",
                                            color = Color.Black,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "#${index + 1}",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual miniature piece token for graveyard listings.
 */
@Composable
private fun GraveyardPieceToken(
    piece: Piece,
    isLatest: Boolean,
    hasPawnPower: Boolean,
    pulseGlow: Float
) {
    val isWhite = piece.player == PlayerSide.WHITE
    val showReviveTargetRing = isLatest && hasPawnPower

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                if (isWhite) Color(0xFFFAF0E6) else Color(0xFF2C243B)
            )
            .border(
                width = if (showReviveTargetRing) 1.5.dp else 1.dp,
                color = if (showReviveTargetRing) Color(0xFF00E676) else if (isWhite) Color.White else Color.White.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (piece.isQueen) {
            Text(
                text = "♛",
                fontSize = 9.sp,
                color = if (isWhite) Color(0xFF8B6508) else Color(0xFFFFD700)
            )
        }
    }
}

/**
 * Full screen Modal Dialog for the Graveyard & Revival Chamber.
 */
@Composable
fun GraveyardModalDialog(
    capturedWhitePieces: List<Piece>,
    capturedBlackPieces: List<Piece>,
    hasWhitePawnPower: Boolean,
    hasBlackPawnPower: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F24)),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFF00E676), Color(0xFF00E5FF)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💀", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Graveyard & Revival Chamber",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Track fallen pieces & target revive positions",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    PlayerGraveyardDetailCard(
                        player = PlayerSide.WHITE,
                        capturedPieces = capturedWhitePieces,
                        hasPawnPower = hasWhitePawnPower,
                        pulseGlow = 1f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PlayerGraveyardDetailCard(
                        player = PlayerSide.BLACK,
                        capturedPieces = capturedBlackPieces,
                        hasPawnPower = hasBlackPawnPower,
                        pulseGlow = 1f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "♟ Pawn (Revive) Superpower Guide",
                                color = Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Revives the LAST piece captured back into its exact old square on the diamond board.\n• A faint ghost corpse marks the revival destination on the board.\n• If the piece was a Queen, it returns as a Queen with full backward mobility.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position

/**
 * Confirmation dialog protecting the Pawn Revive superpower against accidental misclicks.
 * Shows the exact target piece, revival coordinate, and turn effect before spending the power.
 */
@Composable
fun PawnReviveConfirmDialog(
    pieceToRevive: Piece,
    targetPos: Position,
    player: PlayerSide,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isWhite = player == PlayerSide.WHITE
    val isQueen = pieceToRevive.isQueen
    val accentEmerald = Color(0xFF00E676)
    val cardBg = Color(0xFF161224)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, accentEmerald),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pawn_revive_confirm_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentEmerald.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentEmerald.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "♟ PAWN SUPERPOWER", color = accentEmerald, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Glowing Revive Token Preview
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(accentEmerald.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(2.dp, accentEmerald, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isWhite) Color(0xFFFAF0E6) else Color(0xFF2C243B))
                            .border(1.5.dp, if (isWhite) Color.White else Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isQueen) {
                            Text(
                                text = "♛",
                                fontSize = 24.sp,
                                color = if (isWhite) Color(0xFF8B6508) else Color(0xFFFFD700)
                            )
                        } else {
                            Text(
                                text = "♟",
                                fontSize = 22.sp,
                                color = if (isWhite) Color(0xFF333333) else Color(0xFFFAF0E6)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Confirm Pawn Revival",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Detail card of unit and destination
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Unit Type:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text(
                                text = if (isQueen) "${player.displayName} Queen 👑" else "${player.displayName} Checker ♟",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Revival Square:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentEmerald.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = targetPos.notation(),
                                    color = accentEmerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isQueen) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✨ Preserves full Queen backward movement abilities!",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Warning / effect note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF9100).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚠️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This will consume your 1x Pawn power and immediately pass the turn to ${player.opponent().displayName}.",
                            color = Color(0xFFFFCC80),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_pawn_revive_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("confirm_pawn_revive_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Revive Unit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

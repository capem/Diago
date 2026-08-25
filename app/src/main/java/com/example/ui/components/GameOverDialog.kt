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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.model.GameStatus

@Composable
fun GameOverDialog(
    status: GameStatus,
    moveCount: Int,
    powersUsedCount: Int,
    onRematch: () -> Unit,
    onHome: () -> Unit
) {
    val isWhiteWinner = status.isWhiteWin
    val winnerName = if (isWhiteWinner) "White" else "Black"
    val winnerColor = if (isWhiteWinner) Color(0xFFFFD700) else Color(0xFF00E5FF)
    val subtitleText = when {
        status.isTimeout -> "Victorious on Time! ⏱️ Opponent ran out of clock."
        status == GameStatus.DRAW -> "Stalemate / Draw! No remaining legal moves."
        else -> "Checkmate by Elimination & Board Domination"
    }

    Dialog(onDismissRequest = { /* Modal */ }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF161226),
            border = androidx.compose.foundation.BorderStroke(2.dp, winnerColor),
            modifier = Modifier.fillMaxWidth().testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy Crown Banner
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(winnerColor.copy(alpha = 0.4f), Color.Transparent))
                        )
                        .border(2.dp, winnerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = winnerColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (status == GameStatus.DRAW) "Draw Game!" else "$winnerName Wins!",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitleText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stats summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$moveCount",
                                color = winnerColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Turns",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$powersUsedCount",
                                color = Color(0xFFFF4081),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powers Used",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier.weight(1f).testTag("dialog_home_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Menu", color = Color.White)
                    }

                    Button(
                        onClick = onRematch,
                        modifier = Modifier.weight(1f).testTag("dialog_rematch_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = winnerColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Rematch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

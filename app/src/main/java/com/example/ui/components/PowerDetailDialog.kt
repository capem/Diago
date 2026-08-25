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
import com.example.model.Superpower

@Composable
fun PowerDetailDialog(
    power: Superpower,
    isAvailable: Boolean,
    isCurrentTurn: Boolean,
    onActivate: (Superpower) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF191629),
            border = androidx.compose.foundation.BorderStroke(2.dp, power.accentColor),
            modifier = Modifier.fillMaxWidth().testTag("power_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emoji Icon with Glowing Ring
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(power.accentColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(2.dp, power.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = power.emoji, fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${power.title} — ${power.subtitle}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                val statusText = when {
                    !isAvailable -> "Consumed (Already Used)"
                    !isCurrentTurn -> "One-time use (Wait for Your Turn)"
                    else -> "One-time use (Ready to Activate)"
                }
                val statusColor = when {
                    !isAvailable -> Color.Gray
                    !isCurrentTurn -> Color(0xFFFFA726)
                    else -> power.accentColor
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = power.description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("Close", color = Color.White)
                    }

                    if (isAvailable && isCurrentTurn) {
                        Button(
                            onClick = {
                                onActivate(power)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("activate_power_dialog_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = power.accentColor,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Activate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GameRulesConfig
import com.example.model.PlayerSide
import com.example.model.Position
import kotlin.math.roundToInt

@Composable
fun MatchRulesDialog(
    currentConfig: GameRulesConfig,
    onApply: (GameRulesConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var lossThreshold by remember { mutableIntStateOf(currentConfig.lossPieceThreshold) }
    var queenDistance by remember { mutableIntStateOf(currentConfig.queenDistanceThreshold) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF161226),
            border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("match_rules_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Match Custom Rules",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize loss conditions & queening zones",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- SECTION 1: LOSS PIECE THRESHOLD ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Loss Condition (Pieces Remaining)",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (lossThreshold == 0) "Standard: Lose when all 10 pieces captured (0 left)"
                                    else "Player loses when reduced to $lossThreshold piece${if (lossThreshold > 1) "s" else ""} or fewer",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Stepper Decrement (-)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            if (lossThreshold > 0) lossThreshold--
                                        }
                                        .testTag("loss_step_dec"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease Loss Threshold",
                                        tint = if (lossThreshold > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "≤ $lossThreshold left",
                                        color = Color(0xFFFFD700),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                // Stepper Increment (+)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            if (lossThreshold < 9) lossThreshold++
                                        }
                                        .testTag("loss_step_inc"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase Loss Threshold",
                                        tint = if (lossThreshold < 9) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value = lossThreshold.toFloat(),
                            onValueChange = { floatVal ->
                                lossThreshold = floatVal.roundToInt().coerceIn(0, 9)
                            },
                            valueRange = 0f..9f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD700),
                                activeTrackColor = Color(0xFFFFD700),
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("loss_piece_slider")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick discrete step selector pills (0..9) so all steps can be selected by tap or drag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (0..9).forEach { num ->
                                val isSelected = lossThreshold == num
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFFFFD700)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.12f),
                                            CircleShape
                                        )
                                        .clickable { lossThreshold = num }
                                        .testTag("loss_step_$num"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$num",
                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- SECTION 2: QUEENING THRESHOLD (DISTANCE FROM CORNER) ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Queening Border Distance",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val borderCount = if (queenDistance == 0) "1 cell (Apex corner only)"
                                else if (queenDistance >= 6) "Full borders (13 cells)"
                                else "${queenDistance * 2 + 1} border cells (${queenDistance} from corner)"
                                Text(
                                    text = borderCount,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Stepper Decrement (-)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            if (queenDistance > 0) queenDistance--
                                        }
                                        .testTag("queen_step_dec"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease Queen Threshold",
                                        tint = if (queenDistance > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$queenDistance cell${if (queenDistance != 1) "s" else ""}",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                // Stepper Increment (+)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            if (queenDistance < 6) queenDistance++
                                        }
                                        .testTag("queen_step_inc"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase Queen Threshold",
                                        tint = if (queenDistance < 6) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value = queenDistance.toFloat(),
                            onValueChange = { floatVal ->
                                queenDistance = floatVal.roundToInt().coerceIn(0, 6)
                            },
                            valueRange = 0f..6f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("queen_threshold_slider")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Discrete step selector pills (0..6)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (0..6).forEach { num ->
                                val isSelected = queenDistance == num
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.12f),
                                            CircleShape
                                        )
                                        .clickable { queenDistance = num }
                                        .testTag("queen_step_$num"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$num",
                                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini Interactive Visual Preview of Queening Border cells
                        Text(
                            text = "PREVIEW VALID QUEENING SQUARES (HIGHLIGHTED ON BOARD):",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F0B1A))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val whiteGoals = (0..6).flatMap { r -> (0..6).map { c -> Position(r, c) } }
                                .filter { PlayerSide.WHITE.isPromotionGoal(it, queenDistance) }
                                .sortedBy { it.row + it.col }

                            Text(
                                text = "White promotion squares: ${whiteGoals.joinToString(", ") { it.notation() }}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Reset to Default & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                lossThreshold = 0
                                queenDistance = 6
                            }
                            .testTag("reset_rules_btn"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Defaults", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            onApply(GameRulesConfig(lossPieceThreshold = lossThreshold, queenDistanceThreshold = queenDistance))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("apply_rules_btn")
                    ) {
                        Text(
                            text = "Apply Rules",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

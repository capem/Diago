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
import androidx.compose.material.icons.filled.Close
import com.example.ui.theme.Timer
import com.example.ui.theme.Tune
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.model.TimeControl
import kotlin.math.roundToInt

@Composable
fun TimeControlDialog(
    currentSelection: TimeControl,
    onSelect: (TimeControl) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(currentSelection) }
    var activeTab by remember { mutableStateOf(if (currentSelection.isCustom) 1 else 0) } // 0: Presets, 1: Custom Sliders

    // Custom slider states (minutes: 0..60, incrementSeconds: 0..30)
    var customMinutes by remember {
        mutableIntStateOf(if (currentSelection.isTimed) (currentSelection.totalSeconds / 60).coerceIn(0, 60) else 5)
    }
    var customIncrement by remember {
        mutableIntStateOf(currentSelection.incrementSeconds.coerceIn(0, 30))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF161226),
            border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("time_control_dialog")
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
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Time Control",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose match clock & increment",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs: Standard Presets vs Custom Sliders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(3.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { activeTab = 0 }
                            .testTag("tc_tab_presets"),
                        shape = RoundedCornerShape(9.dp),
                        color = if (activeTab == 0) Color(0xFFFFD700) else Color.Transparent
                    ) {
                        Text(
                            text = "Standard Presets",
                            color = if (activeTab == 0) Color.Black else Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { activeTab = 1 }
                            .testTag("tc_tab_custom"),
                        shape = RoundedCornerShape(9.dp),
                        color = if (activeTab == 1) Color(0xFFFFD700) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color.Black else Color(0xFFFFD700),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custom Sliders",
                                color = if (activeTab == 1) Color.Black else Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeTab == 0) {
                    // Time Presets List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimeControl.entries.forEach { tc ->
                            val isChosen = selected == tc && !selected.isCustom
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selected = tc }
                                    .testTag("tc_option_${tc.title.lowercase().replace(" ", "_")}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) Color(0xFFFFD700).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) Color(0xFFFFD700) else Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = tc.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = tc.title,
                                                color = if (isChosen) Color(0xFFFFD700) else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = tc.subtitle,
                                                color = Color.White.copy(alpha = 0.55f),
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }

                                    if (isChosen) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFD700)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom Time Control Sliders
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Slider 1: Base Clock Time (Minutes: 0..60 min)
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
                                    Column {
                                        Text(
                                            text = "Base Time (Minutes)",
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (customMinutes == 0 && customIncrement == 0) "Unlimited Casual (No Timer)"
                                            else if (customMinutes == 0) "Increment only (${customIncrement}s)"
                                            else "$customMinutes minute${if (customMinutes > 1) "s" else ""}",
                                            color = Color(0xFFFFD700),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (customMinutes == 0 && customIncrement == 0) "∞" else "${customMinutes}m",
                                            color = Color(0xFFFFD700),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Slider(
                                    value = customMinutes.toFloat(),
                                    onValueChange = {
                                        customMinutes = it.roundToInt().coerceIn(0, 60)
                                        selected = TimeControl.custom(customMinutes, customIncrement)
                                    },
                                    valueRange = 0f..60f,
                                    steps = 59,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD700),
                                        activeTrackColor = Color(0xFFFFD700),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier.testTag("base_time_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0m (Unlimited)", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    Text("15m", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    Text("30m", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    Text("60m", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                }
                            }
                        }

                        // Slider 2: Increment per move (Seconds: 0..30 s)
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
                                    Column {
                                        Text(
                                            text = "Increment Per Move (Seconds)",
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (customIncrement == 0) "No increment (+0s)"
                                            else "+$customIncrement second${if (customIncrement > 1) "s" else ""} added after each turn",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "+${customIncrement}s",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Slider(
                                    value = customIncrement.toFloat(),
                                    onValueChange = {
                                        customIncrement = it.roundToInt().coerceIn(0, 30)
                                        selected = TimeControl.custom(customMinutes, customIncrement)
                                    },
                                    valueRange = 0f..30f,
                                    steps = 29,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00E5FF),
                                        activeTrackColor = Color(0xFF00E5FF),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier.testTag("increment_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("+0s", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    Text("+10s", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    Text("+20s", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                    Text("+30s", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                                }
                            }
                        }

                        // Preview Box of active custom selection
                        val resolvedCustom = TimeControl.custom(customMinutes, customIncrement)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0D0A18))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = resolvedCustom.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = resolvedCustom.title,
                                    color = Color(0xFFFFD700),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = resolvedCustom.subtitle,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm Button
                val effectiveSelection = if (activeTab == 1) TimeControl.custom(customMinutes, customIncrement) else selected
                Button(
                    onClick = {
                        onSelect(effectiveSelection)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("confirm_time_control_btn")
                ) {
                    Text(
                        text = "Apply ${effectiveSelection.title} (${effectiveSelection.shortBadge})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

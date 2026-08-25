package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameRulesStorage
import com.example.model.AiColorChoice
import com.example.model.AiDifficulty
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.model.GameRulesConfig
import com.example.model.PlayerSide
import com.example.model.Superpower
import com.example.model.TimeControl
import com.example.ui.components.MatchRulesDialog
import com.example.ui.components.TimeControlDialog

@Composable
fun HomeScreen(
    currentTheme: BoardTheme,
    onThemeChange: (BoardTheme) -> Unit,
    isAudioMuted: Boolean,
    onToggleAudio: () -> Unit,
    onStartGame: (GameMode, AiDifficulty?, TimeControl, PlayerSide, GameRulesConfig) -> Unit,
    onOpenCodex: () -> Unit,
    rulesStorage: GameRulesStorage = GameRulesStorage(LocalContext.current)
) {
    var activeMode by remember { mutableStateOf(GameMode.PASS_AND_PLAY) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.TACTICIAN) }
    var selectedAiColor by remember { mutableStateOf(AiColorChoice.WHITE) }
    var selectedTimeControl by remember { mutableStateOf(TimeControl.RAPID_5) }
    var rulesConfig by remember(rulesStorage) { mutableStateOf(rulesStorage.loadRules()) }
    var showTimeControlPicker by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0B14),
                        Color(0xFF141021),
                        Color(0xFF09070F)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP NAVIGATION BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme selector pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.clickable { showThemeMenu = !showThemeMenu }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(currentTheme.accent)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = currentTheme.title,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action buttons: Codex & Audio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onOpenCodex,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("top_bar_codex_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Rules Codex",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleAudio,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("audio_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isAudioMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Audio Toggle",
                            tint = if (isAudioMuted) Color.White.copy(alpha = 0.4f) else currentTheme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Theme dropdown menu
            AnimatedVisibility(visible = showThemeMenu) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191428)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        BoardTheme.entries.forEach { theme ->
                            val isSelected = theme == currentTheme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable {
                                        onThemeChange(theme)
                                        showThemeMenu = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(theme.accent)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = theme.title,
                                    color = if (isSelected) theme.accent else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- HERO HEADER & EMBLEM ---
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .rotate(45f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF261D3B), Color(0xFF140F24))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFF00E5FF).copy(alpha = 0.6f))),
                        RoundedCornerShape(16.dp)
                    )
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = 32.sp,
                    modifier = Modifier.rotate(-45f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DIAGONAL CHESS",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "7×7 Diamond Arena  •  6 Superpowers",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // --- MAIN PLAY CONFIGURATION CARD ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF161224),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // MODE SWITCH TABS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Pass & Play tab
                        ModeTab(
                            title = "Pass & Play",
                            icon = Icons.Default.People,
                            isSelected = activeMode == GameMode.PASS_AND_PLAY,
                            onClick = { activeMode = GameMode.PASS_AND_PLAY },
                            modifier = Modifier.weight(1f)
                        )

                        // vs AI tab
                        ModeTab(
                            title = "vs Computer",
                            icon = Icons.Default.SmartToy,
                            isSelected = activeMode == GameMode.AI,
                            onClick = { activeMode = GameMode.AI },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MODE-SPECIFIC OPTIONS
                    AnimatedContent(
                        targetState = activeMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "mode_content"
                    ) { mode ->
                        if (mode == GameMode.PASS_AND_PLAY) {
                            // Pass & Play Info Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⚪", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "2 Players on 1 Screen",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Take turns on the rotated diamond board",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.5.sp
                                    )
                                }
                                Text(text = "⚫", fontSize = 16.sp)
                            }
                        } else {
                            // AI Mode Setup (Color + Difficulty)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Side / Color Choice
                                Column {
                                    Text(
                                        text = "YOUR SIDE",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AiColorChoice.entries.forEach { choice ->
                                            val isSelected = selectedAiColor == choice
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedAiColor = choice }
                                                    .testTag("ai_color_${choice.name.lowercase()}"),
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.05f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.08f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 7.dp, horizontal = 4.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = choice.emoji, fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = choice.title,
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Difficulty Selection
                                Column {
                                    Text(
                                        text = "DIFFICULTY",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AiDifficulty.entries.forEach { diff ->
                                            val isSelected = selectedDifficulty == diff
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedDifficulty = diff }
                                                    .testTag("ai_diff_${diff.name.lowercase()}"),
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.05f),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f)
                                                )
                                            ) {
                                                Text(
                                                    text = diff.title,
                                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.9f),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- TIME CONTROL SELECTOR ROW ---
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "TIME CONTROL",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Text(
                                text = "Custom Clock ⚙",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { showTimeControlPicker = true }
                                    .testTag("custom_time_control_btn")
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val presets = listOf(
                            TimeControl.UNLIMITED,
                            TimeControl.BULLET_1,
                            TimeControl.BLITZ_3_2,
                            TimeControl.RAPID_5,
                            TimeControl.CLASSICAL_10
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            presets.forEach { tc ->
                                val isSelected = selectedTimeControl == tc
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedTimeControl = tc }
                                        .testTag("home_tc_${tc.title.lowercase().replace(" ", "_")}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = tc.emoji,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = tc.title,
                                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- MATCH RULES STATUS & CONFIG TRIGGER ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showRulesDialog = true }
                            .testTag("custom_rules_config_btn"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rules:",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val rulesSummary = when {
                                    rulesConfig.isDefault -> "Standard (All captured • Full border)"
                                    else -> "≤${rulesConfig.lossPieceThreshold} loss • ${rulesConfig.queenDistanceThreshold} border"
                                }
                                Text(
                                    text = rulesSummary,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Edit Rules",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // --- PRIMARY ACTION START BUTTON ---
                    val isPassAndPlay = activeMode == GameMode.PASS_AND_PLAY
                    val launchButtonTag = if (isPassAndPlay) "pass_and_play_btn" else "play_ai_button"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isPassAndPlay) {
                                    onStartGame(
                                        GameMode.PASS_AND_PLAY,
                                        null,
                                        selectedTimeControl,
                                        PlayerSide.WHITE,
                                        rulesConfig
                                    )
                                } else {
                                    val resolvedSide = when (selectedAiColor) {
                                        AiColorChoice.WHITE -> PlayerSide.WHITE
                                        AiColorChoice.BLACK -> PlayerSide.BLACK
                                        AiColorChoice.RANDOM -> listOf(PlayerSide.WHITE, PlayerSide.BLACK).random()
                                    }
                                    onStartGame(
                                        GameMode.AI,
                                        selectedDifficulty,
                                        selectedTimeControl,
                                        resolvedSide,
                                        rulesConfig
                                    )
                                }
                            }
                            .testTag(launchButtonTag),
                        color = Color(0xFFFFD700),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val buttonText = if (isPassAndPlay) {
                                "Start 2-Player Match (${selectedTimeControl.shortBadge})"
                            } else {
                                "Battle AI (${selectedDifficulty.title} • ${selectedTimeControl.shortBadge})"
                            }
                            Text(
                                text = buttonText,
                                color = Color.Black,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SUPERPOWERS & RULES CODEX BANNER ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenCodex() }
                    .testTag("codex_menu_btn"),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF151021),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rules Codex & Superpowers",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Superpower Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Superpower.entries.forEach { power ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Text(text = power.emoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = power.title,
                                    color = power.accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // DIALOGS
        if (showTimeControlPicker) {
            TimeControlDialog(
                currentSelection = selectedTimeControl,
                onSelect = { selectedTimeControl = it },
                onDismiss = { showTimeControlPicker = false }
            )
        }

        if (showRulesDialog) {
            MatchRulesDialog(
                currentConfig = rulesConfig,
                onApply = {
                    rulesConfig = it
                    rulesStorage.saveRules(it)
                },
                onDismiss = { showRulesDialog = false }
            )
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.audio.SoundManager
import com.example.model.AiDifficulty
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesCodexScreen
import com.example.ui.theme.MyApplicationTheme

sealed class AppScreen {
    data object Home : AppScreen()
    data class Game(val mode: GameMode, val difficulty: AiDifficulty?) : AppScreen()
    data object Codex : AppScreen()
}

class MainActivity : ComponentActivity() {
    private val soundManager = SoundManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DiagonalChessApp(soundManager = soundManager)
            }
        }
    }
}

@Composable
fun DiagonalChessApp(soundManager: SoundManager) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var currentTheme by remember { mutableStateOf(BoardTheme.OBSIDIAN_GOLD) }
    var isAudioMuted by remember { mutableStateOf(soundManager.isAudioMuted()) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (val screen = currentScreen) {
            is AppScreen.Home -> {
                HomeScreen(
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it },
                    isAudioMuted = isAudioMuted,
                    onToggleAudio = { isAudioMuted = soundManager.toggleMute() },
                    onStartGame = { mode, diff -> currentScreen = AppScreen.Game(mode, diff) },
                    onOpenCodex = { currentScreen = AppScreen.Codex }
                )
            }

            is AppScreen.Game -> {
                BackHandler { currentScreen = AppScreen.Home }
                GameScreen(
                    gameMode = screen.mode,
                    aiDifficulty = screen.difficulty,
                    theme = currentTheme,
                    soundManager = soundManager,
                    onBackToHome = { currentScreen = AppScreen.Home }
                )
            }

            is AppScreen.Codex -> {
                BackHandler { currentScreen = AppScreen.Home }
                RulesCodexScreen(
                    onBack = { currentScreen = AppScreen.Home }
                )
            }
        }
    }
}

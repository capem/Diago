package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.audio.SoundManager
import com.example.data.GameRulesStorage
import com.example.model.AiDifficulty
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.model.PlayerSide
import com.example.model.TimeControl
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RulesCodexScreen
import com.example.ui.theme.MyApplicationTheme

sealed class AppScreen {
    data object Home : AppScreen()
    data class Game(
        val mode: GameMode,
        val difficulty: AiDifficulty?,
        val timeControl: TimeControl = TimeControl.UNLIMITED,
        val playerSide: PlayerSide = PlayerSide.WHITE,
        val rulesConfig: com.example.model.GameRulesConfig = com.example.model.GameRulesConfig()
    ) : AppScreen()
    data object Codex : AppScreen()
}

class MainActivity : ComponentActivity() {
    private val soundManager = SoundManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            MyApplicationTheme {
                DiagonalChessApp(soundManager = soundManager)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun DiagonalChessApp(soundManager: SoundManager) {
    val context = LocalContext.current
    val rulesStorage = remember { GameRulesStorage(context) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var currentTheme by remember { mutableStateOf(BoardTheme.OBSIDIAN_GOLD) }
    var isAudioMuted by remember { mutableStateOf(soundManager.isAudioMuted()) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is AppScreen.Home -> {
                HomeScreen(
                    currentTheme = currentTheme,
                    onThemeChange = { currentTheme = it },
                    isAudioMuted = isAudioMuted,
                    onToggleAudio = { isAudioMuted = soundManager.toggleMute() },
                    onStartGame = { mode, diff, tc, side, rules -> currentScreen = AppScreen.Game(mode, diff, tc, side, rules) },
                    onOpenCodex = { currentScreen = AppScreen.Codex },
                    rulesStorage = rulesStorage
                )
            }

            is AppScreen.Game -> {
                BackHandler { currentScreen = AppScreen.Home }
                GameScreen(
                    gameMode = screen.mode,
                    aiDifficulty = screen.difficulty,
                    timeControl = screen.timeControl,
                    playerSide = screen.playerSide,
                    rulesConfig = screen.rulesConfig,
                    theme = currentTheme,
                    soundManager = soundManager,
                    onBackToHome = { currentScreen = AppScreen.Home },
                    rulesStorage = rulesStorage
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

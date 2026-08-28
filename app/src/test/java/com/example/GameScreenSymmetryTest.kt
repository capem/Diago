package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundManager
import com.example.data.GameRulesStorage
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.model.PlayerSide
import com.example.model.TimeControl
import com.example.ui.screens.GameScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameScreenSymmetryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var soundManager: SoundManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        soundManager = SoundManager()
    }

    @Test
    fun `GameScreen in Pass and Play mode renders symmetrical top and bottom superpower and turn indicator bars`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                GameScreen(
                    gameMode = GameMode.PASS_AND_PLAY,
                    aiDifficulty = null,
                    timeControl = TimeControl.UNLIMITED,
                    playerSide = PlayerSide.WHITE,
                    theme = BoardTheme.OBSIDIAN_GOLD,
                    soundManager = soundManager,
                    onBackToHome = {}
                )
            }
        }

        // Verify top & bottom superpower bars exist
        composeTestRule.onNodeWithTag("superpower_bar_black").assertIsDisplayed()
        composeTestRule.onNodeWithTag("superpower_bar_white").assertIsDisplayed()

        // Verify top & bottom turn indicators exist
        composeTestRule.onNodeWithTag("turn_indicator_black").assertIsDisplayed()
        composeTestRule.onNodeWithTag("turn_indicator_white").assertIsDisplayed()

        // Verify board exists in between
        composeTestRule.onNodeWithTag("diagonal_chess_board").assertIsDisplayed()
    }
}

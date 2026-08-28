package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.engine.GameState
import com.example.model.PlayerSide
import com.example.model.TimeControl
import com.example.ui.components.TurnIndicatorBar
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TurnIndicatorBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `TurnIndicatorBar displays active turn text when it is player's turn`() {
        val state = GameState(currentTurn = PlayerSide.WHITE)

        composeTestRule.setContent {
            MyApplicationTheme {
                TurnIndicatorBar(
                    state = state,
                    player = PlayerSide.WHITE,
                    isCurrentTurn = true,
                    isTimerPaused = false,
                    onResumeTimer = {},
                    currentTimeControl = TimeControl.UNLIMITED,
                    mirrored = false
                )
            }
        }

        composeTestRule.onNodeWithTag("turn_indicator_white").assertIsDisplayed()
        composeTestRule.onNodeWithText("👉 White's Turn").assertIsDisplayed()
    }

    @Test
    fun `TurnIndicatorBar displays waiting text when it is opponent's turn`() {
        val state = GameState(currentTurn = PlayerSide.WHITE)

        composeTestRule.setContent {
            MyApplicationTheme {
                TurnIndicatorBar(
                    state = state,
                    player = PlayerSide.BLACK,
                    isCurrentTurn = false,
                    isTimerPaused = false,
                    onResumeTimer = {},
                    currentTimeControl = TimeControl.UNLIMITED,
                    mirrored = true
                )
            }
        }

        composeTestRule.onNodeWithTag("turn_indicator_black").assertIsDisplayed()
        composeTestRule.onNodeWithText("⏳ White's Turn").assertIsDisplayed()
    }

    @Test
    fun `TurnIndicatorBar displays paused state and triggers resume callback`() {
        val state = GameState(currentTurn = PlayerSide.WHITE)
        var resumeClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                TurnIndicatorBar(
                    state = state,
                    player = PlayerSide.WHITE,
                    isCurrentTurn = true,
                    isTimerPaused = true,
                    onResumeTimer = { resumeClicked = true },
                    currentTimeControl = TimeControl.RAPID_5,
                    mirrored = false
                )
            }
        }

        composeTestRule.onNodeWithText("⏸ MATCH PAUSED").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_timer_btn_white").assertIsDisplayed().performClick()
        assertTrue(resumeClicked)
    }

    @Test
    fun `TurnIndicatorBar displays announcement text when available`() {
        val state = GameState(
            currentTurn = PlayerSide.WHITE,
            announcement = "👑 King Activated: You have 2 moves this turn!"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                TurnIndicatorBar(
                    state = state,
                    player = PlayerSide.WHITE,
                    isCurrentTurn = true,
                    isTimerPaused = false,
                    onResumeTimer = {},
                    currentTimeControl = TimeControl.UNLIMITED,
                    mirrored = false
                )
            }
        }

        composeTestRule.onNodeWithText("• 👑 King Activated: You have 2 moves this turn!").assertIsDisplayed()
    }
}

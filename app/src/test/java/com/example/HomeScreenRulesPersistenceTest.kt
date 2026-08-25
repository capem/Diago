package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRulesStorage
import com.example.model.BoardTheme
import com.example.model.GameRulesConfig
import com.example.ui.components.MatchRulesDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenRulesPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var storage: GameRulesStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(GameRulesStorage.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        storage = GameRulesStorage(context)
    }

    @Test
    fun `HomeScreen loads and displays previously saved rules`() {
        storage.saveRules(GameRulesConfig(lossPieceThreshold = 3, queenDistanceThreshold = 2))

        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    currentTheme = BoardTheme.OBSIDIAN_GOLD,
                    onThemeChange = {},
                    isAudioMuted = false,
                    onToggleAudio = {},
                    onStartGame = { _, _, _, _, _ -> },
                    onOpenCodex = {},
                    rulesStorage = storage
                )
            }
        }

        // Check that the summary text exists for the loaded persisted custom rules
        composeTestRule.onNodeWithText("≤3 loss • 2 border").assertExists()
    }

    @Test
    fun `MatchRulesDialog persists custom rules on apply`() {
        storage.saveRules(GameRulesConfig())

        composeTestRule.setContent {
            MyApplicationTheme {
                MatchRulesDialog(
                    currentConfig = storage.loadRules(),
                    onApply = { storage.saveRules(it) },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("match_rules_dialog").assertIsDisplayed()

        // Select loss step 4 and queen step 1 inside the scrollable dialog
        composeTestRule.onNodeWithTag("loss_step_4").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("queen_step_1").performScrollTo().performClick()

        // Apply rules
        composeTestRule.onNodeWithTag("apply_rules_btn").performScrollTo().performClick()

        // Verify storage now holds the updated rules
        val persisted = storage.loadRules()
        assertEquals(4, persisted.lossPieceThreshold)
        assertEquals(1, persisted.queenDistanceThreshold)
    }

    @Test
    fun `MatchRulesDialog reset defaults button persists standard rules`() {
        // Pre-save custom rules
        storage.saveRules(GameRulesConfig(lossPieceThreshold = 5, queenDistanceThreshold = 1))

        composeTestRule.setContent {
            MyApplicationTheme {
                MatchRulesDialog(
                    currentConfig = storage.loadRules(),
                    onApply = { storage.saveRules(it) },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("match_rules_dialog").assertIsDisplayed()

        // Click reset defaults inside the dialog
        composeTestRule.onNodeWithTag("reset_rules_btn").performScrollTo().performClick()

        // Click apply
        composeTestRule.onNodeWithTag("apply_rules_btn").performScrollTo().performClick()

        // Verify storage now holds standard defaults
        val persisted = storage.loadRules()
        assertEquals(0, persisted.lossPieceThreshold)
        assertEquals(6, persisted.queenDistanceThreshold)
        assertTrue(persisted.isDefault)
    }
}

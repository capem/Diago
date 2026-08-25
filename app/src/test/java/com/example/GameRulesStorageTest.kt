package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRulesStorage
import com.example.model.GameRulesConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameRulesStorageTest {

    private lateinit var context: Context
    private lateinit var storage: GameRulesStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences(GameRulesStorage.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        storage = GameRulesStorage(context)
    }

    @Test
    fun `loadRules returns standard defaults when empty`() {
        val rules = storage.loadRules()
        assertEquals(0, rules.lossPieceThreshold)
        assertEquals(6, rules.queenDistanceThreshold)
        assertTrue(rules.isDefault)
    }

    @Test
    fun `saveRules persists custom rules accurately`() {
        val customRules = GameRulesConfig(
            lossPieceThreshold = 4,
            queenDistanceThreshold = 2
        )
        storage.saveRules(customRules)

        val loaded = storage.loadRules()
        assertEquals(4, loaded.lossPieceThreshold)
        assertEquals(2, loaded.queenDistanceThreshold)
        assertFalse(loaded.isDefault)
    }

    @Test
    fun `saveRules and load across new storage instance`() {
        val customRules = GameRulesConfig(
            lossPieceThreshold = 3,
            queenDistanceThreshold = 1
        )
        storage.saveRules(customRules)

        // Create a new instance pointing to same context
        val newStorage = GameRulesStorage(context)
        val loaded = newStorage.loadRules()

        assertEquals(3, loaded.lossPieceThreshold)
        assertEquals(1, loaded.queenDistanceThreshold)
    }

    @Test
    fun `resetRules restores default config and persists it`() {
        storage.saveRules(GameRulesConfig(lossPieceThreshold = 5, queenDistanceThreshold = 0))
        assertFalse(storage.loadRules().isDefault)

        val resetConfig = storage.resetRules()
        assertTrue(resetConfig.isDefault)
        assertEquals(0, resetConfig.lossPieceThreshold)
        assertEquals(6, resetConfig.queenDistanceThreshold)

        // Verify loaded from storage is also reset
        val loaded = storage.loadRules()
        assertTrue(loaded.isDefault)
        assertEquals(0, loaded.lossPieceThreshold)
        assertEquals(6, loaded.queenDistanceThreshold)
    }

    @Test
    fun `saveRules clamps out of bounds values`() {
        storage.saveRules(GameRulesConfig(lossPieceThreshold = 99, queenDistanceThreshold = 99))
        val loadedHigh = storage.loadRules()
        assertEquals(9, loadedHigh.lossPieceThreshold)
        assertEquals(6, loadedHigh.queenDistanceThreshold)

        storage.saveRules(GameRulesConfig(lossPieceThreshold = -5, queenDistanceThreshold = -2))
        val loadedLow = storage.loadRules()
        assertEquals(0, loadedLow.lossPieceThreshold)
        assertEquals(0, loadedLow.queenDistanceThreshold)
    }

    @Test
    fun `loadRules safely handles corrupted preferences with out of bounds values`() {
        context.getSharedPreferences(GameRulesStorage.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(GameRulesStorage.KEY_LOSS_PIECE_THRESHOLD, -10)
            .putInt(GameRulesStorage.KEY_QUEEN_DISTANCE_THRESHOLD, 25)
            .commit()

        val loaded = storage.loadRules()
        assertEquals(0, loaded.lossPieceThreshold)
        assertEquals(6, loaded.queenDistanceThreshold)
    }
}

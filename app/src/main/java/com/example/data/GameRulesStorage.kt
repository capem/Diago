package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GameRulesConfig

/**
 * Storage manager for persisting and retrieving user-configured match rules.
 */
class GameRulesStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Loads the stored rules configuration. Falls back to defaults if not set.
     */
    fun loadRules(): GameRulesConfig {
        val lossThreshold = prefs.getInt(KEY_LOSS_PIECE_THRESHOLD, DEFAULT_LOSS_THRESHOLD)
        val queenDistance = prefs.getInt(KEY_QUEEN_DISTANCE_THRESHOLD, DEFAULT_QUEEN_DISTANCE)
        return GameRulesConfig(
            lossPieceThreshold = lossThreshold.coerceIn(MIN_LOSS_THRESHOLD, MAX_LOSS_THRESHOLD),
            queenDistanceThreshold = queenDistance.coerceIn(MIN_QUEEN_DISTANCE, MAX_QUEEN_DISTANCE)
        )
    }

    /**
     * Persists the given rules configuration.
     */
    fun saveRules(config: GameRulesConfig) {
        val safeLossThreshold = config.lossPieceThreshold.coerceIn(MIN_LOSS_THRESHOLD, MAX_LOSS_THRESHOLD)
        val safeQueenDistance = config.queenDistanceThreshold.coerceIn(MIN_QUEEN_DISTANCE, MAX_QUEEN_DISTANCE)
        prefs.edit()
            .putInt(KEY_LOSS_PIECE_THRESHOLD, safeLossThreshold)
            .putInt(KEY_QUEEN_DISTANCE_THRESHOLD, safeQueenDistance)
            .apply()
    }

    /**
     * Resets the rules to default standard values and saves them.
     */
    fun resetRules(): GameRulesConfig {
        val defaultConfig = GameRulesConfig()
        saveRules(defaultConfig)
        return defaultConfig
    }

    companion object {
        const val PREFS_NAME = "diago_rules_prefs"
        const val KEY_LOSS_PIECE_THRESHOLD = "rules_loss_piece_threshold"
        const val KEY_QUEEN_DISTANCE_THRESHOLD = "rules_queen_distance_threshold"
        const val DEFAULT_LOSS_THRESHOLD = 0
        const val DEFAULT_QUEEN_DISTANCE = 6
        const val MIN_LOSS_THRESHOLD = 0
        const val MAX_LOSS_THRESHOLD = 9
        const val MIN_QUEEN_DISTANCE = 0
        const val MAX_QUEEN_DISTANCE = 6
    }
}

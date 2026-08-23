package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Board grid coordinate (0..6, 0..6) on the 45° rotated diamond arena.
 * Total 49 squares (7 rows x 7 cols). 10 White pieces, 10 Black pieces, 29 neutral squares.
 */
data class Position(val row: Int, val col: Int) {
    val isValid: Boolean get() = row in 0..6 && col in 0..6

    /**
     * Diagonal rank index from 0 (top apex A7/0,0) to 12 (bottom apex G1/6,6).
     */
    val diagonalRank: Int get() = row + col

    fun notation(): String {
        val colLetter = ('A' + col)
        val rowNum = 7 - row
        return "$colLetter$rowNum"
    }

    fun isAdjacentStep(other: Position): Boolean =
        (kotlin.math.abs(row - other.row) + kotlin.math.abs(col - other.col)) == 1

    fun delta(dr: Int, dc: Int): Position = Position(row + dr, col + dc)
}

enum class PlayerSide(val displayName: String) {
    WHITE("White"),
    BLACK("Black");

    val isWhite: Boolean get() = this == WHITE

    fun opponent(): PlayerSide = if (this == WHITE) BLACK else WHITE

    /**
     * White advances towards top apex (row/col decreasing, d = row+col -> 0).
     * Black advances towards bottom apex (row/col increasing, d = row+col -> 12).
     */
    fun forwardDirections(): List<Pair<Int, Int>> =
        if (this == WHITE) listOf(-1 to 0, 0 to -1) else listOf(1 to 0, 0 to 1)

    fun backwardDirections(): List<Pair<Int, Int>> =
        if (this == WHITE) listOf(1 to 0, 0 to 1) else listOf(-1 to 0, 0 to -1)

    fun isPromotionGoal(pos: Position): Boolean =
        if (this == WHITE) pos.row == 0 || pos.col == 0 else pos.row == 6 || pos.col == 6

    fun isHomeTerritory(pos: Position): Boolean =
        if (this == WHITE) (pos.row + pos.col) >= 9 else (pos.row + pos.col) <= 3
}

data class Piece(
    val id: String,
    val player: PlayerSide,
    val isQueen: Boolean = false,
    val isRevived: Boolean = false,
    val capturedAt: Position? = null
) {
    fun withQueen(): Piece = copy(isQueen = true)
}

enum class Superpower(
    val title: String,
    val subtitle: String,
    val description: String,
    val emoji: String,
    val accentColor: Color
) {
    KING(
        title = "King",
        subtitle = "Double Move",
        description = "Make two moves in the same turn to double attack, reposition, or advance.",
        emoji = "👑",
        accentColor = Color(0xFFFFD700) // Gold
    ),
    QUEEN(
        title = "Queen",
        subtitle = "Queen Any Piece",
        description = "Instantly promote one of your pieces into a Queen/Dame piece, granting it the permanent ability to move and capture backward (one cell at a time).",
        emoji = "👸",
        accentColor = Color(0xFFFF4081) // Vibrant Magenta / Rose
    ),
    ROOK(
        title = "Rook",
        subtitle = "Infinite Range",
        description = "One piece gains unlimited diagonal slide distance in its allowed direction this turn.",
        emoji = "🏰",
        accentColor = Color(0xFF00E5FF) // Cyan
    ),
    BISHOP(
        title = "Bishop",
        subtitle = "Teleport",
        description = "Teleport one of your pieces to ANY empty square on the board, bypassing all movement paths.",
        emoji = "🏹",
        accentColor = Color(0xFF7C4DFF) // Deep Violet
    ),
    KNIGHT(
        title = "Knight",
        subtitle = "Backward Move",
        description = "Allow your pieces to move or capture diagonally backward for this turn.",
        emoji = "🐴",
        accentColor = Color(0xFFFF9100) // Amber Orange
    ),
    PAWN(
        title = "Pawn",
        subtitle = "Revive",
        description = "Revive your last captured piece back into its exact old place on the diamond board.",
        emoji = "♟",
        accentColor = Color(0xFF00E676) // Emerald Green
    );
}

enum class GameMode(val title: String, val subtitle: String) {
    AI("Single Player", "Battle against intelligent AI Tacticians"),
    PASS_AND_PLAY("Pass & Play", "Local 2-player match on one device"),
    PUZZLES("Tactical Puzzles", "Master superpowers in 10 tactical challenges"),
    TUTORIAL("Codex & Rules", "Interactive guides & superpower sandbox")
}

enum class AiDifficulty(val title: String, val description: String) {
    NOVICE("Novice", "Friendly AI, casual power usage"),
    TACTICIAN("Tactician", "Strategic play with smart superpower timing"),
    GRANDMASTER("Grandmaster", "Deep search with aggressive superpower combinations")
}

enum class GameStatus {
    PLAYING,
    WHITE_WON,
    BLACK_WON,
    DRAW
}

data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val capturedPos: Position? = null,
    val superpowerUsed: Superpower? = null,
    val isPromotion: Boolean = false,
    val isTeleport: Boolean = false,
    val isRevival: Boolean = false,
    val isDoubleMovePart: Int? = null // 1 or 2
) {
    fun summary(): String {
        return when {
            isRevival -> "♟ Revived piece at ${to.notation()}"
            isTeleport -> "🏹 Teleported ${from.notation()} ➜ ${to.notation()}"
            capturedPiece != null -> "${from.notation()} ⚔ ${to.notation()}${if (isPromotion) " (♛ Promoted)" else ""}"
            else -> "${from.notation()} ➜ ${to.notation()}${if (isPromotion) " (♛ Promoted)" else ""}"
        }
    }
}

enum class BoardTheme(val title: String, val darkTile: Color, val lightTile: Color, val boardBg: Color, val accent: Color) {
    OBSIDIAN_GOLD(
        title = "Obsidian & Gold",
        darkTile = Color(0xFF2C243B),
        lightTile = Color(0xFFEADBCE),
        boardBg = Color(0xFF161224),
        accent = Color(0xFFFFD700)
    ),
    MIDNIGHT_CYAN(
        title = "Midnight Cyber",
        darkTile = Color(0xFF132B45),
        lightTile = Color(0xFF8FE3FF),
        boardBg = Color(0xFF081420),
        accent = Color(0xFF00E5FF)
    ),
    ROYAL_VELVET(
        title = "Royal Velvet",
        darkTile = Color(0xFF4A1525),
        lightTile = Color(0xFFF7E6D0),
        boardBg = Color(0xFF240810),
        accent = Color(0xFFFF4081)
    ),
    EMERALD_JADE(
        title = "Emerald Jade",
        darkTile = Color(0xFF133826),
        lightTile = Color(0xFFD4EED8),
        boardBg = Color(0xFF0A1F15),
        accent = Color(0xFF00E676)
    )
}

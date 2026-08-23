package com.example.engine

import com.example.model.GameStatus
import com.example.model.Move
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position
import com.example.model.Superpower

/**
 * Full state of a Diagonal Chess game on a 7×7 diamond arena.
 */
data class GameState(
    val board: Map<Position, Piece> = initialBoard(),
    val currentTurn: PlayerSide = PlayerSide.WHITE,
    val whitePowers: Set<Superpower> = Superpower.entries.toSet(),
    val blackPowers: Set<Superpower> = Superpower.entries.toSet(),
    val whiteGraveyard: List<Piece> = emptyList(), // Captured white pieces (captured by black)
    val blackGraveyard: List<Piece> = emptyList(), // Captured black pieces (captured by white)
    val status: GameStatus = GameStatus.PLAYING,
    val activePower: Superpower? = null,
    val kingMoveCount: Int = 0, // 0: no king power, 1: first move made, waiting for second move
    val chainCapturePos: Position? = null, // Set during multi-eating jump combos
    val moveHistory: List<Move> = emptyList(),
    val selectedPos: Position? = null,
    val candidateMoves: List<Move> = emptyList(),
    val isPromotingQueenMode: Boolean = false,
    val isRevivingPawnMode: Boolean = false,
    val isBishopTeleportMode: Boolean = false,
    val announcement: String? = null
) {
    fun remainingPowers(player: PlayerSide): Set<Superpower> =
        if (player == PlayerSide.WHITE) whitePowers else blackPowers

    fun graveyard(player: PlayerSide): List<Piece> =
        if (player == PlayerSide.WHITE) whiteGraveyard else blackGraveyard

    fun pieceAt(pos: Position): Piece? = board[pos]

    fun isGameOver(): Boolean = status != GameStatus.PLAYING

    companion object {
        fun initialBoard(): Map<Position, Piece> {
            val grid = mutableMapOf<Position, Piece>()
            var bIndex = 0
            var wIndex = 0
            for (r in 0..6) {
                for (c in 0..6) {
                    val d = r + c
                    if (d <= 3) {
                        grid[Position(r, c)] = Piece(id = "b_${bIndex++}", player = PlayerSide.BLACK)
                    } else if (d >= 9) {
                        grid[Position(r, c)] = Piece(id = "w_${wIndex++}", player = PlayerSide.WHITE)
                    }
                    // d in 4..8 is the neutral open battlefield (29 empty tiles)
                }
            }
            return grid
        }
    }
}

object GameEngine {

    /**
     * Generate all legal moves for a piece at [from] under current [state].
     */
    fun getLegalMovesForPosition(state: GameState, from: Position): List<Move> {
        val piece = state.board[from] ?: return emptyList()
        if (piece.player != state.currentTurn) return emptyList()

        // If in a multi-jump chain, ONLY the piece currently combo-jumping may move, and ONLY captures are legal
        if (state.chainCapturePos != null) {
            if (from != state.chainCapturePos) return emptyList()
            return getRawCapturesForPosition(state, from, piece)
        }

        // If Bishop teleport mode is active, any empty tile on the diamond board is a legal destination
        if (state.isBishopTeleportMode) {
            val moves = mutableListOf<Move>()
            for (r in 0..6) {
                for (c in 0..6) {
                    val dest = Position(r, c)
                    if (!state.board.containsKey(dest)) {
                        val isPromotion = !piece.isQueen && piece.player.isPromotionGoal(dest)
                        moves.add(
                            Move(
                                from = from,
                                to = dest,
                                piece = piece,
                                superpowerUsed = Superpower.BISHOP,
                                isTeleport = true,
                                isPromotion = isPromotion
                            )
                        )
                    }
                }
            }
            return moves
        }

        val legalMoves = mutableListOf<Move>()
        val isQueen = piece.isQueen
        val isRookActive = state.activePower == Superpower.ROOK
        val isKnightActive = state.activePower == Superpower.KNIGHT

        // Determine diagonal movement directions in 45° rotated space
        val directions = mutableListOf<Pair<Int, Int>>()
        directions.addAll(piece.player.forwardDirections())

        if (isQueen || isKnightActive) {
            directions.addAll(piece.player.backwardDirections())
        }

        for ((dr, dc) in directions) {
            if (isRookActive) {
                // ROOK SUPERPOWER: Infinite sliding range + infinite range jump captures
                // 1. Sliding along empty squares
                var foundEnemy = false
                var enemyPiece: Piece? = null
                var enemyPos: Position? = null

                for (step in 1..6) {
                    val nextPos = Position(from.row + dr * step, from.col + dc * step)
                    if (!nextPos.isValid) break

                    val tilePiece = state.board[nextPos]
                    if (!foundEnemy) {
                        if (tilePiece == null) {
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(nextPos)
                            legalMoves.add(
                                Move(
                                    from = from,
                                    to = nextPos,
                                    piece = piece,
                                    superpowerUsed = Superpower.ROOK,
                                    isPromotion = isPromotion
                                )
                            )
                        } else if (tilePiece.player != piece.player) {
                            // First obstacle is enemy piece: can jump over it and land on any empty square behind it
                            foundEnemy = true
                            enemyPiece = tilePiece
                            enemyPos = nextPos
                        } else {
                            // Friendly piece blocks further line
                            break
                        }
                    } else {
                        // After the enemy piece: any empty tile along the line is a legal landing square
                        if (tilePiece == null) {
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(nextPos)
                            legalMoves.add(
                                Move(
                                    from = from,
                                    to = nextPos,
                                    piece = piece,
                                    capturedPiece = enemyPiece,
                                    capturedPos = enemyPos,
                                    superpowerUsed = Superpower.ROOK,
                                    isPromotion = isPromotion
                                )
                            )
                        } else {
                            // Obstructed behind the captured piece
                            break
                        }
                    }
                }
            } else {
                // STANDARD MOVEMENT & JUMPING (Regular pieces & Queened pieces move 1 cell at a time)
                // 1. Step to adjacent empty square (distance 1)
                val stepPos = Position(from.row + dr, from.col + dc)
                if (stepPos.isValid) {
                    val tilePiece = state.board[stepPos]
                    if (tilePiece == null) {
                        val isPromotion = !isQueen && piece.player.isPromotionGoal(stepPos)
                        legalMoves.add(
                            Move(
                                from = from,
                                to = stepPos,
                                piece = piece,
                                superpowerUsed = state.activePower,
                                isPromotion = isPromotion
                            )
                        )
                    } else if (tilePiece.player != piece.player) {
                        // Adjacent enemy piece: check landing square directly behind it (distance 2)
                        val landingPos = Position(from.row + dr * 2, from.col + dc * 2)
                        if (landingPos.isValid && !state.board.containsKey(landingPos)) {
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(landingPos)
                            legalMoves.add(
                                Move(
                                    from = from,
                                    to = landingPos,
                                    piece = piece,
                                    capturedPiece = tilePiece,
                                    capturedPos = stepPos,
                                    superpowerUsed = state.activePower,
                                    isPromotion = isPromotion
                                )
                            )
                        }
                    }
                }
            }
        }

        return legalMoves
    }

    /**
     * Helper to compute raw capture moves from a position.
     */
    private fun getRawCapturesForPosition(state: GameState, from: Position, piece: Piece): List<Move> {
        val captures = mutableListOf<Move>()
        val isQueen = piece.isQueen
        val isRookActive = state.activePower == Superpower.ROOK
        val isKnightActive = state.activePower == Superpower.KNIGHT

        val directions = mutableListOf<Pair<Int, Int>>()
        directions.addAll(piece.player.forwardDirections())
        if (isQueen || isKnightActive) {
            directions.addAll(piece.player.backwardDirections())
        }

        for ((dr, dc) in directions) {
            if (isRookActive) {
                var foundEnemy = false
                var enemyPiece: Piece? = null
                var enemyPos: Position? = null

                for (step in 1..6) {
                    val nextPos = Position(from.row + dr * step, from.col + dc * step)
                    if (!nextPos.isValid) break

                    val tilePiece = state.board[nextPos]
                    if (!foundEnemy) {
                        if (tilePiece != null) {
                            if (tilePiece.player != piece.player) {
                                foundEnemy = true
                                enemyPiece = tilePiece
                                enemyPos = nextPos
                            } else {
                                break
                            }
                        }
                    } else {
                        if (tilePiece == null) {
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(nextPos)
                            captures.add(
                                Move(
                                    from = from,
                                    to = nextPos,
                                    piece = piece,
                                    capturedPiece = enemyPiece,
                                    capturedPos = enemyPos,
                                    superpowerUsed = Superpower.ROOK,
                                    isPromotion = isPromotion
                                )
                            )
                        } else {
                            break
                        }
                    }
                }
            } else {
                val stepPos = Position(from.row + dr, from.col + dc)
                if (stepPos.isValid) {
                    val tilePiece = state.board[stepPos]
                    if (tilePiece != null && tilePiece.player != piece.player) {
                        val landingPos = Position(from.row + dr * 2, from.col + dc * 2)
                        if (landingPos.isValid && !state.board.containsKey(landingPos)) {
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(landingPos)
                            captures.add(
                                Move(
                                    from = from,
                                    to = landingPos,
                                    piece = piece,
                                    capturedPiece = tilePiece,
                                    capturedPos = stepPos,
                                    superpowerUsed = state.activePower,
                                    isPromotion = isPromotion
                                )
                            )
                        }
                    }
                }
            }
        }
        return captures
    }

    /**
     * Get capture moves available for a specific position.
     */
    fun getLegalCapturesForPosition(state: GameState, from: Position): List<Move> {
        val piece = state.board[from] ?: return emptyList()
        if (piece.player != state.currentTurn) return emptyList()
        return getRawCapturesForPosition(state, from, piece)
    }

    /**
     * Get all legal moves for the current player in [state].
     */
    fun getAllLegalMoves(state: GameState): List<Move> {
        if (state.chainCapturePos != null) {
            return getLegalMovesForPosition(state, state.chainCapturePos)
        }
        val allMoves = mutableListOf<Move>()
        for ((pos, piece) in state.board) {
            if (piece.player == state.currentTurn) {
                allMoves.addAll(getLegalMovesForPosition(state, pos))
            }
        }
        return allMoves
    }

    /**
     * Valid placement squares for Pawn Revive (targets exact old place where piece was captured).
     * If the exact old place is currently occupied, falls back to home territory empty squares, then all empty squares.
     */
    fun getReviveDestinations(state: GameState, player: PlayerSide): List<Position> {
        val graveyard = state.graveyard(player)
        if (graveyard.isEmpty()) return emptyList()
        val lastCaptured = graveyard.last()
        val exactPos = lastCaptured.capturedAt

        if (exactPos != null && !state.board.containsKey(exactPos)) {
            return listOf(exactPos)
        }

        // If exact square is occupied, fallback to empty squares on home territory
        val homeEmpty = mutableListOf<Position>()
        for (r in 0..6) {
            for (c in 0..6) {
                val pos = Position(r, c)
                if (player.isHomeTerritory(pos) && !state.board.containsKey(pos)) {
                    homeEmpty.add(pos)
                }
            }
        }
        if (homeEmpty.isNotEmpty()) return homeEmpty

        // Fallback: entire diamond board empty squares
        return (0..6).flatMap { r -> (0..6).map { c -> Position(r, c) } }
            .filter { !state.board.containsKey(it) }
    }

    /**
     * Executes a move on the game state and returns the next [GameState].
     * Supports multiple eating / checkers-style jump combos and superpower consumption.
     */
    fun applyMove(state: GameState, move: Move): GameState {
        val newBoard = state.board.toMutableMap()
        val player = state.currentTurn
        val opponent = player.opponent()

        // Move piece
        newBoard.remove(move.from)
        val isNewlyPromoted = move.isPromotion || (!move.piece.isQueen && player.isPromotionGoal(move.to))
        val movedPiece = if (isNewlyPromoted) move.piece.withQueen() else move.piece
        newBoard[move.to] = movedPiece

        // Handle capture
        var newWhiteGraveyard = state.whiteGraveyard
        var newBlackGraveyard = state.blackGraveyard

        if (move.capturedPiece != null && move.capturedPos != null) {
            newBoard.remove(move.capturedPos)
            val capturedWithPos = move.capturedPiece.copy(capturedAt = move.capturedPos)
            if (move.capturedPiece.player == PlayerSide.WHITE) {
                newWhiteGraveyard = newWhiteGraveyard + capturedWithPos
            } else {
                newBlackGraveyard = newBlackGraveyard + capturedWithPos
            }
        }

        // Superpower consumption: consume either state.activePower or move.superpowerUsed
        val powerToConsume = state.activePower ?: move.superpowerUsed
        var newWhitePowers = state.whitePowers
        var newBlackPowers = state.blackPowers

        if (powerToConsume != null) {
            if (player == PlayerSide.WHITE) {
                newWhitePowers = newWhitePowers - powerToConsume
            } else {
                newBlackPowers = newBlackPowers - powerToConsume
            }
        }

        val isRookActive = (state.activePower == Superpower.ROOK || move.superpowerUsed == Superpower.ROOK)
        val isCapture = move.capturedPiece != null

        // Check if piece can chain further captures from landing square
        val continuationState = GameState(
            board = newBoard,
            currentTurn = player,
            activePower = if (isRookActive) Superpower.ROOK else null,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers
        )

        val nextCaptures = if (isCapture) {
            getRawCapturesForPosition(continuationState, move.to, movedPiece)
        } else emptyList()

        if (nextCaptures.isNotEmpty()) {
            // MULTI-JUMP CHAIN: Player continues turn with subsequent jump
            val updatedHistory = state.moveHistory + move
            return GameState(
                board = newBoard,
                currentTurn = player,
                whitePowers = newWhitePowers,
                blackPowers = newBlackPowers,
                whiteGraveyard = newWhiteGraveyard,
                blackGraveyard = newBlackGraveyard,
                status = checkWinCondition(newBoard, player),
                activePower = if (isRookActive) Superpower.ROOK else null,
                kingMoveCount = state.kingMoveCount,
                chainCapturePos = move.to,
                moveHistory = updatedHistory,
                selectedPos = move.to,
                candidateMoves = nextCaptures,
                isPromotingQueenMode = false,
                isRevivingPawnMode = false,
                isBishopTeleportMode = false,
                announcement = "⚔ Combo Jump! Piece at ${move.to.notation()} can jump again."
            )
        }

        // Capture sequence complete (or non-capture move). Handle turn progression:
        var nextKingCount = state.kingMoveCount
        var nextTurn = player
        var nextActivePower: Superpower? = null

        val isKingActive = (state.activePower == Superpower.KING || move.superpowerUsed == Superpower.KING || state.kingMoveCount == 1)

        if (isKingActive) {
            if (state.kingMoveCount == 0) {
                // First move completed, player gets 2nd move
                nextKingCount = 1
                nextTurn = player
                nextActivePower = null
            } else {
                // Second move completed! Turn passes to opponent
                nextKingCount = 0
                nextTurn = opponent
                nextActivePower = null
            }
        } else {
            // Turn passes to opponent
            nextTurn = opponent
            nextActivePower = null
            nextKingCount = 0
        }

        val updatedHistory = state.moveHistory + move
        val status = checkWinCondition(newBoard, nextTurn)

        val announcement = when {
            status == GameStatus.WHITE_WON -> "🏆 White claims victory!"
            status == GameStatus.BLACK_WON -> "🏆 Black claims victory!"
            nextKingCount == 1 -> "👑 King Double Move: Make your 2nd move!"
            isNewlyPromoted -> "♛ Piece promoted to Queen/Dame at enemy border!"
            move.capturedPiece != null -> "${player.displayName} captured ${opponent.displayName} piece!"
            else -> null
        }

        return GameState(
            board = newBoard,
            currentTurn = nextTurn,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers,
            whiteGraveyard = newWhiteGraveyard,
            blackGraveyard = newBlackGraveyard,
            status = status,
            activePower = nextActivePower,
            kingMoveCount = nextKingCount,
            chainCapturePos = null,
            moveHistory = updatedHistory,
            selectedPos = null,
            candidateMoves = emptyList(),
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            announcement = announcement
        )
    }

    /**
     * Concludes multi-jump combo if player chooses not to jump further.
     */
    fun finishMultiJump(state: GameState): GameState {
        if (state.chainCapturePos == null) return state
        val player = state.currentTurn
        val opponent = player.opponent()

        var nextKingCount = state.kingMoveCount
        var nextTurn = opponent

        if (state.kingMoveCount == 1) {
            nextKingCount = 0
            nextTurn = opponent
        }

        val status = checkWinCondition(state.board, nextTurn)
        return state.copy(
            currentTurn = nextTurn,
            activePower = null,
            kingMoveCount = nextKingCount,
            chainCapturePos = null,
            selectedPos = null,
            candidateMoves = emptyList(),
            status = status,
            announcement = "${player.displayName} completed capture sequence."
        )
    }

    /**
     * Executes Queen transformation superpower on a selected piece.
     */
    fun applyQueenTransformation(state: GameState, targetPos: Position): GameState {
        val piece = state.board[targetPos] ?: return state
        if (piece.player != state.currentTurn) return state

        val newBoard = state.board.toMutableMap()
        newBoard[targetPos] = piece.withQueen()

        val player = state.currentTurn
        val newWhitePowers = if (player == PlayerSide.WHITE) state.whitePowers - Superpower.QUEEN else state.whitePowers
        val newBlackPowers = if (player == PlayerSide.BLACK) state.blackPowers - Superpower.QUEEN else state.blackPowers

        val move = Move(
            from = targetPos,
            to = targetPos,
            piece = piece.withQueen(),
            superpowerUsed = Superpower.QUEEN,
            isPromotion = true
        )

        val nextTurn = player.opponent()
        val status = checkWinCondition(newBoard, nextTurn)

        return GameState(
            board = newBoard,
            currentTurn = nextTurn,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers,
            whiteGraveyard = state.whiteGraveyard,
            blackGraveyard = state.blackGraveyard,
            status = status,
            activePower = null,
            kingMoveCount = 0,
            chainCapturePos = null,
            moveHistory = state.moveHistory + move,
            selectedPos = null,
            candidateMoves = emptyList(),
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            announcement = "👸 ${player.displayName} transformed piece at ${targetPos.notation()} into Queen/Dame!"
        )
    }

    /**
     * Executes Pawn Revive placement in the exact old place where the piece was captured (or chosen square).
     */
    fun applyPawnRevival(state: GameState, targetPos: Position? = null): GameState {
        val player = state.currentTurn
        val graveyard = state.graveyard(player)
        if (graveyard.isEmpty()) return state

        val lastCaptured = graveyard.last()
        val destPos = targetPos ?: lastCaptured.capturedAt ?: getReviveDestinations(state, player).firstOrNull() ?: return state
        if (state.board.containsKey(destPos)) return state

        val pieceToRevive = lastCaptured.copy(isRevived = true, isQueen = false, capturedAt = null)
        val newGraveyard = graveyard.dropLast(1)
        val newBoard = state.board.toMutableMap()
        newBoard[destPos] = pieceToRevive

        val newWhiteGraveyard = if (player == PlayerSide.WHITE) newGraveyard else state.whiteGraveyard
        val newBlackGraveyard = if (player == PlayerSide.BLACK) newGraveyard else state.blackGraveyard

        val newWhitePowers = if (player == PlayerSide.WHITE) state.whitePowers - Superpower.PAWN else state.whitePowers
        val newBlackPowers = if (player == PlayerSide.BLACK) state.blackPowers - Superpower.PAWN else state.blackPowers

        val move = Move(
            from = destPos,
            to = destPos,
            piece = pieceToRevive,
            superpowerUsed = Superpower.PAWN,
            isRevival = true
        )

        val nextTurn = player.opponent()
        val status = checkWinCondition(newBoard, nextTurn)

        return GameState(
            board = newBoard,
            currentTurn = nextTurn,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers,
            whiteGraveyard = newWhiteGraveyard,
            blackGraveyard = newBlackGraveyard,
            status = status,
            activePower = null,
            kingMoveCount = 0,
            chainCapturePos = null,
            moveHistory = state.moveHistory + move,
            selectedPos = null,
            candidateMoves = emptyList(),
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            announcement = "♟ ${player.displayName} revived piece back at exact old position ${destPos.notation()}!"
        )
    }

    /**
     * Checks if either side has won (all opponent pieces captured or no legal moves).
     */
    fun checkWinCondition(board: Map<Position, Piece>, nextTurnPlayer: PlayerSide): GameStatus {
        val whitePieces = board.values.filter { it.player == PlayerSide.WHITE }
        val blackPieces = board.values.filter { it.player == PlayerSide.BLACK }

        if (whitePieces.isEmpty()) return GameStatus.BLACK_WON
        if (blackPieces.isEmpty()) return GameStatus.WHITE_WON

        // Check if nextTurnPlayer has any legal regular moves
        val dummyState = GameState(board = board, currentTurn = nextTurnPlayer)
        val legalMoves = getAllLegalMoves(dummyState)
        if (legalMoves.isEmpty()) {
            // Player has no moves left - opponent wins
            return if (nextTurnPlayer == PlayerSide.WHITE) GameStatus.BLACK_WON else GameStatus.WHITE_WON
        }

        return GameStatus.PLAYING
    }
}

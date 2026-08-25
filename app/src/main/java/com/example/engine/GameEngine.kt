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
    val positionHistory: List<String> = listOf(initialSignature()),
    val selectedPos: Position? = null,
    val candidateMoves: List<Move> = emptyList(),
    val isPromotingQueenMode: Boolean = false,
    val isRevivingPawnMode: Boolean = false,
    val isBishopTeleportMode: Boolean = false,
    val announcement: String? = null,
    val rulesConfig: com.example.model.GameRulesConfig = com.example.model.GameRulesConfig()
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

        fun initialSignature(): String {
            return computeSignature(initialBoard(), PlayerSide.WHITE, Superpower.entries.toSet(), Superpower.entries.toSet(), null, 0)
        }

        fun computeSignature(
            board: Map<Position, Piece>,
            currentTurn: PlayerSide,
            whitePowers: Set<Superpower>,
            blackPowers: Set<Superpower>,
            activePower: Superpower?,
            kingMoveCount: Int
        ): String {
            val sb = StringBuilder()
            sb.append(currentTurn.name[0]).append("|")
            for (r in 0..6) {
                for (c in 0..6) {
                    val p = board[Position(r, c)]
                    if (p != null) {
                        sb.append(r).append(c).append(p.player.name[0]).append(if (p.isQueen) 'Q' else 'P').append(';')
                    }
                }
            }
            sb.append("|W:")
            whitePowers.sortedBy { it.name }.forEach { sb.append(it.name[0]) }
            sb.append("|B:")
            blackPowers.sortedBy { it.name }.forEach { sb.append(it.name[0]) }
            sb.append("|A:").append(activePower?.name?.get(0) ?: '-')
            sb.append("|K:").append(kingMoveCount)
            return sb.toString()
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
                        val isPromotion = !piece.isQueen && piece.player.isPromotionGoal(dest, state.rulesConfig.queenDistanceThreshold)
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
        val queenThresh = state.rulesConfig.queenDistanceThreshold

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
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(nextPos, queenThresh)
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
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(nextPos, queenThresh)
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
                        val isPromotion = !isQueen && piece.player.isPromotionGoal(stepPos, queenThresh)
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
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(landingPos, queenThresh)
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
        val queenThresh = state.rulesConfig.queenDistanceThreshold

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
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(nextPos, queenThresh)
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
                            val isPromotion = !isQueen && piece.player.isPromotionGoal(landingPos, queenThresh)
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
     * Check if a player is legally allowed to use the Pawn (Revive) superpower.
     * Constraint: A piece can ONLY be revived on the immediate turn following its capture.
     * If the player made any moves after the capture or the turn window passed, revival is forbidden.
     */
    fun canRevivePawn(state: GameState, player: PlayerSide = state.currentTurn): Boolean {
        if (!state.remainingPowers(player).contains(Superpower.PAWN)) return false
        val graveyard = state.graveyard(player)
        if (graveyard.isEmpty()) return false

        // Find the index of the latest move in history that captured a piece belonging to player
        val lastCaptureIdx = state.moveHistory.indexOfLast { it.capturedPiece?.player == player }
        if (lastCaptureIdx == -1) return false

        // Check moves that occurred after that capture
        val movesAfterCapture = state.moveHistory.subList(lastCaptureIdx + 1, state.moveHistory.size)
        // If the player has made ANY move since the capture occurred, the turn window has passed
        if (movesAfterCapture.any { it.piece.player == player }) return false

        return true
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
        val isNewlyPromoted = move.isPromotion || (!move.piece.isQueen && player.isPromotionGoal(move.to, state.rulesConfig.queenDistanceThreshold))
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
            blackPowers = newBlackPowers,
            rulesConfig = state.rulesConfig
        )

        val nextCaptures = if (isCapture) {
            getRawCapturesForPosition(continuationState, move.to, movedPiece)
        } else emptyList()

        val isKingActive = (state.activePower == Superpower.KING || move.superpowerUsed == Superpower.KING || state.kingMoveCount == 1)

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
                status = checkWinCondition(newBoard, player, rulesConfig = state.rulesConfig),
                activePower = if (isRookActive) Superpower.ROOK else if (isKingActive) Superpower.KING else null,
                kingMoveCount = state.kingMoveCount,
                chainCapturePos = move.to,
                moveHistory = updatedHistory,
                selectedPos = move.to,
                candidateMoves = nextCaptures,
                isPromotingQueenMode = false,
                isRevivingPawnMode = false,
                isBishopTeleportMode = false,
                announcement = "⚔ Combo Jump! Piece at ${move.to.notation()} can jump again.",
                rulesConfig = state.rulesConfig
            )
        }

        // Capture sequence complete (or non-capture move). Handle turn progression:
        var nextKingCount = state.kingMoveCount
        var nextTurn = player
        var nextActivePower: Superpower? = null

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
        val nextSignature = GameState.computeSignature(
            board = newBoard,
            currentTurn = nextTurn,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers,
            activePower = nextActivePower,
            kingMoveCount = nextKingCount
        )
        val updatedPositionHistory = state.positionHistory + nextSignature
        val status = checkWinCondition(newBoard, nextTurn, state.positionHistory, nextSignature, rulesConfig = state.rulesConfig)

        val announcement = when {
            status == GameStatus.WHITE_WON -> "🏆 White claims victory!"
            status == GameStatus.BLACK_WON -> "🏆 Black claims victory!"
            status == GameStatus.DRAW_STALEMATE -> "🤝 Stalemate! ${nextTurn.displayName} has no legal moves. Game drawn."
            status == GameStatus.DRAW_REPETITION -> "🤝 Draw by Threefold Repetition! Same position occurred 3 times."
            status == GameStatus.DRAW -> "🤝 Draw game!"
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
            positionHistory = updatedPositionHistory,
            selectedPos = null,
            candidateMoves = emptyList(),
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            announcement = announcement,
            rulesConfig = state.rulesConfig
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

        if (state.activePower == Superpower.KING && state.kingMoveCount == 0) {
            nextKingCount = 1
            nextTurn = player
        } else if (state.kingMoveCount == 1) {
            nextKingCount = 0
            nextTurn = opponent
        }

        val nextSignature = GameState.computeSignature(
            board = state.board,
            currentTurn = nextTurn,
            whitePowers = state.whitePowers,
            blackPowers = state.blackPowers,
            activePower = null,
            kingMoveCount = nextKingCount
        )
        val updatedPositionHistory = state.positionHistory + nextSignature
        val status = checkWinCondition(state.board, nextTurn, state.positionHistory, nextSignature, rulesConfig = state.rulesConfig)
        val announcement = when (status) {
            GameStatus.WHITE_WON -> "🏆 White claims victory!"
            GameStatus.BLACK_WON -> "🏆 Black claims victory!"
            GameStatus.DRAW_STALEMATE -> "🤝 Stalemate! ${nextTurn.displayName} has no legal moves. Game drawn."
            GameStatus.DRAW_REPETITION -> "🤝 Draw by Threefold Repetition! Same position occurred 3 times."
            GameStatus.DRAW -> "🤝 Draw game!"
            else -> "${player.displayName} completed capture sequence."
        }
        return state.copy(
            currentTurn = nextTurn,
            activePower = null,
            kingMoveCount = nextKingCount,
            chainCapturePos = null,
            selectedPos = null,
            candidateMoves = emptyList(),
            positionHistory = updatedPositionHistory,
            status = status,
            announcement = announcement
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
        val nextSignature = GameState.computeSignature(
            board = newBoard,
            currentTurn = nextTurn,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers,
            activePower = null,
            kingMoveCount = 0
        )
        val updatedPositionHistory = state.positionHistory + nextSignature
        val status = checkWinCondition(newBoard, nextTurn, state.positionHistory, nextSignature, rulesConfig = state.rulesConfig)

        val announcement = when (status) {
            GameStatus.WHITE_WON -> "🏆 White claims victory!"
            GameStatus.BLACK_WON -> "🏆 Black claims victory!"
            GameStatus.DRAW_STALEMATE -> "🤝 Stalemate! ${nextTurn.displayName} has no legal moves. Game drawn."
            GameStatus.DRAW_REPETITION -> "🤝 Draw by Threefold Repetition! Same position occurred 3 times."
            GameStatus.DRAW -> "🤝 Draw game!"
            else -> "👸 ${player.displayName} transformed piece at ${targetPos.notation()} into Queen/Dame!"
        }

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
            positionHistory = updatedPositionHistory,
            selectedPos = null,
            candidateMoves = emptyList(),
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            announcement = announcement,
            rulesConfig = state.rulesConfig
        )
    }

    /**
     * Executes Pawn Revive placement in the exact old place where the piece was captured (or chosen square).
     * Constraint: Cannot revive if turn has passed or player already made a move since the capture.
     */
    fun applyPawnRevival(state: GameState, targetPos: Position? = null): GameState {
        val player = state.currentTurn
        if (!canRevivePawn(state, player)) {
            return state.copy(
                announcement = "⚠️ Cannot revive: piece can only be revived on the immediate turn after it was captured!"
            )
        }

        val graveyard = state.graveyard(player)
        if (graveyard.isEmpty()) return state

        val lastCaptured = graveyard.last()
        val destPos = targetPos ?: lastCaptured.capturedAt ?: getReviveDestinations(state, player).firstOrNull() ?: return state
        if (state.board.containsKey(destPos)) return state

        val pieceToRevive = lastCaptured.copy(isRevived = true, isQueen = lastCaptured.isQueen, capturedAt = null)
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
        val nextSignature = GameState.computeSignature(
            board = newBoard,
            currentTurn = nextTurn,
            whitePowers = newWhitePowers,
            blackPowers = newBlackPowers,
            activePower = null,
            kingMoveCount = 0
        )
        val updatedPositionHistory = state.positionHistory + nextSignature
        val status = checkWinCondition(newBoard, nextTurn, state.positionHistory, nextSignature, rulesConfig = state.rulesConfig)

        val pieceTypeLabel = if (pieceToRevive.isQueen) "Queen/Dame piece 👑" else "piece"
        val announcement = when (status) {
            GameStatus.WHITE_WON -> "🏆 White claims victory!"
            GameStatus.BLACK_WON -> "🏆 Black claims victory!"
            GameStatus.DRAW_STALEMATE -> "🤝 Stalemate! ${nextTurn.displayName} has no legal moves. Game drawn."
            GameStatus.DRAW_REPETITION -> "🤝 Draw by Threefold Repetition! Same position occurred 3 times."
            GameStatus.DRAW -> "🤝 Draw game!"
            else -> "♟ ${player.displayName} revived $pieceTypeLabel back at exact old position ${destPos.notation()}!"
        }

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
            positionHistory = updatedPositionHistory,
            selectedPos = null,
            candidateMoves = emptyList(),
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            announcement = announcement,
            rulesConfig = state.rulesConfig
        )
    }

    /**
     * Checks if either side has won (all opponent pieces captured or piece threshold reached) or if game is drawn (stalemate or repetition).
     */
    fun checkWinCondition(
        board: Map<Position, Piece>,
        nextTurnPlayer: PlayerSide,
        positionHistory: List<String> = emptyList(),
        currentSignature: String? = null,
        rulesConfig: com.example.model.GameRulesConfig = com.example.model.GameRulesConfig()
    ): GameStatus {
        val whitePiecesCount = board.values.count { it.player == PlayerSide.WHITE }
        val blackPiecesCount = board.values.count { it.player == PlayerSide.BLACK }
        val lossThreshold = rulesConfig.lossPieceThreshold

        val whiteLost = whitePiecesCount <= lossThreshold
        val blackLost = blackPiecesCount <= lossThreshold

        if (whiteLost && blackLost) return GameStatus.DRAW
        if (whiteLost) return GameStatus.BLACK_WON
        if (blackLost) return GameStatus.WHITE_WON

        // Threefold repetition: if current position has occurred 3 or more times
        if (currentSignature != null) {
            val previousOccurrences = positionHistory.count { it == currentSignature }
            if (previousOccurrences + 1 >= 3) {
                return GameStatus.DRAW_REPETITION
            }
        }

        // Check if nextTurnPlayer has any legal regular moves
        val dummyState = GameState(board = board, currentTurn = nextTurnPlayer, rulesConfig = rulesConfig)
        val legalMoves = getAllLegalMoves(dummyState)
        if (legalMoves.isEmpty()) {
            // Player has pieces on board, but 0 legal moves: Stalemate (Draw)
            return GameStatus.DRAW_STALEMATE
        }

        return GameStatus.PLAYING
    }
}

package com.example.engine

import com.example.model.AiDifficulty
import com.example.model.GameStatus
import com.example.model.Move
import com.example.model.PlayerSide
import com.example.model.Position
import com.example.model.Superpower
import kotlin.random.Random

sealed class AiDecision {
    data class RegularMove(val move: Move) : AiDecision()
    data class ActivateSuperpower(val superpower: Superpower, val followUpMove: Move? = null) : AiDecision()
    data class QueenTransform(val pos: Position) : AiDecision()
    data class RevivePawn(val pos: Position) : AiDecision()
}

object ChessAi {

    fun computeNextMove(state: GameState, difficulty: AiDifficulty): AiDecision? {
        val aiPlayer = state.currentTurn

        // If in a multi-jump combo, continue jumping
        if (state.chainCapturePos != null) {
            val comboMoves = GameEngine.getLegalCapturesForPosition(state, state.chainCapturePos)
            if (comboMoves.isNotEmpty()) {
                return AiDecision.RegularMove(comboMoves.maxByOrNull { it.to.diagonalRank } ?: comboMoves.random())
            }
        }

        val remainingPowers = state.remainingPowers(aiPlayer)

        when (difficulty) {
            AiDifficulty.NOVICE -> {
                // 1. Check if can use power for fun (20% chance if power available)
                if (remainingPowers.isNotEmpty() && Random.nextFloat() < 0.20f && state.activePower == null) {
                    val powerDecision = evaluateCasualPower(state, aiPlayer, remainingPowers)
                    if (powerDecision != null) return powerDecision
                }

                // 2. Play a random legal move with strong priority on captures
                val legalMoves = GameEngine.getAllLegalMoves(state)
                if (legalMoves.isEmpty()) return null

                val captureMoves = legalMoves.filter { it.capturedPiece != null }
                return if (captureMoves.isNotEmpty() && Random.nextFloat() < 0.8f) {
                    AiDecision.RegularMove(captureMoves.random())
                } else {
                    AiDecision.RegularMove(legalMoves.random())
                }
            }

            AiDifficulty.TACTICIAN -> {
                return findBestStrategicAction(state, aiPlayer, depth = 2, maxPowersToTest = 2)
            }

            AiDifficulty.GRANDMASTER -> {
                return findBestStrategicAction(state, aiPlayer, depth = 3, maxPowersToTest = 4)
            }
        }
    }

    private fun evaluateCasualPower(state: GameState, aiPlayer: PlayerSide, powers: Set<Superpower>): AiDecision? {
        if (powers.contains(Superpower.PAWN) && state.graveyard(aiPlayer).isNotEmpty()) {
            val reviveSpots = GameEngine.getReviveDestinations(state, aiPlayer)
            if (reviveSpots.isNotEmpty()) {
                return AiDecision.RevivePawn(reviveSpots.first())
            }
        }

        if (powers.contains(Superpower.QUEEN)) {
            val ownNonQueens = state.board.filter { it.value.player == aiPlayer && !it.value.isQueen }.keys.toList()
            if (ownNonQueens.isNotEmpty() && (state.board.size <= 8 || Random.nextBoolean())) {
                return AiDecision.QueenTransform(ownNonQueens.random())
            }
        }

        if (powers.contains(Superpower.ROOK)) {
            val rookState = state.copy(activePower = Superpower.ROOK)
            val rookMoves = GameEngine.getAllLegalMoves(rookState)
            val captures = rookMoves.filter { it.capturedPiece != null }
            if (captures.isNotEmpty()) {
                return AiDecision.ActivateSuperpower(Superpower.ROOK, captures.random())
            }
        }

        if (powers.contains(Superpower.BISHOP)) {
            val emptySquares = (0..6).flatMap { r -> (0..6).map { c -> Position(r, c) } }
                .filter { !state.board.containsKey(it) }
            val ownPieces = state.board.filter { it.value.player == aiPlayer }.keys.toList()
            if (emptySquares.isNotEmpty() && ownPieces.isNotEmpty()) {
                val from = ownPieces.random()
                val to = emptySquares.random()
                val piece = state.board[from]!!
                return AiDecision.ActivateSuperpower(
                    Superpower.BISHOP,
                    Move(from = from, to = to, piece = piece, superpowerUsed = Superpower.BISHOP, isTeleport = true)
                )
            }
        }

        if (powers.contains(Superpower.KING)) {
            val kingState = state.copy(activePower = Superpower.KING)
            val moves = GameEngine.getAllLegalMoves(kingState)
            if (moves.isNotEmpty()) {
                return AiDecision.ActivateSuperpower(Superpower.KING, moves.random())
            }
        }

        return null
    }

    private fun resolveMultiJumps(initialState: GameState): GameState {
        var cur = initialState
        var steps = 0
        while (cur.chainCapturePos != null && steps < 8) {
            val nextCaptures = GameEngine.getLegalCapturesForPosition(cur, cur.chainCapturePos)
            if (nextCaptures.isNotEmpty()) {
                cur = GameEngine.applyMove(cur, nextCaptures.first())
            } else {
                cur = GameEngine.finishMultiJump(cur)
                break
            }
            steps++
        }
        return cur
    }

    private fun findBestStrategicAction(
        state: GameState,
        aiPlayer: PlayerSide,
        depth: Int,
        maxPowersToTest: Int
    ): AiDecision? {
        var bestScore = Int.MIN_VALUE
        var bestDecision: AiDecision? = null

        // 1. Evaluate normal legal moves (captures prioritized)
        val normalMoves = GameEngine.getAllLegalMoves(state)
        val sortedMoves = normalMoves.sortedByDescending { it.capturedPiece != null }

        for (move in sortedMoves) {
            val afterMove = GameEngine.applyMove(state, move)
            val resolvedState = resolveMultiJumps(afterMove)
            val score = minimax(resolvedState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
            if (score > bestScore || bestDecision == null) {
                bestScore = score
                bestDecision = AiDecision.RegularMove(move)
            }
        }

        // 2. Evaluate Superpowers if state is not currently in an active power or multi-jump
        if (state.activePower == null && state.kingMoveCount == 0 && state.chainCapturePos == null) {
            val availablePowers = state.remainingPowers(aiPlayer)

            // Evaluate PAWN Revive
            if (availablePowers.contains(Superpower.PAWN) && state.graveyard(aiPlayer).isNotEmpty()) {
                val reviveSpots = GameEngine.getReviveDestinations(state, aiPlayer)
                for (spot in reviveSpots.take(3)) {
                    val nextState = GameEngine.applyPawnRevival(state, spot)
                    val score = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
                    if (score > bestScore + 40) {
                        bestScore = score
                        bestDecision = AiDecision.RevivePawn(spot)
                    }
                }
            }

            // Evaluate QUEEN Transformation
            if (availablePowers.contains(Superpower.QUEEN)) {
                val ownRegularPieces = state.board.filter { it.value.player == aiPlayer && !it.value.isQueen }.keys
                for (pos in ownRegularPieces.take(4)) {
                    val nextState = GameEngine.applyQueenTransformation(state, pos)
                    val score = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
                    if (score > bestScore + 80) {
                        bestScore = score
                        bestDecision = AiDecision.QueenTransform(pos)
                    }
                }
            }

            // Evaluate ROOK Superpower (Infinite Range + Multi-Eating from infinite range)
            if (availablePowers.contains(Superpower.ROOK)) {
                val rookState = state.copy(activePower = Superpower.ROOK)
                val rookMoves = GameEngine.getAllLegalMoves(rookState)
                val rookCaptures = rookMoves.filter { it.capturedPiece != null }
                val candidates = if (rookCaptures.isNotEmpty()) rookCaptures else rookMoves.take(4)

                for (rm in candidates) {
                    val afterMove = GameEngine.applyMove(rookState, rm)
                    val resolvedState = resolveMultiJumps(afterMove)
                    val score = minimax(resolvedState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
                    if (score > bestScore + 50) {
                        bestScore = score
                        bestDecision = AiDecision.ActivateSuperpower(Superpower.ROOK, rm)
                    }
                }
            }

            // Evaluate KING Double Move
            if (availablePowers.contains(Superpower.KING)) {
                val kingState = state.copy(activePower = Superpower.KING)
                val kingFirstMoves = GameEngine.getAllLegalMoves(kingState)
                for (m1 in kingFirstMoves.take(4)) {
                    val afterM1 = resolveMultiJumps(GameEngine.applyMove(kingState, m1))
                    val kingSecondMoves = GameEngine.getAllLegalMoves(afterM1)
                    for (m2 in kingSecondMoves.take(3)) {
                        val afterM2 = resolveMultiJumps(GameEngine.applyMove(afterM1, m2))
                        val score = minimax(afterM2, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
                        if (score > bestScore + 60) {
                            bestScore = score
                            bestDecision = AiDecision.ActivateSuperpower(Superpower.KING, m1)
                        }
                    }
                }
            }

            // Evaluate BISHOP Teleport
            if (availablePowers.contains(Superpower.BISHOP)) {
                val bishopState = state.copy(isBishopTeleportMode = true)
                val ownPieces = state.board.filter { it.value.player == aiPlayer }.keys
                val emptySquares = (0..6).flatMap { r -> (0..6).map { c -> Position(r, c) } }
                    .filter { !state.board.containsKey(it) }

                for (from in ownPieces.take(2)) {
                    val piece = state.board[from]!!
                    for (to in emptySquares.take(4)) {
                        val teleportMove = Move(
                            from = from,
                            to = to,
                            piece = piece,
                            superpowerUsed = Superpower.BISHOP,
                            isTeleport = true
                        )
                        val nextState = resolveMultiJumps(GameEngine.applyMove(bishopState, teleportMove))
                        val score = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
                        if (score > bestScore + 50) {
                            bestScore = score
                            bestDecision = AiDecision.ActivateSuperpower(Superpower.BISHOP, teleportMove)
                        }
                    }
                }
            }

            // Evaluate KNIGHT Superpower
            if (availablePowers.contains(Superpower.KNIGHT)) {
                val knightState = state.copy(activePower = Superpower.KNIGHT)
                val knightMoves = GameEngine.getAllLegalMoves(knightState)
                val knightCaptures = knightMoves.filter { it.capturedPiece != null }
                val candidates = if (knightCaptures.isNotEmpty()) knightCaptures else knightMoves.take(3)

                for (km in candidates) {
                    val nextState = resolveMultiJumps(GameEngine.applyMove(knightState, km))
                    val score = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, isMaximizing = false, aiPlayer)
                    if (score > bestScore + 40) {
                        bestScore = score
                        bestDecision = AiDecision.ActivateSuperpower(Superpower.KNIGHT, km)
                    }
                }
            }
        }

        return bestDecision ?: (sortedMoves.firstOrNull()?.let { AiDecision.RegularMove(it) })
    }

    private fun minimax(
        state: GameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        aiPlayer: PlayerSide
    ): Int {
        if (depth <= 0 || state.isGameOver()) {
            return evaluateState(state, aiPlayer)
        }

        var curAlpha = alpha
        var curBeta = beta

        val moves = GameEngine.getAllLegalMoves(state)
        if (moves.isEmpty()) {
            return if (state.currentTurn == aiPlayer) -10000 else 10000
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves.take(8)) {
                val nextState = resolveMultiJumps(GameEngine.applyMove(state, move))
                val eval = minimax(nextState, depth - 1, curAlpha, curBeta, false, aiPlayer)
                maxEval = maxOf(maxEval, eval)
                curAlpha = maxOf(curAlpha, eval)
                if (curBeta <= curAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves.take(8)) {
                val nextState = resolveMultiJumps(GameEngine.applyMove(state, move))
                val eval = minimax(nextState, depth - 1, curAlpha, curBeta, true, aiPlayer)
                minEval = minOf(minEval, eval)
                curBeta = minOf(curBeta, eval)
                if (curBeta <= curAlpha) break
            }
            return minEval
        }
    }

    private fun evaluateState(state: GameState, aiPlayer: PlayerSide): Int {
        if (state.status == GameStatus.WHITE_WON) {
            return if (aiPlayer == PlayerSide.WHITE) 10000 else -10000
        }
        if (state.status == GameStatus.BLACK_WON) {
            return if (aiPlayer == PlayerSide.BLACK) 10000 else -10000
        }

        var score = 0

        for ((pos, piece) in state.board) {
            val isAi = piece.player == aiPlayer
            val sign = if (isAi) 1 else -1

            // Base piece value
            var pieceValue = if (piece.isQueen) 350 else 100

            // Advancement value towards opponent border
            val advanceDistance = if (piece.player == PlayerSide.WHITE) (12 - (pos.row + pos.col)) else (pos.row + pos.col)
            pieceValue += advanceDistance * 12

            // Center control bonus (middle neutral ranks r + c in 5..7)
            if (pos.row + pos.col == 6) {
                pieceValue += 20
            } else if (pos.row + pos.col in 5..7) {
                pieceValue += 10
            }

            score += sign * pieceValue
        }

        // Remaining powers reserve value
        val aiPowersCount = state.remainingPowers(aiPlayer).size
        val enemyPowersCount = state.remainingPowers(aiPlayer.opponent()).size
        score += (aiPowersCount - enemyPowersCount) * 25

        return score
    }
}

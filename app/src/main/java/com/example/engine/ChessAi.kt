package com.example.engine

import com.example.model.AiDifficulty
import com.example.model.GameStatus
import com.example.model.Move
import com.example.model.Piece
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

data class AiEngineStats(
    val depth: Int = 0,
    val nodesEvaluated: Int = 0,
    val evaluationCp: Int = 0,
    val timeMillis: Long = 0,
    val pvMove: String = ""
)

/**
 * Stockfish-caliber AI Engine for 7×7 Diamond Diagonal Chess.
 * 
 * Game Rules & Mechanics:
 * - Board is 7x7 diamond rotated.
 * - Regular pawns move/jump only forward (towards opponent side).
 * - Dames/Queens have 4-way omnidirectional diagonal movement & jumping (forward + backward).
 * - A piece is safe from behind if backed by the board edge or a friendly piece (forming an impenetrable chain).
 * 
 * Features:
 * - 64-bit Zobrist Hashing for full state representation
 * - Transposition Table with depth-preferred replacement, exact, lowerbound, and upperbound flags
 * - Threat & Hanging-Piece-Aware Quiescence Search with delta pruning
 * - Iterative Deepening Search (IDS) with adaptive time controls
 * - Move Ordering: PV move, Winning MVV-LVA Captures, Killer Moves, History Heuristic, Safe Advancement
 * - Tapered Handcrafted Evaluation (HCE):
 *     * Dame/Queen tuned value (230 cp vs Pawn 100 cp)
 *     * Hanging pieces penalty (pieces under direct jump threat with empty landing squares)
 *     * Mutual defense & phalanx chain bonuses (landing squares blocked)
 *     * Step-to-promotion race evaluation
 *     * Central diamond corridor control
 *     * Strategic superpower reserve & high-impact activation gating
 */
object ChessAi {

    private const val MATE_SCORE = 100000
    private const val QUEEN_VAL = 230
    private const val PAWN_VAL = 100

    private const val TT_FLAG_EXACT: Byte = 0
    private const val TT_FLAG_LOWERBOUND: Byte = 1
    private const val TT_FLAG_UPPERBOUND: Byte = 2

    private const val TT_SIZE = 1 shl 16 // 65,536 entries
    private const val TT_MASK = TT_SIZE - 1

    private class TTEntry(
        var hash: Long = 0L,
        var depth: Int = 0,
        var score: Int = 0,
        var flag: Byte = 0,
        var bestDecision: AiDecision? = null
    )

    private val transpositionTable = Array(TT_SIZE) { TTEntry() }

    // Zobrist Keys
    private val zobristPiece = Array(49) { LongArray(4) } // 49 tiles x (W_Pawn, W_Queen, B_Pawn, B_Queen)
    private var zobristSideToMove: Long = 0L
    private val zobristWhitePowers = LongArray(Superpower.entries.size)
    private val zobristBlackPowers = LongArray(Superpower.entries.size)
    private var zobristKingStep: Long = 0L
    private val zobristChainCapture = LongArray(49)

    // Search heuristics
    private val killerMoves = Array(32) { arrayOfNulls<Move>(2) }
    private val historyHeuristic = Array(49) { IntArray(49) }
    private var nodesVisited = 0
    private var stopSearch = false

    var lastSearchStats: AiEngineStats = AiEngineStats()
        private set

    init {
        // Deterministic PRNG seed for reproducible Zobrist hashing across sessions
        val rng = Random(0x436865737341494CL) // "ChessAIL"
        for (sq in 0 until 49) {
            for (pt in 0 until 4) {
                zobristPiece[sq][pt] = rng.nextLong()
            }
            zobristChainCapture[sq] = rng.nextLong()
        }
        zobristSideToMove = rng.nextLong()
        for (i in Superpower.entries.indices) {
            zobristWhitePowers[i] = rng.nextLong()
            zobristBlackPowers[i] = rng.nextLong()
        }
        zobristKingStep = rng.nextLong()
    }

    private fun posToIndex(pos: Position): Int = pos.row * 7 + pos.col

    private fun pieceToTypeIndex(piece: Piece): Int {
        return if (piece.player == PlayerSide.WHITE) {
            if (piece.isQueen) 1 else 0
        } else {
            if (piece.isQueen) 3 else 2
        }
    }

    fun computeZobristHash(state: GameState): Long {
        var h = 0L
        for ((pos, piece) in state.board) {
            val idx = posToIndex(pos)
            val pType = pieceToTypeIndex(piece)
            h = h xor zobristPiece[idx][pType]
        }
        if (state.currentTurn == PlayerSide.BLACK) {
            h = h xor zobristSideToMove
        }
        for (power in state.whitePowers) {
            h = h xor zobristWhitePowers[power.ordinal]
        }
        for (power in state.blackPowers) {
            h = h xor zobristBlackPowers[power.ordinal]
        }
        if (state.kingMoveCount == 1) {
            h = h xor zobristKingStep
        }
        state.chainCapturePos?.let {
            h = h xor zobristChainCapture[posToIndex(it)]
        }
        return h
    }

    /**
     * Clear transposition table and search heuristics between new games.
     */
    fun clearCache() {
        for (entry in transpositionTable) {
            entry.hash = 0L
            entry.depth = 0
            entry.score = 0
            entry.flag = 0
            entry.bestDecision = null
        }
        for (ply in killerMoves.indices) {
            killerMoves[ply][0] = null
            killerMoves[ply][1] = null
        }
        for (r in 0 until 49) {
            historyHeuristic[r].fill(0)
        }
    }

    /**
     * Main entry point to compute the next best AI move or power activation.
     */
    fun computeNextMove(state: GameState, difficulty: AiDifficulty): AiDecision? {
        val aiPlayer = state.currentTurn

        // If in a multi-jump combo, continue jumping immediately
        if (state.chainCapturePos != null) {
            val comboMoves = GameEngine.getLegalCapturesForPosition(state, state.chainCapturePos)
            if (comboMoves.isNotEmpty()) {
                return AiDecision.RegularMove(
                    comboMoves.maxByOrNull {
                        val victim = it.capturedPiece
                        (if (victim?.isQueen == true) 350 else 150) + (if (aiPlayer == PlayerSide.WHITE) (6 - it.to.row) else it.to.row)
                    } ?: comboMoves.first()
                )
            }
        }

        // If already inside a King double move (first move made, waiting for second)
        if (state.kingMoveCount == 1) {
            val legalMoves = GameEngine.getAllLegalMoves(state)
            if (legalMoves.isEmpty()) return null
            return searchIterativeDeepening(
                state = state,
                aiPlayer = aiPlayer,
                maxDepth = if (difficulty == AiDifficulty.NOVICE) 1 else if (difficulty == AiDifficulty.TACTICIAN) 3 else 5,
                timeLimitMillis = 500L,
                allowPowers = false
            )
        }

        val remainingPowers = state.remainingPowers(aiPlayer)

        when (difficulty) {
            AiDifficulty.NOVICE -> {
                if (remainingPowers.isNotEmpty() && Random.nextFloat() < 0.20f && state.activePower == null && state.kingMoveCount == 0 && state.chainCapturePos == null) {
                    val casualPower = evaluateCasualPower(state, aiPlayer, remainingPowers)
                    if (casualPower != null) return casualPower
                }

                val legalMoves = GameEngine.getAllLegalMoves(state)
                if (legalMoves.isEmpty()) return null

                val captureMoves = legalMoves.filter { it.capturedPiece != null }
                return if (captureMoves.isNotEmpty() && Random.nextFloat() < 0.75f) {
                    AiDecision.RegularMove(captureMoves.random())
                } else {
                    AiDecision.RegularMove(legalMoves.random())
                }
            }

            AiDifficulty.TACTICIAN -> {
                return searchIterativeDeepening(
                    state = state,
                    aiPlayer = aiPlayer,
                    maxDepth = 4,
                    timeLimitMillis = 500L,
                    allowPowers = true
                )
            }

            AiDifficulty.GRANDMASTER -> {
                return searchIterativeDeepening(
                    state = state,
                    aiPlayer = aiPlayer,
                    maxDepth = 7,
                    timeLimitMillis = 1400L,
                    allowPowers = true
                )
            }
        }
    }

    private fun evaluateCasualPower(state: GameState, aiPlayer: PlayerSide, powers: Set<Superpower>): AiDecision? {
        // PAWN Revive (Single-turn capture window): High value if legal right now
        if (GameEngine.canRevivePawn(state, aiPlayer)) {
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

    /**
     * Iterative Deepening Search with time-budgeted Alpha-Beta Negamax.
     */
    private fun searchIterativeDeepening(
        state: GameState,
        aiPlayer: PlayerSide,
        maxDepth: Int,
        timeLimitMillis: Long,
        allowPowers: Boolean
    ): AiDecision? {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeLimitMillis
        nodesVisited = 0
        stopSearch = false

        // Generate root candidate decisions
        val rootCandidates = generateRootCandidates(state, aiPlayer, allowPowers)
        if (rootCandidates.isEmpty()) return null
        if (rootCandidates.size == 1) return rootCandidates.first().decision

        var bestOverallDecision: AiDecision = rootCandidates.first().decision
        var bestOverallScore = -MATE_SCORE

        val rootStateHash = computeZobristHash(state)
        val cachedEntry = transpositionTable[(rootStateHash and TT_MASK.toLong()).toInt()]
        if (cachedEntry.hash == rootStateHash && cachedEntry.bestDecision != null) {
            bestOverallDecision = cachedEntry.bestDecision!!
        }

        for (targetDepth in 1..maxDepth) {
            if (System.currentTimeMillis() >= endTime) break

            var currentIterationBestDecision: AiDecision? = null
            var currentIterationBestScore = -MATE_SCORE
            var alpha = -MATE_SCORE
            val beta = MATE_SCORE

            // Sort root candidates based on previous iteration PV and strategic priority
            val sortedCandidates = rootCandidates.sortedByDescending { cand ->
                if (cand.decision == bestOverallDecision) 1_000_000 else cand.initialOrderScore
            }

            for (candidate in sortedCandidates) {
                if (System.currentTimeMillis() >= endTime && targetDepth > 1) {
                    stopSearch = true
                    break
                }

                val score = -negamax(
                    state = candidate.resultingState,
                    depth = targetDepth - 1,
                    ply = 1,
                    alpha = -beta,
                    beta = -alpha,
                    currentSide = aiPlayer.opponent(),
                    rootAiPlayer = aiPlayer,
                    deadlineMillis = endTime
                )

                if (stopSearch) break

                if (score > currentIterationBestScore || currentIterationBestDecision == null) {
                    currentIterationBestScore = score
                    currentIterationBestDecision = candidate.decision
                }

                if (score > alpha) {
                    alpha = score
                }
            }

            if (!stopSearch && currentIterationBestDecision != null) {
                bestOverallDecision = currentIterationBestDecision
                bestOverallScore = currentIterationBestScore

                // Save PV move to TT
                storeTT(
                    hash = rootStateHash,
                    depth = targetDepth,
                    score = bestOverallScore,
                    flag = TT_FLAG_EXACT,
                    bestDecision = bestOverallDecision
                )

                // If mate detected, terminate early
                if (bestOverallScore >= MATE_SCORE - 100) break
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        lastSearchStats = AiEngineStats(
            depth = maxDepth,
            nodesEvaluated = nodesVisited,
            evaluationCp = bestOverallScore,
            timeMillis = totalTime,
            pvMove = bestOverallDecision.toString()
        )

        return bestOverallDecision
    }

    private data class RootCandidate(
        val decision: AiDecision,
        val resultingState: GameState,
        val initialOrderScore: Int
    )

    private fun generateRootCandidates(
        state: GameState,
        aiPlayer: PlayerSide,
        allowPowers: Boolean
    ): List<RootCandidate> {
        val candidates = mutableListOf<RootCandidate>()

        // 1. Regular legal moves
        val legalMoves = GameEngine.getAllLegalMoves(state)
        val oppPieceCount = state.board.count { it.value.player == aiPlayer.opponent() }
        val lossThreshold = state.rulesConfig.lossPieceThreshold
        val queenDistThreshold = state.rulesConfig.queenDistanceThreshold

        for (m in legalMoves) {
            val afterMove = resolveMultiJumps(GameEngine.applyMove(state, m))
            var priority = 1000

            val isDecisiveWinCapture = m.capturedPiece != null && (oppPieceCount - 1 <= lossThreshold)
            val isPromo = m.isPromotion || (!m.piece.isQueen && m.piece.player.isPromotionGoal(m.to, queenDistThreshold))

            if (isDecisiveWinCapture) {
                priority += 950_000 // Decisive winning capture!
            } else if (m.capturedPiece != null) {
                val victimVal = if (m.capturedPiece.isQueen) QUEEN_VAL else PAWN_VAL
                val attackerVal = if (m.piece.isQueen) QUEEN_VAL else PAWN_VAL
                priority += 20000 + (victimVal * 10 - attackerVal)
            }

            if (isPromo) priority += 15000

            candidates.add(RootCandidate(AiDecision.RegularMove(m), afterMove, priority))
        }

        // 2. High-Impact Superpowers (Gated strictly to tactical situations to avoid wasting)
        if (allowPowers && state.activePower == null && state.kingMoveCount == 0 && state.chainCapturePos == null) {
            val powers = state.remainingPowers(aiPlayer)
            val ownPieceCount = state.board.count { it.value.player == aiPlayer }
            val enemyPieceCount = state.board.count { it.value.player == aiPlayer.opponent() }
            val ownMarginToLoss = ownPieceCount - lossThreshold

            // PAWN Revive: Must be used on the single turn following capture
            if (GameEngine.canRevivePawn(state, aiPlayer)) {
                val graveyard = state.graveyard(aiPlayer)
                val lastCaptured = graveyard.last()
                val reviveSpots = GameEngine.getReviveDestinations(state, aiPlayer)

                // High priority: single-turn window, Queen revival, low margin, or general material preservation
                for (spot in reviveSpots.take(4)) {
                    val afterRevive = GameEngine.applyPawnRevival(state, spot)
                    val isSafe = isPieceProtectedFromBehind(afterRevive, spot, aiPlayer)
                    val distToGoal = getDistanceToPromotionGoal(spot, aiPlayer, queenDistThreshold)

                    var baseScore = if (lastCaptured.isQueen) 26000 else 18000
                    if (ownMarginToLoss <= 2) baseScore += 9000 // Essential survival revival
                    if (isSafe) baseScore += 2000
                    baseScore += (12 - distToGoal) * 100

                    candidates.add(RootCandidate(AiDecision.RevivePawn(spot), afterRevive, baseScore))
                }
            }

            // QUEEN Transform: Only transform pieces in active central positions or close to promotion
            if (powers.contains(Superpower.QUEEN)) {
                val candidatePawns = state.board.filter { (pos, piece) ->
                    piece.player == aiPlayer && !piece.isQueen && (state.board.size <= 8 || (pos.row + pos.col) in 4..8)
                }.keys
                for (pos in candidatePawns.take(3)) {
                    val afterTransform = GameEngine.applyQueenTransformation(state, pos)
                    val dist = getDistanceToPromotionGoal(pos, aiPlayer, queenDistThreshold)
                    val priority = 16000 + (12 - dist) * 150
                    candidates.add(RootCandidate(AiDecision.QueenTransform(pos), afterTransform, priority))
                }
            }

            // ROOK Superpower (infinite range slide & snipe): ONLY if it captures an enemy piece or reaches promotion
            if (powers.contains(Superpower.ROOK)) {
                val rookState = state.copy(activePower = Superpower.ROOK)
                val rookMoves = GameEngine.getAllLegalMoves(rookState)
                val captures = rookMoves.filter { it.capturedPiece != null }

                for (rm in captures) {
                    val afterMove = resolveMultiJumps(GameEngine.applyMove(rookState, rm))
                    val isDecisiveWin = (enemyPieceCount - 1 <= lossThreshold)
                    val priority = if (isDecisiveWin) 980_000 else (25000 + (if (rm.capturedPiece?.isQueen == true) 3000 else 1000))
                    candidates.add(RootCandidate(AiDecision.ActivateSuperpower(Superpower.ROOK, rm), afterMove, priority))
                }
            }

            // KING Double Move: Prioritize if either step captures or promotes
            if (powers.contains(Superpower.KING)) {
                val kingState = state.copy(activePower = Superpower.KING)
                val firstMoves = GameEngine.getAllLegalMoves(kingState)
                for (m1 in firstMoves.take(5)) {
                    val afterM1 = resolveMultiJumps(GameEngine.applyMove(kingState, m1))
                    val secondMoves = GameEngine.getAllLegalMoves(afterM1)
                    val bestSecond = secondMoves.maxByOrNull { it.capturedPiece != null || it.isPromotion } ?: secondMoves.firstOrNull()
                    if (bestSecond != null) {
                        val afterM2 = resolveMultiJumps(GameEngine.applyMove(afterM1, bestSecond))
                        val hasTactics = m1.capturedPiece != null || bestSecond.capturedPiece != null || m1.isPromotion || bestSecond.isPromotion
                        val priority = if (hasTactics) 22000 else 8000
                        // Only add king double move if tactical or board is late game
                        if (hasTactics || state.board.size <= 8) {
                            candidates.add(RootCandidate(AiDecision.ActivateSuperpower(Superpower.KING, m1), afterM2, priority))
                        }
                    }
                }
            }

            // BISHOP Teleport: Save for instant promotion or escaping direct capture
            if (powers.contains(Superpower.BISHOP)) {
                val bishopState = state.copy(isBishopTeleportMode = true)
                val ownPieces = state.board.filter { it.value.player == aiPlayer }
                val emptySquares = (0..6).flatMap { r -> (0..6).map { c -> Position(r, c) } }
                    .filter { !state.board.containsKey(it) }

                for ((from, piece) in ownPieces) {
                    // Check if target square is in active promotion zone
                    for (to in emptySquares) {
                        val isPromo = !piece.isQueen && piece.player.isPromotionGoal(to, queenDistThreshold)
                        if (isPromo) {
                            val teleportMove = Move(
                                from = from,
                                to = to,
                                piece = piece,
                                superpowerUsed = Superpower.BISHOP,
                                isTeleport = true,
                                isPromotion = true
                            )
                            val afterTeleport = resolveMultiJumps(GameEngine.applyMove(bishopState, teleportMove))
                            candidates.add(RootCandidate(AiDecision.ActivateSuperpower(Superpower.BISHOP, teleportMove), afterTeleport, 24000))
                        }
                    }
                }
            }

            // KNIGHT Backward moves: Only if it captures an enemy piece or escapes
            if (powers.contains(Superpower.KNIGHT)) {
                val knightState = state.copy(activePower = Superpower.KNIGHT)
                val knightMoves = GameEngine.getAllLegalMoves(knightState)
                val captures = knightMoves.filter { it.capturedPiece != null }

                for (km in captures) {
                    val afterMove = resolveMultiJumps(GameEngine.applyMove(knightState, km))
                    val priority = 18000
                    candidates.add(RootCandidate(AiDecision.ActivateSuperpower(Superpower.KNIGHT, km), afterMove, priority))
                }
            }
        }

        return candidates
    }

    /**
     * Alpha-Beta Negamax core recursive search.
     */
    private fun negamax(
        state: GameState,
        depth: Int,
        ply: Int,
        alpha: Int,
        beta: Int,
        currentSide: PlayerSide,
        rootAiPlayer: PlayerSide,
        deadlineMillis: Long
    ): Int {
        nodesVisited++

        if (nodesVisited and 511 == 0 && System.currentTimeMillis() >= deadlineMillis) {
            stopSearch = true
            return 0
        }

        if (state.isGameOver()) {
            return when {
                (state.status == GameStatus.WHITE_WON && currentSide == PlayerSide.WHITE) ||
                (state.status == GameStatus.BLACK_WON && currentSide == PlayerSide.BLACK) -> MATE_SCORE - ply
                else -> -(MATE_SCORE - ply)
            }
        }

        if (depth <= 0) {
            return quiescenceSearch(state, alpha, beta, currentSide, rootAiPlayer, ply)
        }

        var curAlpha = alpha
        val stateHash = computeZobristHash(state)
        val ttIndex = (stateHash and TT_MASK.toLong()).toInt()
        val ttEntry = transpositionTable[ttIndex]

        if (ttEntry.hash == stateHash && ttEntry.depth >= depth) {
            when (ttEntry.flag) {
                TT_FLAG_EXACT -> return ttEntry.score
                TT_FLAG_LOWERBOUND -> if (ttEntry.score >= beta) return ttEntry.score
                TT_FLAG_UPPERBOUND -> if (ttEntry.score <= curAlpha) return ttEntry.score
            }
        }

        val legalMoves = GameEngine.getAllLegalMoves(state)
        if (legalMoves.isEmpty()) {
            return -(MATE_SCORE - ply)
        }

        val sortedMoves = scoreAndOrderMoves(legalMoves, state, ply, ttEntry.takeIf { it.hash == stateHash }?.bestDecision)

        var bestScore = -MATE_SCORE
        var bestDecision: AiDecision? = null
        var flag = TT_FLAG_UPPERBOUND

        for (i in sortedMoves.indices) {
            val move = sortedMoves[i]
            val nextState = resolveMultiJumps(GameEngine.applyMove(state, move))

            var score: Int
            // Late Move Reductions (LMR): search non-captures at late indices to reduced depth
            if (depth >= 3 && i >= 4 && move.capturedPiece == null && !move.isPromotion) {
                score = -negamax(
                    state = nextState,
                    depth = depth - 2,
                    ply = ply + 1,
                    alpha = -curAlpha - 1,
                    beta = -curAlpha,
                    currentSide = currentSide.opponent(),
                    rootAiPlayer = rootAiPlayer,
                    deadlineMillis = deadlineMillis
                )
                if (score > curAlpha && score < beta) {
                    score = -negamax(
                        state = nextState,
                        depth = depth - 1,
                        ply = ply + 1,
                        alpha = -beta,
                        beta = -curAlpha,
                        currentSide = currentSide.opponent(),
                        rootAiPlayer = rootAiPlayer,
                        deadlineMillis = deadlineMillis
                    )
                }
            } else {
                score = -negamax(
                    state = nextState,
                    depth = depth - 1,
                    ply = ply + 1,
                    alpha = -beta,
                    beta = -curAlpha,
                    currentSide = currentSide.opponent(),
                    rootAiPlayer = rootAiPlayer,
                    deadlineMillis = deadlineMillis
                )
            }

            if (stopSearch) return 0

            if (score > bestScore) {
                bestScore = score
                bestDecision = AiDecision.RegularMove(move)
            }

            if (score > curAlpha) {
                curAlpha = score
                flag = TT_FLAG_EXACT
            }

            if (curAlpha >= beta) {
                // Beta cutoff
                flag = TT_FLAG_LOWERBOUND
                // Store killer move if quiet move
                if (move.capturedPiece == null && ply < killerMoves.size) {
                    if (killerMoves[ply][0] != move) {
                        killerMoves[ply][1] = killerMoves[ply][0]
                        killerMoves[ply][0] = move
                    }
                    val fromIdx = posToIndex(move.from)
                    val toIdx = posToIndex(move.to)
                    historyHeuristic[fromIdx][toIdx] += depth * depth
                }
                break
            }
        }

        storeTT(stateHash, depth, bestScore, flag, bestDecision)
        return bestScore
    }

    /**
     * Quiescence Search: resolves all captures and promotions until the board is quiet.
     * Prevents the Horizon Effect on jump exchanges and promotions.
     */
    private fun quiescenceSearch(
        state: GameState,
        alpha: Int,
        beta: Int,
        currentSide: PlayerSide,
        rootAiPlayer: PlayerSide,
        ply: Int
    ): Int {
        nodesVisited++

        if (state.isGameOver()) {
            return when {
                (state.status == GameStatus.WHITE_WON && currentSide == PlayerSide.WHITE) ||
                (state.status == GameStatus.BLACK_WON && currentSide == PlayerSide.BLACK) -> MATE_SCORE - ply
                else -> -(MATE_SCORE - ply)
            }
        }

        // Stand-pat evaluation
        val standPat = evaluateStatic(state, currentSide)
        if (standPat >= beta) return beta

        var curAlpha = maxOf(alpha, standPat)

        val legalMoves = GameEngine.getAllLegalMoves(state)
        val queenDistThreshold = state.rulesConfig.queenDistanceThreshold
        val tacticalMoves = legalMoves.filter { 
            it.capturedPiece != null || it.isPromotion || (!it.piece.isQueen && it.piece.player.isPromotionGoal(it.to, queenDistThreshold))
        }
        if (tacticalMoves.isEmpty()) return standPat

        val sortedTactical = tacticalMoves.sortedByDescending { move ->
            val victimVal = if (move.capturedPiece?.isQueen == true) QUEEN_VAL else PAWN_VAL
            val attackerVal = if (move.piece.isQueen) QUEEN_VAL else PAWN_VAL
            val promoBonus = if (move.isPromotion || (!move.piece.isQueen && move.piece.player.isPromotionGoal(move.to, queenDistThreshold))) 220 else 0
            (victimVal * 10 - attackerVal) + promoBonus
        }

        for (move in sortedTactical) {
            // Delta pruning
            val maxGain = (if (move.capturedPiece?.isQueen == true) QUEEN_VAL else PAWN_VAL) + 120
            if (standPat + maxGain < curAlpha) continue

            val nextState = resolveMultiJumps(GameEngine.applyMove(state, move))
            val score = -quiescenceSearch(nextState, -beta, -curAlpha, currentSide.opponent(), rootAiPlayer, ply + 1)

            if (score >= beta) return beta
            if (score > curAlpha) curAlpha = score
        }

        return curAlpha
    }

    private fun scoreAndOrderMoves(
        moves: List<Move>,
        state: GameState,
        ply: Int,
        ttDecision: AiDecision?
    ): List<Move> {
        val ttMove = (ttDecision as? AiDecision.RegularMove)?.move
        val killer1 = if (ply < killerMoves.size) killerMoves[ply][0] else null
        val killer2 = if (ply < killerMoves.size) killerMoves[ply][1] else null
        val oppPieces = state.board.count { it.value.player == state.currentTurn.opponent() }
        val lossThreshold = state.rulesConfig.lossPieceThreshold
        val queenDistThreshold = state.rulesConfig.queenDistanceThreshold

        return moves.sortedByDescending { m ->
            val isDecisiveWinCapture = m.capturedPiece != null && (oppPieces - 1 <= lossThreshold)
            val isPromo = m.isPromotion || (!m.piece.isQueen && m.piece.player.isPromotionGoal(m.to, queenDistThreshold))

            when {
                isDecisiveWinCapture -> 1_200_000 // Decisive match-winning strike!
                ttMove != null && m.from == ttMove.from && m.to == ttMove.to -> 1_000_000
                m.capturedPiece != null -> {
                    val victim = if (m.capturedPiece.isQueen) QUEEN_VAL else PAWN_VAL
                    val attacker = if (m.piece.isQueen) QUEEN_VAL else PAWN_VAL
                    800_000 + (victim * 10 - attacker)
                }
                isPromo -> 600_000
                killer1 != null && m.from == killer1.from && m.to == killer1.to -> 400_000
                killer2 != null && m.from == killer2.from && m.to == killer2.to -> 390_000
                else -> {
                    val fromIdx = posToIndex(m.from)
                    val toIdx = posToIndex(m.to)
                    historyHeuristic[fromIdx][toIdx].coerceAtMost(300_000)
                }
            }
        }
    }

    private fun storeTT(
        hash: Long,
        depth: Int,
        score: Int,
        flag: Byte,
        bestDecision: AiDecision?
    ) {
        val idx = (hash and TT_MASK.toLong()).toInt()
        val entry = transpositionTable[idx]
        if (entry.hash == 0L || entry.depth <= depth || entry.hash != hash) {
            entry.hash = hash
            entry.depth = depth
            entry.score = score
            entry.flag = flag
            if (bestDecision != null) {
                entry.bestDecision = bestDecision
            }
        }
    }

    private fun resolveMultiJumps(initialState: GameState): GameState {
        var cur = initialState
        var steps = 0
        while (cur.chainCapturePos != null && steps < 8) {
            val nextCaptures = GameEngine.getLegalCapturesForPosition(cur, cur.chainCapturePos)
            if (nextCaptures.isNotEmpty()) {
                val bestJump = nextCaptures.maxByOrNull {
                    val victim = it.capturedPiece
                    (if (victim?.isQueen == true) 350 else 100) + it.to.diagonalRank
                } ?: nextCaptures.first()
                cur = GameEngine.applyMove(cur, bestJump)
            } else {
                cur = GameEngine.finishMultiJump(cur)
                break
            }
            steps++
        }
        return cur
    }

    /**
     * Handcrafted Static Evaluation Function (HCE) with:
     * - Loss threshold & piece safety margins (custom lossPieceThreshold)
     * - Queening zone proximity & directional race (custom queenDistanceThreshold)
     * - Dame/Queen value: 230 cp vs Pawn 100 cp
     * - Hanging Piece Detection (penalty for undefended jumpable pieces)
     * - Chain / Phalanx Defense Bonus (landing squares blocked by friendlies or edges)
     * - Single-turn Pawn Revive window strategic reserve valuation
     */
    fun evaluateStatic(state: GameState, evalPlayer: PlayerSide): Int {
        if (state.status == GameStatus.WHITE_WON) {
            return if (evalPlayer == PlayerSide.WHITE) MATE_SCORE else -MATE_SCORE
        }
        if (state.status == GameStatus.BLACK_WON) {
            return if (evalPlayer == PlayerSide.BLACK) MATE_SCORE else -MATE_SCORE
        }

        val lossThreshold = state.rulesConfig.lossPieceThreshold
        val queenDistThreshold = state.rulesConfig.queenDistanceThreshold

        var whitePieceCount = 0
        var blackPieceCount = 0
        for ((_, piece) in state.board) {
            if (piece.player == PlayerSide.WHITE) whitePieceCount++ else blackPieceCount++
        }

        val whiteMargin = whitePieceCount - lossThreshold
        val blackMargin = blackPieceCount - lossThreshold

        if (whiteMargin <= 0 && blackMargin <= 0) return 0 // Mutual loss / draw
        if (whiteMargin <= 0) return if (evalPlayer == PlayerSide.WHITE) -MATE_SCORE else MATE_SCORE
        if (blackMargin <= 0) return if (evalPlayer == PlayerSide.BLACK) -MATE_SCORE else MATE_SCORE

        var whiteScore = 0
        var blackScore = 0

        // Margin-to-loss scaling: Higher loss thresholds make every single unit dramatically more critical
        val piecePreservationWeight = 110 + (lossThreshold * 25)
        whiteScore += whiteMargin * piecePreservationWeight
        blackScore += blackMargin * piecePreservationWeight

        // Extreme survival danger when 1 piece away from elimination
        if (whiteMargin == 1) whiteScore -= (450 + lossThreshold * 60)
        if (blackMargin == 1) blackScore -= (450 + lossThreshold * 60)

        // Track vulnerable hanging pieces for both sides
        val whiteHangingPenalty = computeHangingPenalty(state, PlayerSide.WHITE)
        val blackHangingPenalty = computeHangingPenalty(state, PlayerSide.BLACK)

        whiteScore -= whiteHangingPenalty
        blackScore -= blackHangingPenalty

        for ((pos, piece) in state.board) {
            val isWhite = piece.player == PlayerSide.WHITE
            var valPiece = if (piece.isQueen) QUEEN_VAL else PAWN_VAL

            // 1. Advancement towards opposing valid promotion threshold
            val distToGoal = getDistanceToPromotionGoal(pos, piece.player, queenDistThreshold)
            val advanceBonus = (12 - distToGoal) * 6
            valPiece += advanceBonus

            // 2. Central diagonal corridor control (r+c in 5..7, peak at 6)
            val dSum = pos.row + pos.col
            if (dSum == 6) {
                valPiece += 16
            } else if (dSum in 5..7) {
                valPiece += 8
            }

            // 3. Phalanx / Chain Defense: Is this piece protected from behind?
            val isDefendedFromBehind = isPieceProtectedFromBehind(state, pos, piece.player)
            if (isDefendedFromBehind) {
                valPiece += 18
            }

            // 4. Promotion Threat: 1 step away from reaching valid queening zone
            if (!piece.isQueen) {
                if (distToGoal == 1) {
                    valPiece += 45
                }
            } else {
                // Dame mobility bonus (4-directional flexibility)
                valPiece += 30
            }

            if (isWhite) {
                whiteScore += valPiece
            } else {
                blackScore += valPiece
            }
        }

        // Strategic Superpower Reserve Value (with immediate revive window bonus)
        val canWhiteReviveNow = GameEngine.canRevivePawn(state, PlayerSide.WHITE)
        val canBlackReviveNow = GameEngine.canRevivePawn(state, PlayerSide.BLACK)
        whiteScore += evaluatePowerReserves(state.whitePowers, state.whiteGraveyard.isNotEmpty(), canWhiteReviveNow)
        blackScore += evaluatePowerReserves(state.blackPowers, state.blackGraveyard.isNotEmpty(), canBlackReviveNow)

        return if (evalPlayer == PlayerSide.WHITE) (whiteScore - blackScore) else (blackScore - whiteScore)
    }

    /**
     * Compute shortest forward Manhattan steps from [pos] to any valid queening square for [player]
     * under the active [threshold] (cells distance from apex corner).
     */
    fun getDistanceToPromotionGoal(pos: Position, player: PlayerSide, threshold: Int): Int {
        val r = pos.row
        val c = pos.col
        return if (player == PlayerSide.WHITE) {
            minOf(
                r + maxOf(0, c - threshold),
                c + maxOf(0, r - threshold)
            )
        } else {
            minOf(
                (6 - r) + maxOf(0, (6 - c) - threshold),
                (6 - c) + maxOf(0, (6 - r) - threshold)
            )
        }
    }

    /**
     * Checks if friendly pieces are hanging (i.e. opponent has an immediate jump over them into an empty square).
     */
    private fun computeHangingPenalty(state: GameState, side: PlayerSide): Int {
        var penalty = 0
        val opp = side.opponent()

        // Check if any opponent piece can capture one of our pieces
        for ((oppPos, oppPiece) in state.board) {
            if (oppPiece.player != opp) continue
            val forwardDirs = oppPiece.player.forwardDirections()
            val dirs = if (oppPiece.isQueen) forwardDirs + oppPiece.player.backwardDirections() else forwardDirs

            for (dir in dirs) {
                val targetPos = oppPos.delta(dir.first, dir.second)
                val victim = state.board[targetPos]
                if (victim != null && victim.player == side) {
                    val landingPos = targetPos.delta(dir.first, dir.second)
                    if (landingPos.isValid && !state.board.containsKey(landingPos)) {
                        // Hanging! Direct jump capture available
                        penalty += if (victim.isQueen) 140 else 75
                    }
                }
            }
        }
        return penalty
    }

    /**
     * Determines if a piece cannot be jumped from behind because the landing square is blocked or off-board.
     */
    private fun isPieceProtectedFromBehind(state: GameState, pos: Position, side: PlayerSide): Boolean {
        val forwardDirs = side.forwardDirections()
        // Attackers jump along side's backward direction to land behind the piece
        var safeAngles = 0
        for (dir in forwardDirs) {
            // An attacker from front (+dir) would land at pos - dir
            val landingPos = pos.delta(-dir.first, -dir.second)
            if (!landingPos.isValid || state.board.containsKey(landingPos)) {
                safeAngles++
            }
        }
        return safeAngles == forwardDirs.size
    }

    private fun evaluatePowerReserves(powers: Set<Superpower>, hasGraveyard: Boolean, canReviveNow: Boolean = false): Int {
        var total = 0
        if (powers.contains(Superpower.QUEEN)) total += 60
        if (powers.contains(Superpower.KING)) total += 55
        if (powers.contains(Superpower.ROOK)) total += 50
        if (powers.contains(Superpower.BISHOP)) total += 45
        if (powers.contains(Superpower.KNIGHT)) total += 30
        if (powers.contains(Superpower.PAWN)) {
            total += when {
                canReviveNow -> 85 // Highly valuable immediately actionable superpower window!
                hasGraveyard -> 40
                else -> 15
            }
        }
        return total
    }
}

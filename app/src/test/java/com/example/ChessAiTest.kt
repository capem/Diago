package com.example

import com.example.engine.AiDecision
import com.example.engine.ChessAi
import com.example.engine.GameEngine
import com.example.engine.GameState
import com.example.model.AiDifficulty
import com.example.model.GameStatus
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position
import com.example.model.Superpower
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChessAiTest {

    @Before
    fun setup() {
        ChessAi.clearCache()
    }

    @Test
    fun testZobristHashUniqueness() {
        val state1 = GameState()
        val hash1 = ChessAi.computeZobristHash(state1)

        val state2 = GameState(currentTurn = PlayerSide.BLACK)
        val hash2 = ChessAi.computeZobristHash(state2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testAiFindsImmediateWinningCapture() {
        // Black to move: Black piece at (2, 2), White Queen at (3, 2), landing square at (4, 2)
        val customBoard = mapOf(
            Position(2, 2) to Piece("b1", PlayerSide.BLACK),
            Position(3, 2) to Piece("w_queen", PlayerSide.WHITE, isQueen = true)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.BLACK)

        val decision = ChessAi.computeNextMove(state, AiDifficulty.GRANDMASTER)
        assertNotNull(decision)
        assertTrue(decision is AiDecision.RegularMove)
        val move = (decision as AiDecision.RegularMove).move
        assertEquals(Position(2, 2), move.from)
        assertEquals(Position(4, 2), move.to)
        assertEquals("w_queen", move.capturedPiece?.id)
    }

    @Test
    fun testAiPromotesWhenReachingBorder() {
        // Black piece at (5, 2), moving to (6, 2) achieves promotion
        val customBoard = mapOf(
            Position(5, 2) to Piece("b1", PlayerSide.BLACK)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.BLACK)

        val decision = ChessAi.computeNextMove(state, AiDifficulty.GRANDMASTER)
        assertNotNull(decision)
        assertTrue(decision is AiDecision.RegularMove)
        val move = (decision as AiDecision.RegularMove).move
        assertEquals(Position(6, 2), move.to)
        assertTrue(move.isPromotion)
    }

    @Test
    fun testTacticianAndGrandmasterLevelsComplete() {
        val state = GameState()
        val decisionNovice = ChessAi.computeNextMove(state, AiDifficulty.NOVICE)
        val decisionTactician = ChessAi.computeNextMove(state, AiDifficulty.TACTICIAN)
        val decisionGM = ChessAi.computeNextMove(state, AiDifficulty.GRANDMASTER)

        assertNotNull(decisionNovice)
        assertNotNull(decisionTactician)
        assertNotNull(decisionGM)
    }

    @Test
    fun testKingDoubleMoveProgression() {
        // Black activates King power
        val initialBoard = mapOf(
            Position(1, 1) to Piece("b1", PlayerSide.BLACK),
            Position(5, 5) to Piece("w1", PlayerSide.WHITE)
        )
        val stateWithKingPower = GameState(
            board = initialBoard,
            currentTurn = PlayerSide.BLACK,
            activePower = Superpower.KING,
            kingMoveCount = 0
        )

        // Step 1: Compute first move under King power
        val decision1 = ChessAi.computeNextMove(stateWithKingPower, AiDifficulty.GRANDMASTER)
        assertNotNull(decision1)
        val move1 = when (decision1) {
            is AiDecision.RegularMove -> decision1.move
            is AiDecision.ActivateSuperpower -> decision1.followUpMove!!
            else -> null
        }
        assertNotNull(move1)

        // Apply Move 1
        val stateAfterMove1 = GameEngine.applyMove(stateWithKingPower, move1!!)
        assertEquals(1, stateAfterMove1.kingMoveCount)
        assertEquals(PlayerSide.BLACK, stateAfterMove1.currentTurn)

        // Step 2: Compute second move when kingMoveCount is 1
        val decision2 = ChessAi.computeNextMove(stateAfterMove1, AiDifficulty.GRANDMASTER)
        assertNotNull(decision2)
        assertTrue(decision2 is AiDecision.RegularMove)
        val move2 = (decision2 as AiDecision.RegularMove).move

        // Apply Move 2
        val stateAfterMove2 = GameEngine.applyMove(stateAfterMove1, move2)
        assertEquals(0, stateAfterMove2.kingMoveCount)
        assertEquals(PlayerSide.WHITE, stateAfterMove2.currentTurn)
    }
}

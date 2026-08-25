package com.example

import com.example.engine.ChessAi
import com.example.engine.GameState
import com.example.model.GameStatus
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluationBarTest {

    @Test
    fun testInitialStateSymmetricEvaluation() {
        val state = GameState()
        val whiteEval = ChessAi.evaluateStatic(state, PlayerSide.WHITE)
        val blackEval = ChessAi.evaluateStatic(state, PlayerSide.BLACK)

        // Balanced starting position
        assertEquals(0, whiteEval)
        assertEquals(0, blackEval)
    }

    @Test
    fun testWhiteMaterialAdvantageEvaluation() {
        // White has an extra Queen
        val board = mapOf(
            Position(0, 0) to Piece("w_pawn", PlayerSide.WHITE),
            Position(1, 1) to Piece("w_queen", PlayerSide.WHITE, isQueen = true),
            Position(6, 6) to Piece("b_pawn", PlayerSide.BLACK)
        )
        val state = GameState(board = board)

        val eval = ChessAi.evaluateStatic(state, PlayerSide.WHITE)
        assertTrue("White should have significant positive evaluation with extra Queen", eval > 200)
    }

    @Test
    fun testBlackMaterialAdvantageEvaluation() {
        // Black has an extra Queen
        val board = mapOf(
            Position(0, 0) to Piece("w_pawn", PlayerSide.WHITE),
            Position(6, 6) to Piece("b_pawn", PlayerSide.BLACK),
            Position(5, 5) to Piece("b_queen", PlayerSide.BLACK, isQueen = true)
        )
        val state = GameState(board = board)

        val eval = ChessAi.evaluateStatic(state, PlayerSide.WHITE)
        assertTrue("White evaluation should be negative when Black is winning", eval < -200)
    }

    @Test
    fun testMateEvaluation() {
        val whiteWonState = GameState(status = GameStatus.WHITE_WON)
        val blackWonState = GameState(status = GameStatus.BLACK_WON)

        val whiteWinEval = ChessAi.evaluateStatic(whiteWonState, PlayerSide.WHITE)
        val blackWinEval = ChessAi.evaluateStatic(blackWonState, PlayerSide.WHITE)

        assertEquals(100000, whiteWinEval)
        assertEquals(-100000, blackWinEval)
    }
}

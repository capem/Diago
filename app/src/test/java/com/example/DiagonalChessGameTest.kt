package com.example

import com.example.engine.GameEngine
import com.example.engine.GameState
import com.example.model.GameStatus
import com.example.model.Move
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position
import com.example.model.Superpower
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagonalChessGameTest {

    @Test
    fun testInitialBoardSetup() {
        val state = GameState()
        assertEquals(20, state.board.size)
        val whitePieces = state.board.values.filter { it.player == PlayerSide.WHITE }
        val blackPieces = state.board.values.filter { it.player == PlayerSide.BLACK }
        assertEquals(10, whitePieces.size)
        assertEquals(10, blackPieces.size)
        assertEquals(PlayerSide.WHITE, state.currentTurn)
    }

    @Test
    fun testWhiteForwardMoves() {
        val state = GameState()
        val moves = GameEngine.getLegalMovesForPosition(state, Position(3, 6))
        assertTrue(moves.isNotEmpty())
        assertTrue(moves.any { it.to == Position(2, 6) })
    }

    @Test
    fun testNormalPieceCannotEatFromInfiniteRange() {
        // White piece at (4, 3), enemy at (2, 3) - distance 2 away with empty square at (3, 3)
        val customBoard = mapOf(
            Position(4, 3) to Piece("w1", PlayerSide.WHITE),
            Position(2, 3) to Piece("b1", PlayerSide.BLACK)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.WHITE)
        val moves = GameEngine.getLegalMovesForPosition(state, Position(4, 3))

        // Normal piece cannot jump enemy at distance 2; it can only step to (3, 3)
        assertEquals(1, moves.filter { it.to.col == 3 }.size)
        val stepMove = moves.find { it.to == Position(3, 3) }
        assertNotNull(stepMove)
        assertNull(stepMove?.capturedPiece)

        // No capture moves exist
        assertTrue(moves.none { it.capturedPiece != null })
    }

    @Test
    fun testRookInfiniteRangeSlideAndJump() {
        // White piece with Rook superpower at (5, 3), enemy at (2, 3), landing squares at (1, 3) and (0, 3)
        val customBoard = mapOf(
            Position(5, 3) to Piece("w1", PlayerSide.WHITE),
            Position(2, 3) to Piece("b1", PlayerSide.BLACK)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.WHITE, activePower = Superpower.ROOK)
        val moves = GameEngine.getLegalMovesForPosition(state, Position(5, 3))

        // Sliding moves: (4, 3), (3, 3)
        assertTrue(moves.any { it.to == Position(4, 3) && it.capturedPiece == null })
        assertTrue(moves.any { it.to == Position(3, 3) && it.capturedPiece == null })

        // Infinite range flying jump moves over enemy at (2, 3): landing at (1, 3) and (0, 3)
        val captureTo1 = moves.find { it.to == Position(1, 3) && it.capturedPiece?.id == "b1" }
        val captureTo0 = moves.find { it.to == Position(0, 3) && it.capturedPiece?.id == "b1" }
        assertNotNull(captureTo1)
        assertNotNull(captureTo0)
        assertTrue(captureTo0!!.isPromotion) // Reaching row 0 is promotion
    }

    @Test
    fun testPromotionAtEnemyBorders() {
        // White piece moving from (1, 3) to (0, 3) hits top border
        val customBoard = mapOf(
            Position(1, 3) to Piece("w1", PlayerSide.WHITE)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.WHITE)
        val moves = GameEngine.getLegalMovesForPosition(state, Position(1, 3))
        val promoMove = moves.find { it.to == Position(0, 3) }
        assertNotNull(promoMove)
        assertTrue(promoMove!!.isPromotion)

        val nextState = GameEngine.applyMove(state, promoMove)
        assertTrue(nextState.board[Position(0, 3)]!!.isQueen)
    }

    @Test
    fun testMultiEatingCheckersChainJumps() {
        // Setup a 2-jump sequence:
        // White at (4, 4)
        // Enemy 1 at (3, 4), landing at (2, 4)
        // Enemy 2 at (1, 4), landing at (0, 4)
        val customBoard = mapOf(
            Position(4, 4) to Piece("w1", PlayerSide.WHITE),
            Position(3, 4) to Piece("b1", PlayerSide.BLACK),
            Position(1, 4) to Piece("b2", PlayerSide.BLACK)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.WHITE)
        val firstMoves = GameEngine.getLegalMovesForPosition(state, Position(4, 4))
        val firstJump = firstMoves.find { it.to == Position(2, 4) }
        assertNotNull(firstJump)

        // Execute first jump
        val afterFirstJump = GameEngine.applyMove(state, firstJump!!)
        // Player should still have the turn in multi-jump mode
        assertEquals(PlayerSide.WHITE, afterFirstJump.currentTurn)
        assertEquals(Position(2, 4), afterFirstJump.chainCapturePos)
        assertEquals(1, afterFirstJump.blackGraveyard.size)

        // Candidate moves should only be the subsequent jump from (2, 4) to (0, 4)
        val nextMoves = afterFirstJump.candidateMoves
        val secondJump = nextMoves.find { it.to == Position(0, 4) }
        assertNotNull(secondJump)

        // Execute second jump
        val afterSecondJump = GameEngine.applyMove(afterFirstJump, secondJump!!)
        // Captured both pieces
        assertEquals(2, afterSecondJump.blackGraveyard.size)
        // Landed at (0, 4) and promoted to Queen!
        assertTrue(afterSecondJump.board[Position(0, 4)]!!.isQueen)
        // Combo finished, turn passed to Black
        assertNull(afterSecondJump.chainCapturePos)
        assertEquals(PlayerSide.BLACK, afterSecondJump.currentTurn)
    }

    @Test
    fun testKingSuperpowerDoubleMove() {
        val state = GameState(activePower = Superpower.KING)
        val move = Move(
            from = Position(3, 6),
            to = Position(2, 6),
            piece = state.board[Position(3, 6)]!!,
            superpowerUsed = Superpower.KING
        )
        val nextState = GameEngine.applyMove(state, move)
        // Turn should still be WHITE for the second move
        assertEquals(PlayerSide.WHITE, nextState.currentTurn)
        assertEquals(1, nextState.kingMoveCount)
    }

    @Test
    fun testQueenSuperpowerTransformation() {
        val state = GameState()
        val nextState = GameEngine.applyQueenTransformation(state, Position(3, 6))
        assertTrue(nextState.board[Position(3, 6)]!!.isQueen)
        assertEquals(PlayerSide.BLACK, nextState.currentTurn)

        // Queened piece can move 1 cell backward in addition to forward
        val queenState = nextState.copy(currentTurn = PlayerSide.WHITE)
        val queenMoves = GameEngine.getLegalMovesForPosition(queenState, Position(3, 6))
        assertTrue(queenMoves.isNotEmpty())
        assertTrue(queenMoves.all { (kotlin.math.abs(it.to.row - 3) + kotlin.math.abs(it.to.col - 6)) in 1..2 })
    }

    @Test
    fun testBishopTeleportMoves() {
        val state = GameState(isBishopTeleportMode = true, activePower = Superpower.BISHOP)
        val moves = GameEngine.getLegalMovesForPosition(state, Position(3, 6))
        assertEquals(29, moves.size)
        assertTrue(moves.all { it.isTeleport })
    }

    @Test
    fun testPawnRevivalInExactOldPlace() {
        // Setup: Black piece at (2, 4), White piece at (3, 4).
        // Black jumps over White piece to (4, 4), capturing White piece at (3, 4).
        val customBoard = mapOf(
            Position(2, 4) to Piece("b1", PlayerSide.BLACK),
            Position(3, 4) to Piece("w1", PlayerSide.WHITE)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.BLACK)
        val blackMoves = GameEngine.getLegalMovesForPosition(state, Position(2, 4))
        val captureMove = blackMoves.find { it.to == Position(4, 4) && it.capturedPiece?.id == "w1" }
        assertNotNull(captureMove)

        val stateAfterCapture = GameEngine.applyMove(state, captureMove!!)
        // White graveyard should have the captured piece with capturedAt = (3, 4)
        assertEquals(1, stateAfterCapture.whiteGraveyard.size)
        val deadPiece = stateAfterCapture.whiteGraveyard.first()
        assertEquals("w1", deadPiece.id)
        assertEquals(Position(3, 4), deadPiece.capturedAt)

        // Turn is now White's. White uses PAWN superpower to revive piece in exact old place (3, 4)
        assertEquals(PlayerSide.WHITE, stateAfterCapture.currentTurn)
        val reviveSpots = GameEngine.getReviveDestinations(stateAfterCapture, PlayerSide.WHITE)
        assertEquals(listOf(Position(3, 4)), reviveSpots)

        val stateAfterRevive = GameEngine.applyPawnRevival(stateAfterCapture)
        // Piece should be back at exact position (3, 4)
        assertNotNull(stateAfterRevive.board[Position(3, 4)])
        assertEquals("w1", stateAfterRevive.board[Position(3, 4)]!!.id)
        assertTrue(stateAfterRevive.board[Position(3, 4)]!!.isRevived)
        assertTrue(stateAfterRevive.whiteGraveyard.isEmpty())
        // PAWN superpower consumed
        assertTrue(!stateAfterRevive.whitePowers.contains(Superpower.PAWN))
        // Turn passed to Black
        assertEquals(PlayerSide.BLACK, stateAfterRevive.currentTurn)
    }

    @Test
    fun testPawnRevivalFallbackWhenExactPlaceOccupied() {
        // Captured White piece was at (3, 4), but (3, 4) is now occupied by another piece
        val customBoard = mapOf(
            Position(3, 4) to Piece("b_blocker", PlayerSide.BLACK)
        )
        val state = GameState(
            board = customBoard,
            currentTurn = PlayerSide.WHITE,
            whiteGraveyard = listOf(Piece("w_dead", PlayerSide.WHITE, capturedAt = Position(3, 4)))
        )
        val reviveSpots = GameEngine.getReviveDestinations(state, PlayerSide.WHITE)
        // Since (3, 4) is occupied, revive spots should be empty home squares
        assertTrue(reviveSpots.isNotEmpty())
        assertTrue(!reviveSpots.contains(Position(3, 4)))
    }
}

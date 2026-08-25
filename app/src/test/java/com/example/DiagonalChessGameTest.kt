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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

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

    @Test
    fun testPawnRevivalPreservesQueenedStatus() {
        // Setup: Queened White Piece at (3, 3) is captured by Black piece jumping from (2, 3) to (4, 3)
        val queenedWhitePiece = Piece("w_queen", PlayerSide.WHITE, isQueen = true)
        val customBoard = mapOf(
            Position(2, 3) to Piece("b1", PlayerSide.BLACK),
            Position(3, 3) to queenedWhitePiece
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.BLACK)
        val blackMoves = GameEngine.getLegalMovesForPosition(state, Position(2, 3))
        val captureMove = blackMoves.find { it.to == Position(4, 3) && it.capturedPiece?.id == "w_queen" }
        assertNotNull(captureMove)
        assertTrue(captureMove!!.capturedPiece!!.isQueen)

        val stateAfterCapture = GameEngine.applyMove(state, captureMove)
        // White graveyard has the captured queen
        val capturedDeadPiece = stateAfterCapture.whiteGraveyard.first()
        assertTrue(capturedDeadPiece.isQueen)
        assertEquals(Position(3, 3), capturedDeadPiece.capturedAt)

        // White revives piece
        val stateAfterRevive = GameEngine.applyPawnRevival(stateAfterCapture)
        val revivedPiece = stateAfterRevive.board[Position(3, 3)]
        assertNotNull(revivedPiece)
        // Verified: Revived piece keeps its Queened status!
        assertTrue(revivedPiece!!.isQueen)
        assertTrue(revivedPiece.isRevived)
    }

    @Test
    fun testTimeControlPresetsAndFormatting() {
        val tcBlitz = com.example.model.TimeControl.BLITZ_3_2
        assertEquals(180, tcBlitz.totalSeconds)
        assertEquals(2, tcBlitz.incrementSeconds)
        assertTrue(tcBlitz.isTimed)
        assertEquals("3|2", tcBlitz.shortBadge)

        val tcUnlimited = com.example.model.TimeControl.UNLIMITED
        assertEquals(0, tcUnlimited.totalSeconds)
        assertTrue(!tcUnlimited.isTimed)

        // Time format tests
        assertEquals("03:00", com.example.model.TimeControl.formatTimePrecise(180_000L))
        assertEquals("09.5s", com.example.model.TimeControl.formatTimePrecise(9_500L))
        assertEquals("00:00", com.example.model.TimeControl.formatTimePrecise(0L))
    }

    @Test
    fun testTimeoutGameOverStatus() {
        val state = GameState(status = GameStatus.WHITE_WON_TIMEOUT)
        assertTrue(state.isGameOver())
        assertTrue(state.status.isWhiteWin)
        assertTrue(state.status.isTimeout)

        val stateBlack = GameState(status = GameStatus.BLACK_WON_TIMEOUT)
        assertTrue(stateBlack.isGameOver())
        assertTrue(stateBlack.status.isBlackWin)
        assertTrue(stateBlack.status.isTimeout)
    }

    @Test
    fun testFirstMoveTrackingForTimerStart() {
        val initialState = GameState()
        assertTrue("Initial state must have no moves made", initialState.moveHistory.isEmpty())

        val whiteMoves = GameEngine.getAllLegalMoves(initialState)
        assertTrue("White has valid opening moves", whiteMoves.isNotEmpty())

        val nextState = GameEngine.applyMove(initialState, whiteMoves.first())
        assertEquals("Move history has 1 move after first move", 1, nextState.moveHistory.size)
        assertTrue("First move is made so timer can start", nextState.moveHistory.isNotEmpty())
    }

    @Test
    fun testStalemateDetection() {
        // Construct a board where White has a piece that has no valid forward moves and cannot jump
        // e.g., White piece at top corner (0, 0) trapped by Black pieces at (1, 0) and (0, 1) without landing squares
        val customBoard = mapOf(
            Position(0, 0) to Piece("w1", PlayerSide.WHITE),
            Position(1, 0) to Piece("b1", PlayerSide.BLACK),
            Position(2, 0) to Piece("b2", PlayerSide.BLACK), // Blocks jump landing
            Position(0, 1) to Piece("b3", PlayerSide.BLACK),
            Position(0, 2) to Piece("b4", PlayerSide.BLACK)  // Blocks jump landing
        )
        // White turn, no powers remaining
        val state = GameState(
            board = customBoard,
            currentTurn = PlayerSide.WHITE,
            whitePowers = emptySet(),
            blackPowers = emptySet()
        )
        val whiteMoves = GameEngine.getAllLegalMoves(state)
        assertTrue("White has 0 legal moves", whiteMoves.isEmpty())

        // Win/draw condition evaluation
        val evaluatedState = state.copy(status = GameEngine.checkWinCondition(state.board, state.currentTurn))
        assertEquals(GameStatus.DRAW_STALEMATE, evaluatedState.status)
        assertTrue(evaluatedState.isGameOver())
        assertTrue(evaluatedState.status.isDraw)
    }

    @Test
    fun testThreefoldRepetitionDetection() {
        // Setup two pieces moving back and forth
        val p1 = Position(3, 3)
        val p2 = Position(2, 3)
        val customBoard = mapOf(
            Position(3, 3) to Piece("w1", PlayerSide.WHITE, isQueen = true), // Queen can move back and forth
            Position(0, 0) to Piece("b1", PlayerSide.BLACK, isQueen = true)
        )
        val initSig = GameState.computeSignature(customBoard, PlayerSide.WHITE, emptySet(), emptySet(), null, 0)
        var state = GameState(
            board = customBoard,
            currentTurn = PlayerSide.WHITE,
            whitePowers = emptySet(),
            blackPowers = emptySet(),
            positionHistory = listOf(initSig)
        )

        // Make repeated queen oscillate moves
        // 1. White moves (3,3) -> (2,3)
        val m1 = Move(from = Position(3, 3), to = Position(2, 3), piece = state.board[Position(3, 3)]!!)
        state = GameEngine.applyMove(state, m1)

        // 2. Black moves (0,0) -> (1,0)
        val m2 = Move(from = Position(0, 0), to = Position(1, 0), piece = state.board[Position(0, 0)]!!)
        state = GameEngine.applyMove(state, m2)

        // 3. White moves (2,3) -> (3,3) (State 1 repeats 2nd time)
        val m3 = Move(from = Position(2, 3), to = Position(3, 3), piece = state.board[Position(2, 3)]!!)
        state = GameEngine.applyMove(state, m3)

        // 4. Black moves (1,0) -> (0,0) (Initial position repeated 2nd time)
        val m4 = Move(from = Position(1, 0), to = Position(0, 0), piece = state.board[Position(1, 0)]!!)
        state = GameEngine.applyMove(state, m4)

        // 5. White moves (3,3) -> (2,3)
        val m5 = Move(from = Position(3, 3), to = Position(2, 3), piece = state.board[Position(3, 3)]!!)
        state = GameEngine.applyMove(state, m5)

        // 6. Black moves (0,0) -> (1,0)
        val m6 = Move(from = Position(0, 0), to = Position(1, 0), piece = state.board[Position(0, 0)]!!)
        state = GameEngine.applyMove(state, m6)

        // 7. White moves (2,3) -> (3,3) (State 1 repeats 3rd time)
        val m7 = Move(from = Position(2, 3), to = Position(3, 3), piece = state.board[Position(2, 3)]!!)
        state = GameEngine.applyMove(state, m7)

        // 8. Black moves (1,0) -> (0,0) (Initial position repeated 3rd time -> Repetition Draw triggered!)
        val m8 = Move(from = Position(1, 0), to = Position(0, 0), piece = state.board[Position(1, 0)]!!)
        state = GameEngine.applyMove(state, m8)

        assertEquals(GameStatus.DRAW_REPETITION, state.status)
        assertTrue(state.isGameOver())
        assertTrue(state.status.isDraw)
    }

    @Test
    fun testBoardAngleEnumProperties() {
        val angle45 = com.example.model.BoardAngle.DIAMOND_45
        val angle0 = com.example.model.BoardAngle.GRID_0

        assertEquals("45°", angle45.shortLabel)
        assertEquals("0°", angle0.shortLabel)
        assertEquals("45° Diamond Arena", angle45.title)
        assertEquals("0° Square Grid", angle0.title)
        assertEquals(com.example.model.BoardAngle.GRID_0, angle45.toggle())
        assertEquals(com.example.model.BoardAngle.DIAMOND_45, angle0.toggle())
    }

    @Test
    fun testPawnReviveGhostExpiresAfterPlayerMakesAnotherMove() {
        // Setup: Black piece at (2, 4), White pieces at (3, 4) and (5, 5).
        // Black captures White piece at (3, 4) landing on (4, 4).
        val customBoard = mapOf(
            Position(2, 4) to Piece("b1", PlayerSide.BLACK),
            Position(3, 4) to Piece("w1", PlayerSide.WHITE),
            Position(5, 5) to Piece("w2", PlayerSide.WHITE)
        )
        val state = GameState(board = customBoard, currentTurn = PlayerSide.BLACK)
        val blackMoves = GameEngine.getLegalMovesForPosition(state, Position(2, 4))
        val captureMove = blackMoves.find { it.to == Position(4, 4) && it.capturedPiece?.id == "w1" }
        assertNotNull(captureMove)

        val stateAfterCapture = GameEngine.applyMove(state, captureMove!!)
        // Turn is now White. White has not made any moves since the capture.
        assertEquals(PlayerSide.WHITE, stateAfterCapture.currentTurn)
        assertTrue("White is legally allowed to revive right now", GameEngine.canRevivePawn(stateAfterCapture, PlayerSide.WHITE))

        // White decides NOT to revive and instead plays a normal move with piece w2 from (5,5) to (4,5)
        val whitePiece = stateAfterCapture.board[Position(5, 5)]!!
        val whiteMove = Move(from = Position(5, 5), to = Position(4, 5), piece = whitePiece)
        val stateAfterWhiteMove = GameEngine.applyMove(stateAfterCapture, whiteMove)

        // Turn is now Black's. White played another move, so White can no longer revive piece w1!
        assertEquals(PlayerSide.BLACK, stateAfterWhiteMove.currentTurn)
        assertFalse("White can no longer revive after playing another move", GameEngine.canRevivePawn(stateAfterWhiteMove, PlayerSide.WHITE))
    }

    @Test
    fun testAllLossConditionStepsSelectable() {
        // Test step positions 0..9 and rounding with float interpolations
        val steps = 8 // intermediate steps
        val totalIntervals = steps + 1 // 9 intervals
        val min = 0f
        val max = 9f

        for (i in 0..9) {
            val rawFloat = min + (max - min) * (i.toFloat() / totalIntervals)
            val selectedInt = rawFloat.roundToInt().coerceIn(0, 9)
            assertEquals("Step $i should resolve accurately to $i", i, selectedInt)

            val config = com.example.model.GameRulesConfig(lossPieceThreshold = selectedInt)
            assertEquals(i, config.lossPieceThreshold)
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.engine.AiDecision
import com.example.engine.ChessAi
import com.example.engine.GameEngine
import com.example.engine.GameState
import com.example.model.AiDifficulty
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.model.GameStatus
import com.example.model.Move
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position
import com.example.model.Superpower
import com.example.ui.components.BoardView
import com.example.ui.components.GameOverDialog
import com.example.ui.components.GraveyardView
import com.example.ui.components.PowerDetailDialog
import com.example.ui.components.SuperpowerBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    gameMode: GameMode,
    aiDifficulty: AiDifficulty?,
    theme: BoardTheme,
    soundManager: SoundManager,
    onBackToHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(GameState()) }
    var historyStates by remember { mutableStateOf(listOf<GameState>()) }
    var isFlipped by remember { mutableStateOf(false) }
    var detailPower by remember { mutableStateOf<Superpower?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(soundManager.isAudioMuted()) }

    // Helper: execute move
    fun executeMove(move: Move) {
        historyStates = historyStates + state
        val nextState = GameEngine.applyMove(state, move)
        state = nextState

        if (move.isTeleport) {
            soundManager.playTeleportSound()
        } else if (move.capturedPiece != null) {
            soundManager.playCaptureSound()
        } else if (move.isPromotion) {
            soundManager.playQueenPromoteSound()
        } else {
            soundManager.playMoveSound()
        }

        if (nextState.status != GameStatus.PLAYING) {
            soundManager.playVictorySound()
        }
    }

    // Helper: trigger superpower
    fun activatePower(power: Superpower) {
        val player = state.currentTurn
        val remaining = state.remainingPowers(player)
        if (!remaining.contains(power)) return

        soundManager.playPowerSound()

        when (power) {
            Superpower.KING -> {
                // Double move active for this turn
                state = state.copy(
                    activePower = Superpower.KING,
                    announcement = "👑 King Activated: You have 2 moves this turn!"
                )
            }
            Superpower.QUEEN -> {
                // Enter Queen promotion selection mode
                state = state.copy(
                    isPromotingQueenMode = true,
                    activePower = Superpower.QUEEN,
                    candidateMoves = emptyList(),
                    selectedPos = null,
                    announcement = "👸 Tap any of your pieces to transform it into a Queen!"
                )
            }
            Superpower.ROOK -> {
                // Infinite range mode
                state = state.copy(
                    activePower = Superpower.ROOK,
                    selectedPos = null,
                    candidateMoves = emptyList(),
                    announcement = "🏰 Rook Activated: Move with infinite diagonal range!"
                )
            }
            Superpower.BISHOP -> {
                // Teleport mode
                state = state.copy(
                    isBishopTeleportMode = true,
                    activePower = Superpower.BISHOP,
                    selectedPos = null,
                    candidateMoves = emptyList(),
                    announcement = "🏹 Bishop Activated: Select a piece to teleport anywhere!"
                )
            }
            Superpower.KNIGHT -> {
                // Backward move mode
                state = state.copy(
                    activePower = Superpower.KNIGHT,
                    selectedPos = null,
                    candidateMoves = emptyList(),
                    announcement = "🐴 Knight Activated: Backward diagonal moves enabled!"
                )
            }
            Superpower.PAWN -> {
                // Revive last captured piece in its exact old place
                val graveyard = state.graveyard(player)
                if (graveyard.isEmpty()) {
                    state = state.copy(announcement = "⚠️ No captured pieces to revive!")
                } else {
                    val lastCaptured = graveyard.last()
                    val exactPos = lastCaptured.capturedAt
                    if (exactPos != null && !state.board.containsKey(exactPos)) {
                        // Instant revival in exact original position!
                        historyStates = historyStates + state
                        state = GameEngine.applyPawnRevival(state, exactPos)
                        soundManager.playPowerSound()
                    } else {
                        val reviveSpots = GameEngine.getReviveDestinations(state, player)
                        if (reviveSpots.isEmpty()) {
                            state = state.copy(announcement = "⚠️ No available open squares to revive piece!")
                        } else {
                            val candidateReviveMoves = reviveSpots.map { dest ->
                                Move(
                                    from = dest,
                                    to = dest,
                                    piece = lastCaptured,
                                    superpowerUsed = Superpower.PAWN,
                                    isRevival = true
                                )
                            }
                            state = state.copy(
                                isRevivingPawnMode = true,
                                activePower = Superpower.PAWN,
                                selectedPos = null,
                                candidateMoves = candidateReviveMoves,
                                announcement = if (exactPos != null)
                                    "⚠️ Exact position ${exactPos.notation()} occupied! Tap an open square to place revived piece."
                                else
                                    "♟ Pawn Activated: Tap an emerald square to revive your piece!"
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancel active superpower mode
    fun cancelActivePower() {
        state = state.copy(
            activePower = null,
            isPromotingQueenMode = false,
            isRevivingPawnMode = false,
            isBishopTeleportMode = false,
            selectedPos = null,
            candidateMoves = emptyList(),
            announcement = "Power canceled."
        )
    }

    // Board Square Click Handler
    fun handleSquareClick(pos: Position) {
        if (state.isGameOver() || isAiThinking) return
        val currentTurn = state.currentTurn
        if (gameMode == GameMode.AI && currentTurn == PlayerSide.BLACK) return

        // Multi-Jump continuation lock
        if (state.chainCapturePos != null) {
            val selectedMove = state.candidateMoves.find { it.to == pos }
            if (selectedMove != null) {
                executeMove(selectedMove)
            }
            return
        }

        // 1. Queen Transformation Mode
        if (state.isPromotingQueenMode) {
            val piece = state.board[pos]
            if (piece != null && piece.player == currentTurn && !piece.isQueen) {
                historyStates = historyStates + state
                state = GameEngine.applyQueenTransformation(state, pos)
                soundManager.playQueenPromoteSound()
            }
            return
        }

        // 2. Pawn Revive Placement Mode
        if (state.isRevivingPawnMode) {
            if (!state.board.containsKey(pos)) {
                val allowedReviveSpots = GameEngine.getReviveDestinations(state, currentTurn)
                if (allowedReviveSpots.contains(pos)) {
                    historyStates = historyStates + state
                    state = GameEngine.applyPawnRevival(state, pos)
                    soundManager.playPowerSound()
                }
            }
            return
        }

        // 3. Bishop Teleport Mode
        if (state.isBishopTeleportMode) {
            if (state.selectedPos == null) {
                // First step of teleport: pick own piece
                val piece = state.board[pos]
                if (piece != null && piece.player == currentTurn) {
                    val moves = GameEngine.getLegalMovesForPosition(state, pos)
                    state = state.copy(selectedPos = pos, candidateMoves = moves)
                }
            } else {
                // Second step of teleport: pick destination square
                val selectedMove = state.candidateMoves.find { it.to == pos }
                if (selectedMove != null) {
                    executeMove(selectedMove)
                } else {
                    // Tap another of own pieces or deselect
                    val piece = state.board[pos]
                    if (piece != null && piece.player == currentTurn) {
                        val moves = GameEngine.getLegalMovesForPosition(state, pos)
                        state = state.copy(selectedPos = pos, candidateMoves = moves)
                    } else {
                        state = state.copy(selectedPos = null, candidateMoves = emptyList())
                    }
                }
            }
            return
        }

        // 4. Standard / Power Movement
        if (state.selectedPos == null) {
            val piece = state.board[pos]
            if (piece != null && piece.player == currentTurn) {
                val moves = GameEngine.getLegalMovesForPosition(state, pos)
                state = state.copy(selectedPos = pos, candidateMoves = moves)
            }
        } else {
            val selectedMove = state.candidateMoves.find { it.to == pos }
            if (selectedMove != null) {
                executeMove(selectedMove)
            } else {
                val piece = state.board[pos]
                if (piece != null && piece.player == currentTurn) {
                    val moves = GameEngine.getLegalMovesForPosition(state, pos)
                    state = state.copy(selectedPos = pos, candidateMoves = moves)
                } else {
                    state = state.copy(selectedPos = null, candidateMoves = emptyList())
                }
            }
        }
    }

    // AI Turn Coroutine
    LaunchedEffect(state.currentTurn, state.chainCapturePos, state.status) {
        if (gameMode == GameMode.AI && state.currentTurn == PlayerSide.BLACK && state.status == GameStatus.PLAYING) {
            isAiThinking = true
            delay(if (state.chainCapturePos != null) 400 else 550) // Quick delay between combo jumps
            val decision = ChessAi.computeNextMove(state, aiDifficulty ?: AiDifficulty.TACTICIAN)
            if (decision != null) {
                when (decision) {
                    is AiDecision.RegularMove -> {
                        executeMove(decision.move)
                    }
                    is AiDecision.ActivateSuperpower -> {
                        soundManager.playPowerSound()
                        val powerState = state.copy(activePower = decision.superpower)
                        state = powerState
                        if (decision.followUpMove != null) {
                            executeMove(decision.followUpMove)
                        } else {
                            val normalMoves = GameEngine.getAllLegalMoves(powerState)
                            if (normalMoves.isNotEmpty()) {
                                executeMove(normalMoves.random())
                            }
                        }
                    }
                    is AiDecision.QueenTransform -> {
                        soundManager.playQueenPromoteSound()
                        historyStates = historyStates + state
                        state = GameEngine.applyQueenTransformation(state, decision.pos)
                    }
                    is AiDecision.RevivePawn -> {
                        soundManager.playPowerSound()
                        historyStates = historyStates + state
                        state = GameEngine.applyPawnRevival(state, decision.pos)
                    }
                }
            } else if (state.chainCapturePos != null) {
                state = GameEngine.finishMultiJump(state)
            }
            isAiThinking = false
        }
    }

    // Root UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F0B1A),
                        Color(0xFF151026),
                        Color(0xFF0A0713)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).testTag("game_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (gameMode == GameMode.AI) "vs AI (${aiDifficulty?.title})" else "Pass & Play",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Turn ${state.moveHistory.size + 1}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                Row {
                    if (gameMode == GameMode.PASS_AND_PLAY) {
                        IconButton(
                            onClick = { isFlipped = !isFlipped },
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = "Flip", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = {
                            isMuted = soundManager.toggleMute()
                        },
                        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Gray else Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            if (historyStates.isNotEmpty()) {
                                state = historyStates.last()
                                historyStates = historyStates.dropLast(1)
                            }
                        },
                        enabled = historyStates.isNotEmpty(),
                        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).testTag("undo_move_btn")
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (historyStates.isNotEmpty()) Color.White else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Opponent (Black) Superpower Bar
            SuperpowerBar(
                state = state,
                player = PlayerSide.BLACK,
                isCurrentTurn = state.currentTurn == PlayerSide.BLACK,
                onPowerClick = { activatePower(it) },
                onCancelPower = { cancelActivePower() },
                onShowPowerInfo = { detailPower = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Graveyard Bar
            GraveyardView(
                capturedWhitePieces = state.whiteGraveyard,
                capturedBlackPieces = state.blackGraveyard,
                activeTurn = state.currentTurn
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Turn Indicator / Thinking Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (state.currentTurn == PlayerSide.WHITE) Color(0xFFFFD700).copy(alpha = 0.15f) else Color(0xFF00E5FF).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.currentTurn == PlayerSide.WHITE) Color(0xFFFFD700).copy(alpha = 0.4f) else Color(0xFF00E5FF).copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAiThinking) "🤖 AI is planning strategic move..." else "👉 ${state.currentTurn.displayName}'s Move",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.announcement != null) {
                        Text(
                            text = state.announcement ?: "",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main 7x7 Diagonal Chess Board
            BoardView(
                state = state,
                theme = theme,
                onSquareClick = { handleSquareClick(it) },
                isFlipped = isFlipped
            )

            // Finish Multi-Jump Combo button
            if (state.chainCapturePos != null && (gameMode == GameMode.PASS_AND_PLAY || state.currentTurn == PlayerSide.WHITE)) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Button(
                    onClick = { state = GameEngine.finishMultiJump(state) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("finish_combo_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish Capture Combo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player (White) Superpower Bar
            SuperpowerBar(
                state = state,
                player = PlayerSide.WHITE,
                isCurrentTurn = state.currentTurn == PlayerSide.WHITE,
                onPowerClick = { activatePower(it) },
                onCancelPower = { cancelActivePower() },
                onShowPowerInfo = { detailPower = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Reset Button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        state = GameState()
                        historyStates = emptyList()
                    }
                    .testTag("reset_board_btn"),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart Game", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }

        // Superpower Detail Dialog
        detailPower?.let { power ->
            PowerDetailDialog(
                power = power,
                isAvailable = state.remainingPowers(state.currentTurn).contains(power),
                isCurrentTurn = true,
                onActivate = { activatePower(it) },
                onDismiss = { detailPower = null }
            )
        }

        // Game Over Dialog
        if (state.isGameOver()) {
            GameOverDialog(
                status = state.status,
                moveCount = state.moveHistory.size,
                powersUsedCount = (6 - state.whitePowers.size) + (6 - state.blackPowers.size),
                onRematch = {
                    state = GameState()
                    historyStates = emptyList()
                },
                onHome = onBackToHome
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.model.TimeControl
import com.example.ui.components.BoardView
import com.example.ui.components.EvaluationBar
import com.example.ui.components.GameOverDialog
import com.example.ui.components.GraveyardModalDialog
import com.example.ui.components.MatchRulesDialog
import com.example.ui.components.PawnReviveConfirmDialog
import com.example.ui.components.PowerDetailDialog
import com.example.ui.components.SuperpowerBar
import com.example.ui.components.TimeControlDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    gameMode: GameMode,
    aiDifficulty: AiDifficulty?,
    timeControl: TimeControl = TimeControl.UNLIMITED,
    playerSide: PlayerSide = PlayerSide.WHITE,
    rulesConfig: com.example.model.GameRulesConfig = com.example.model.GameRulesConfig(),
    theme: BoardTheme,
    soundManager: SoundManager,
    onBackToHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentRulesConfig by remember { mutableStateOf(rulesConfig) }
    var currentTimeControl by remember { mutableStateOf(timeControl) }
    var whiteTimeMillis by remember(currentTimeControl) { mutableLongStateOf(currentTimeControl.totalSeconds * 1000L) }
    var blackTimeMillis by remember(currentTimeControl) { mutableLongStateOf(currentTimeControl.totalSeconds * 1000L) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var showTimeControlDialog by remember { mutableStateOf(false) }

    var state by remember { mutableStateOf(GameState(rulesConfig = currentRulesConfig)) }
    var historyStates by remember { mutableStateOf(listOf<GameState>()) }
    var isFlipped by remember { mutableStateOf(playerSide == PlayerSide.BLACK) }
    var boardAngle by remember { mutableStateOf(com.example.model.BoardAngle.DIAMOND_45) }
    var detailPowerInfo by remember { mutableStateOf<Pair<Superpower, PlayerSide>?>(null) }
    var pendingPawnRevive by remember { mutableStateOf<Triple<Piece, Position, PlayerSide>?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(soundManager.isAudioMuted()) }
    var showGraveyardModal by remember { mutableStateOf(false) }
    var showEvalBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    fun applyIncrement(player: PlayerSide) {
        if (currentTimeControl.incrementSeconds > 0) {
            val addMillis = currentTimeControl.incrementSeconds * 1000L
            if (player == PlayerSide.WHITE) {
                whiteTimeMillis += addMillis
            } else {
                blackTimeMillis += addMillis
            }
        }
    }

    // Helper: execute move
    fun executeMove(move: Move) {
        val previousTurn = state.currentTurn
        historyStates = historyStates + state
        val nextState = GameEngine.applyMove(state, move)
        state = nextState

        if (nextState.currentTurn != previousTurn) {
            applyIncrement(previousTurn)
        }

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
    fun activatePower(power: Superpower, player: PlayerSide = state.currentTurn) {
        if (state.isGameOver() || isAiThinking || isTimerPaused) return
        if (player != state.currentTurn) return
        if (gameMode == GameMode.AI && state.currentTurn != playerSide) return
        val remaining = state.remainingPowers(player)
        if (!remaining.contains(power)) return

        if (power == Superpower.PAWN) {
            if (!GameEngine.canRevivePawn(state, player)) {
                val graveyard = state.graveyard(player)
                state = state.copy(
                    announcement = if (graveyard.isEmpty()) "⚠️ No captured pieces to revive!"
                    else "⚠️ Pawn revival expired! You can only revive on the immediate turn after capture."
                )
                return
            }

            val graveyard = state.graveyard(player)
            val lastCaptured = graveyard.last()
            val exactPos = lastCaptured.capturedAt

            if (exactPos != null && !state.board.containsKey(exactPos)) {
                // Open confirmation dialog protecting against accidental misclick!
                pendingPawnRevive = Triple(lastCaptured, exactPos, player)
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
                            "⚠️ Original square ${exactPos.notation()} occupied! Tap an emerald square to choose revival spot."
                        else
                            "♟ Pawn Activated: Tap an emerald square to choose revival spot."
                    )
                }
            }
            return
        }

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
                // Handled above with misclick dialog
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

    // Reset match helper
    fun resetMatch(newTc: TimeControl = currentTimeControl) {
        state = GameState(rulesConfig = currentRulesConfig)
        historyStates = emptyList()
        currentTimeControl = newTc
        whiteTimeMillis = newTc.totalSeconds * 1000L
        blackTimeMillis = newTc.totalSeconds * 1000L
        isTimerPaused = false
    }

    // Board Square Click Handler
    fun handleSquareClick(pos: Position) {
        if (state.isGameOver() || isAiThinking || isTimerPaused) return
        val currentTurn = state.currentTurn
        if (gameMode == GameMode.AI && currentTurn != playerSide) return

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
                applyIncrement(currentTurn)
                soundManager.playQueenPromoteSound()
            }
            return
        }

        // 2. Pawn Revive Placement Mode (Protected with confirmation dialog)
        if (state.isRevivingPawnMode) {
            if (!state.board.containsKey(pos)) {
                val allowedReviveSpots = GameEngine.getReviveDestinations(state, currentTurn)
                if (allowedReviveSpots.contains(pos)) {
                    val lastCaptured = state.graveyard(currentTurn).lastOrNull()
                    if (lastCaptured != null) {
                        pendingPawnRevive = Triple(lastCaptured, pos, currentTurn)
                    }
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

    // Time Control Countdown Engine (starts only after the first move is made)
    LaunchedEffect(state.status, state.currentTurn, isTimerPaused, currentTimeControl, state.moveHistory.size) {
        val isMatchStarted = state.moveHistory.isNotEmpty()
        if (!currentTimeControl.isTimed || isTimerPaused || state.status != GameStatus.PLAYING || !isMatchStarted) return@LaunchedEffect

        var lastTickSecond = -1
        while (state.status == GameStatus.PLAYING && !isTimerPaused && currentTimeControl.isTimed && state.moveHistory.isNotEmpty()) {
            delay(100L)
            if (state.currentTurn == PlayerSide.WHITE) {
                val newTime = (whiteTimeMillis - 100L).coerceAtLeast(0L)
                whiteTimeMillis = newTime
                val currentSec = (newTime / 1000).toInt()
                if (newTime in 1..10_000 && currentSec != lastTickSecond) {
                    lastTickSecond = currentSec
                    soundManager.playLowTimeTick()
                }
                if (newTime <= 0L) {
                    soundManager.playTimeoutBuzzer()
                    state = state.copy(
                        status = GameStatus.BLACK_WON_TIMEOUT,
                        announcement = "⏱️ White ran out of time! Black wins."
                    )
                    break
                }
            } else {
                val newTime = (blackTimeMillis - 100L).coerceAtLeast(0L)
                blackTimeMillis = newTime
                val currentSec = (newTime / 1000).toInt()
                if (newTime in 1..10_000 && currentSec != lastTickSecond) {
                    lastTickSecond = currentSec
                    soundManager.playLowTimeTick()
                }
                if (newTime <= 0L) {
                    soundManager.playTimeoutBuzzer()
                    state = state.copy(
                        status = GameStatus.WHITE_WON_TIMEOUT,
                        announcement = "⏱️ Black ran out of time! White wins."
                    )
                    break
                }
            }
        }
    }

    // AI Turn Coroutine
    LaunchedEffect(state.currentTurn, state.chainCapturePos, state.kingMoveCount, state.status, isTimerPaused) {
        val aiSide = playerSide.opponent()
        if (gameMode == GameMode.AI && state.currentTurn == aiSide && state.status == GameStatus.PLAYING && !isTimerPaused) {
            isAiThinking = true
            delay(if (state.chainCapturePos != null) 350 else 500) // Quick delay between combo jumps
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
                        applyIncrement(aiSide)
                    }
                    is AiDecision.RevivePawn -> {
                        soundManager.playPowerSound()
                        historyStates = historyStates + state
                        state = GameEngine.applyPawnRevival(state, decision.pos)
                        applyIncrement(aiSide)
                    }
                }
            } else if (state.chainCapturePos != null) {
                val prevTurn = state.currentTurn
                state = GameEngine.finishMultiJump(state)
                if (state.currentTurn != prevTurn) {
                    applyIncrement(prevTurn)
                }
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
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top App Bar (Sleek, Pixel-Perfect 40dp Glassmorphic Action Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button + Mode & Turn Information
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onBackToHome)
                            .testTag("game_back_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (gameMode == GameMode.AI) Color(0xFF00E5FF) else Color(0xFFFFD700))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (gameMode == GameMode.AI) "vs AI (${aiDifficulty?.title})" else "Pass & Play",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Turn ${state.moveHistory.size + 1}",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 9.5.sp,
                            modifier = Modifier.padding(start = 11.dp)
                        )
                    }
                }

                // Right: Unified Action Capsule with Uniform Sizing & Breathing Room (No Overlap)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    // Time Control Quick Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentTimeControl.isTimed) Color(0xFFFFD700).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                if (currentTimeControl.isTimed) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { showTimeControlDialog = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("game_tc_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentTimeControl.emoji, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = currentTimeControl.shortBadge,
                                color = if (currentTimeControl.isTimed) Color(0xFFFFD700) else Color.White.copy(alpha = 0.75f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Board Rotation Angle Toggle Button (45° Diamond vs 0° Full Space Grid)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (boardAngle == com.example.model.BoardAngle.GRID_0) Color(0xFFFFD700).copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = if (boardAngle == com.example.model.BoardAngle.GRID_0) 1.2.dp else 0.dp,
                                color = if (boardAngle == com.example.model.BoardAngle.GRID_0) Color(0xFFFFD700) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                boardAngle = if (boardAngle == com.example.model.BoardAngle.DIAMOND_45) com.example.model.BoardAngle.GRID_0 else com.example.model.BoardAngle.DIAMOND_45
                            }
                            .testTag("board_rotation_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (boardAngle == com.example.model.BoardAngle.DIAMOND_45) "45°" else "0°",
                            color = if (boardAngle == com.example.model.BoardAngle.GRID_0) Color(0xFFFFD700) else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Evaluation Bar Toggle Button (Vs User & Vs AI)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (showEvalBar) Color(0xFF00E5FF).copy(alpha = 0.28f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = if (showEvalBar) 1.2.dp else 0.dp,
                                color = if (showEvalBar) Color(0xFF00E5FF) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { showEvalBar = !showEvalBar }
                            .testTag("eval_bar_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📊",
                            fontSize = 12.sp
                        )
                    }

                    // Undo Move Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (historyStates.isNotEmpty()) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
                            .clickable(enabled = historyStates.isNotEmpty()) {
                                state = historyStates.last()
                                historyStates = historyStates.dropLast(1)
                            }
                            .testTag("undo_move_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (historyStates.isNotEmpty()) Color.White else Color.DarkGray,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // More Options Dropdown Menu (Pause/Resume, Graveyard, Mute, Flip, Restart)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { showMoreMenu = true }
                            .testTag("more_options_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF181428))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            if (currentTimeControl.isTimed && state.status == GameStatus.PLAYING) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isTimerPaused) "Resume Timer" else "Pause Timer",
                                            color = if (isTimerPaused) Color(0xFF00E5FF) else Color(0xFFFFD700),
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            tint = if (isTimerPaused) Color(0xFF00E5FF) else Color(0xFFFFD700),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        isTimerPaused = !isTimerPaused
                                        showMoreMenu = false
                                    }
                                )
                            }

                            val totalFallen = state.whiteGraveyard.size + state.blackGraveyard.size
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Graveyard (${totalFallen} fallen)",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Text("💀", fontSize = 14.sp)
                                },
                                onClick = {
                                    showGraveyardModal = true
                                    showMoreMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Rules Config (Loss & Queening)",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showRulesDialog = true
                                    showMoreMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (boardAngle == com.example.model.BoardAngle.DIAMOND_45) "Board Rotation: 0° (Max Space)" else "Board Rotation: 45° (Diamond)",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Text(
                                        if (boardAngle == com.example.model.BoardAngle.DIAMOND_45) "□" else "◇",
                                        color = Color(0xFFFFD700),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    boardAngle = if (boardAngle == com.example.model.BoardAngle.DIAMOND_45) com.example.model.BoardAngle.GRID_0 else com.example.model.BoardAngle.DIAMOND_45
                                    showMoreMenu = false
                                }
                            )

                            if (gameMode == GameMode.PASS_AND_PLAY) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Flip Board",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Flip, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        isFlipped = !isFlipped
                                        showMoreMenu = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isMuted) "Unmute Audio" else "Mute Audio",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = if (isMuted) Color.Gray else Color(0xFFFFD700),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    isMuted = soundManager.toggleMute()
                                    showMoreMenu = false
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Restart Match",
                                        color = Color(0xFFFF8A80),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    resetMatch(currentTimeControl)
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }

            val topPlayer = if (gameMode == GameMode.AI) playerSide.opponent() else (if (isFlipped) PlayerSide.WHITE else PlayerSide.BLACK)
            val topTimeMillis = if (topPlayer == PlayerSide.WHITE) whiteTimeMillis else blackTimeMillis
            val bottomPlayer = if (gameMode == GameMode.AI) playerSide else (if (isFlipped) PlayerSide.BLACK else PlayerSide.WHITE)
            val bottomTimeMillis = if (bottomPlayer == PlayerSide.WHITE) whiteTimeMillis else blackTimeMillis

            // Top Superpower Bar (Opponent in AI mode or Top player in Pass & Play)
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                SuperpowerBar(
                    state = state,
                    player = topPlayer,
                    isCurrentTurn = state.currentTurn == topPlayer,
                    onPowerClick = { activatePower(it, topPlayer) },
                    onCancelPower = { cancelActivePower() },
                    onShowPowerInfo = { detailPowerInfo = Pair(it, topPlayer) },
                    onOpenGraveyard = { showGraveyardModal = true },
                    timeMillis = topTimeMillis,
                    isTimed = currentTimeControl.isTimed,
                    isClockTicking = currentTimeControl.isTimed && state.currentTurn == topPlayer && state.moveHistory.isNotEmpty() && !isTimerPaused && state.status == GameStatus.PLAYING,
                    mirrored = (gameMode == GameMode.PASS_AND_PLAY)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Turn Indicator & Live Action Bar with Inline Status
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isTimerPaused) Color(0xFFFF9800).copy(alpha = 0.12f)
                    else if (state.currentTurn == PlayerSide.WHITE) Color(0xFFFFD700).copy(alpha = 0.10f)
                    else Color(0xFF00E5FF).copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isTimerPaused) Color(0xFFFF9800).copy(alpha = 0.5f)
                    else if (state.currentTurn == PlayerSide.WHITE) Color(0xFFFFD700).copy(alpha = 0.35f)
                    else Color(0xFF00E5FF).copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isTimerPaused) "⏸ MATCH PAUSED"
                                else if (isAiThinking) "🤖 AI thinking..."
                                else "👉 ${state.currentTurn.displayName}'s Turn",
                            color = if (isTimerPaused) Color(0xFFFFB74D) else Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.announcement != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${state.announcement}",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    if (isTimerPaused) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isTimerPaused = false },
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "▶ Resume",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        // Quick info hint / timer status
                        Text(
                            text = if (currentTimeControl.isTimed && state.moveHistory.isEmpty()) "⏱ Starts on 1st move"
                                else if (currentTimeControl.isTimed) "⏱ ${currentTimeControl.title}"
                                else "Tap power for info",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main 7x7 Diagonal Chess Board Container with Integrated Non-Shrinking Live Evaluation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                // Board extends edge-to-edge with zero horizontal margins
                BoardView(
                    state = state,
                    theme = theme,
                    onSquareClick = { handleSquareClick(it) },
                    isFlipped = isFlipped,
                    boardAngle = boardAngle,
                    modifier = Modifier.fillMaxWidth()
                )

                // Seamlessly docked Evaluation Bar in the left negative space of the board frame
                androidx.compose.animation.AnimatedVisibility(
                    visible = showEvalBar,
                    enter = fadeIn() + androidx.compose.animation.slideInHorizontally { -it },
                    exit = fadeOut() + androidx.compose.animation.slideOutHorizontally { -it },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    EvaluationBar(
                        state = state,
                        isFlipped = isFlipped,
                        modifier = Modifier
                            .height(260.dp)
                            .testTag("game_evaluation_bar")
                    )
                }
            }

            // Finish Multi-Jump Combo button (Floating pill if chain capture active)
            if (state.chainCapturePos != null && (gameMode == GameMode.PASS_AND_PLAY || state.currentTurn == playerSide)) {
                Spacer(modifier = Modifier.height(3.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        val prevTurn = state.currentTurn
                        state = GameEngine.finishMultiJump(state)
                        if (state.currentTurn != prevTurn) {
                            applyIncrement(prevTurn)
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("finish_combo_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Finish Capture Combo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Superpower Bar (Human Player in AI mode or Bottom player in Pass & Play)
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                SuperpowerBar(
                    state = state,
                    player = bottomPlayer,
                    isCurrentTurn = state.currentTurn == bottomPlayer,
                    onPowerClick = { activatePower(it, bottomPlayer) },
                    onCancelPower = { cancelActivePower() },
                    onShowPowerInfo = { detailPowerInfo = Pair(it, bottomPlayer) },
                    onOpenGraveyard = { showGraveyardModal = true },
                    timeMillis = bottomTimeMillis,
                    isTimed = currentTimeControl.isTimed,
                    isClockTicking = currentTimeControl.isTimed && state.currentTurn == bottomPlayer && state.moveHistory.isNotEmpty() && !isTimerPaused && state.status == GameStatus.PLAYING,
                    mirrored = false
                )
            }
        }

        // Superpower Detail Dialog
        detailPowerInfo?.let { (power, forPlayer) ->
            val isPlayerTurn = if (gameMode == GameMode.AI) state.currentTurn == playerSide else true
            val isCurrentTurn = state.currentTurn == forPlayer && isPlayerTurn && !isTimerPaused && !state.isGameOver()
            PowerDetailDialog(
                power = power,
                isAvailable = state.remainingPowers(forPlayer).contains(power),
                isCurrentTurn = isCurrentTurn,
                onActivate = { activatePower(it, forPlayer) },
                onDismiss = { detailPowerInfo = null }
            )
        }

        // Pawn Revive Confirmation Dialog (Misclick Protection)
        pendingPawnRevive?.let { (piece, targetPos, player) ->
            PawnReviveConfirmDialog(
                pieceToRevive = piece,
                targetPos = targetPos,
                player = player,
                onConfirm = {
                    val previousTurn = state.currentTurn
                    historyStates = historyStates + state
                    state = GameEngine.applyPawnRevival(state, targetPos)
                    state = state.copy(
                        isRevivingPawnMode = false,
                        activePower = null,
                        candidateMoves = emptyList(),
                        selectedPos = null
                    )
                    applyIncrement(previousTurn)
                    soundManager.playPowerSound()
                    pendingPawnRevive = null
                },
                onDismiss = {
                    pendingPawnRevive = null
                }
            )
        }

        // Graveyard & Revival Chamber Modal Dialog
        if (showGraveyardModal) {
            GraveyardModalDialog(
                capturedWhitePieces = state.whiteGraveyard,
                capturedBlackPieces = state.blackGraveyard,
                hasWhitePawnPower = state.remainingPowers(PlayerSide.WHITE).contains(Superpower.PAWN),
                hasBlackPawnPower = state.remainingPowers(PlayerSide.BLACK).contains(Superpower.PAWN),
                onDismiss = { showGraveyardModal = false }
            )
        }

        // Time Control Modal Dialog
        if (showTimeControlDialog) {
            TimeControlDialog(
                currentSelection = currentTimeControl,
                onSelect = { newTc ->
                    resetMatch(newTc)
                },
                onDismiss = { showTimeControlDialog = false }
            )
        }

        // Match Rules Config Modal Dialog
        if (showRulesDialog) {
            MatchRulesDialog(
                currentConfig = currentRulesConfig,
                onApply = { newConfig ->
                    currentRulesConfig = newConfig
                    state = state.copy(rulesConfig = newConfig)
                },
                onDismiss = { showRulesDialog = false }
            )
        }

        // Game Over Dialog
        if (state.isGameOver()) {
            GameOverDialog(
                status = state.status,
                moveCount = state.moveHistory.size,
                powersUsedCount = (6 - state.whitePowers.size) + (6 - state.blackPowers.size),
                onRematch = {
                    resetMatch(currentTimeControl)
                },
                onHome = onBackToHome
            )
        }
    }
}

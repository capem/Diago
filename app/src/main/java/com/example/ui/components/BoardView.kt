package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.engine.GameState
import com.example.model.BoardAngle
import com.example.model.BoardTheme
import com.example.model.Piece
import com.example.model.PlayerSide
import com.example.model.Position
import com.example.model.Superpower

/**
 * Renders the 7×7 arena for Diagonal Chess.
 * Supports both:
 * - 45° Diamond Arena (isometric diagonal perspective)
 * - 0° Square Grid (straight orthogonal layout maximizing 100% available viewport space)
 */
@Composable
fun BoardView(
    state: GameState,
    theme: BoardTheme,
    onSquareClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    boardAngle: BoardAngle = BoardAngle.DIAMOND_45
) {
    val lastMove = state.moveHistory.lastOrNull()

    val infiniteTransition = rememberInfiniteTransition(label = "board_indicators")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val is0Deg = boardAngle == BoardAngle.GRID_0

    // Outer Board Container
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag("diagonal_chess_board"),
        color = Color.Transparent
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val boardWidth = maxWidth
            val boardHeight = maxHeight
            val boardDimension = minOf(boardWidth, boardHeight)

            // Dynamic geometry parameters based on rotation mode
            val step = if (is0Deg) (boardDimension / 7f) else (boardDimension / 14f)
            val tileSize = if (is0Deg) (boardDimension / 7f) else (step * 1.41421356f)
            val tileRotation = if (is0Deg) 0f else 45f
            val contentRotation = if (is0Deg) 0f else -45f

            // Decorative background canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                if (is0Deg) {
                    // 0° Straight Grid container background
                    val gridTotalPx = boardDimension.toPx()
                    val left = (size.width - gridTotalPx) / 2f
                    val top = (size.height - gridTotalPx) / 2f
                    val cornerRad = 12.dp.toPx()

                    drawRoundRect(
                        color = theme.boardBg,
                        topLeft = Offset(left, top),
                        size = Size(gridTotalPx, gridTotalPx),
                        cornerRadius = CornerRadius(cornerRad, cornerRad)
                    )
                    drawRoundRect(
                        color = theme.accent.copy(alpha = 0.75f),
                        topLeft = Offset(left, top),
                        size = Size(gridTotalPx, gridTotalPx),
                        cornerRadius = CornerRadius(cornerRad, cornerRad),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                } else {
                    // 45° Diamond Arena perimeter
                    val diamondRadius = step.toPx() * 7f
                    val diamondPath = Path().apply {
                        moveTo(cx, cy - diamondRadius)
                        lineTo(cx + diamondRadius, cy)
                        lineTo(cx, cy + diamondRadius)
                        lineTo(cx - diamondRadius, cy)
                        close()
                    }

                    drawPath(
                        path = diamondPath,
                        color = theme.boardBg
                    )
                    drawPath(
                        path = diamondPath,
                        color = theme.accent.copy(alpha = 0.65f),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Neutral center equator line (rank 6)
                    drawLine(
                        color = theme.accent.copy(alpha = 0.35f),
                        start = Offset(cx - diamondRadius * 0.95f, cy),
                        end = Offset(cx + diamondRadius * 0.95f, cy),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // Active corpse tracking for Pawn (Revive) superpower
            // Ghost corpse is ONLY rendered while revival of that piece is legally available (immediate turn after capture before any other move is played)
            val currentTurn = state.currentTurn
            val canCurrentRevive = GameEngine.canRevivePawn(state, currentTurn)
            val currentTurnCorpsePiece = if (canCurrentRevive) state.graveyard(currentTurn).lastOrNull() else null
            val currentTurnCorpsePos = currentTurnCorpsePiece?.capturedAt

            val opponent = currentTurn.opponent()
            val canOpponentRevive = GameEngine.canRevivePawn(state, opponent)
            val opponentCorpsePiece = if (canOpponentRevive) state.graveyard(opponent).lastOrNull() else null
            val opponentCorpsePos = opponentCorpsePiece?.capturedAt

            // Render all 49 squares (7 rows x 7 cols)
            for (r in 0..6) {
                for (c in 0..6) {
                    val pos = Position(r, c)
                    val isDarkSquare = (r + c) % 2 == 1
                    val tileColor = if (isDarkSquare) theme.darkTile else theme.lightTile

                    // Coordinate offset relative to center
                    val tileOffsetX = if (is0Deg) {
                        if (isFlipped) step * (3 - c) else step * (c - 3)
                    } else {
                        val u = if (isFlipped) (r - c) else (c - r)
                        step * u
                    }

                    val tileOffsetY = if (is0Deg) {
                        if (isFlipped) step * (3 - r) else step * (r - 3)
                    } else {
                        val v = if (isFlipped) (6 - (r + c)) else (r + c - 6)
                        step * v
                    }

                    val isSelected = state.selectedPos == pos
                    val isLastMoveFrom = lastMove?.from == pos
                    val isLastMoveTo = lastMove?.to == pos
                    val candidateMove = state.candidateMoves.find { it.to == pos }
                    val isCandidateDestination = candidateMove != null
                    val isCaptureMove = candidateMove?.capturedPiece != null
                    val piece = state.board[pos]

                    // Check if square is in valid queening zone for either player
                    val isWhiteQueeningZone = PlayerSide.WHITE.isPromotionGoal(pos, state.rulesConfig.queenDistanceThreshold)
                    val isBlackQueeningZone = PlayerSide.BLACK.isPromotionGoal(pos, state.rulesConfig.queenDistanceThreshold)
                    val isQueeningZone = isWhiteQueeningZone || isBlackQueeningZone
                    val isWhiteApexCorner = (pos.row == 0 && pos.col == 0)
                    val isBlackApexCorner = (pos.row == 6 && pos.col == 6)

                    // Check if square holds a faint corpse of the latest captured piece
                    val corpsePieceToRender = when {
                        piece == null && currentTurnCorpsePos == pos -> currentTurnCorpsePiece
                        piece == null && opponentCorpsePos == pos -> opponentCorpsePiece
                        else -> null
                    }

                    val tileShape = if (is0Deg) RoundedCornerShape(6.dp) else RoundedCornerShape(4.dp)

                    // Base tile gradient colors + subtle queening highlight
                    val tileBaseColor = if (isLastMoveFrom || isLastMoveTo) theme.accent.copy(alpha = 0.6f)
                    else if (isWhiteQueeningZone && state.currentTurn == PlayerSide.WHITE) Color(0xFFFFD700).copy(alpha = if (isDarkSquare) 0.35f else 0.45f)
                    else if (isBlackQueeningZone && state.currentTurn == PlayerSide.BLACK) Color(0xFF00E5FF).copy(alpha = if (isDarkSquare) 0.35f else 0.45f)
                    else if (isQueeningZone) theme.accent.copy(alpha = if (isDarkSquare) 0.16f else 0.22f)
                    else tileColor

                    // Tile Container
                    Box(
                        modifier = Modifier
                            .size(tileSize)
                            .offset(
                                x = tileOffsetX,
                                y = tileOffsetY
                            )
                            .graphicsLayer(rotationZ = tileRotation)
                            .padding(if (is0Deg) 1.dp else 0.dp)
                            .clip(tileShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        tileBaseColor,
                                        if (isDarkSquare) tileColor.copy(alpha = 0.88f) else tileColor.copy(alpha = 0.98f)
                                    )
                                )
                            )
                            .border(
                                width = if (isSelected) 2.5.dp
                                else if (isLastMoveTo) 2.dp
                                else if (isWhiteApexCorner || isBlackApexCorner) 1.5.dp
                                else if (isQueeningZone) 1.dp
                                else if (is0Deg) 0.5.dp
                                else 0.75.dp,
                                color = if (isSelected) theme.accent
                                else if (isLastMoveTo) theme.accent.copy(alpha = 0.85f)
                                else if (isWhiteApexCorner) Color(0xFFFFD700).copy(alpha = 0.8f)
                                else if (isBlackApexCorner) Color(0xFF00E5FF).copy(alpha = 0.8f)
                                else if (isQueeningZone) theme.accent.copy(alpha = 0.4f)
                                else Color.White.copy(alpha = if (isDarkSquare) 0.10f else 0.20f),
                                shape = tileShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSquareClick(pos)
                            }
                            .testTag("tile_${pos.row}_${pos.col}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle crown / queening glyph watermark on valid promotion squares
                        if (isQueeningZone && piece == null && corpsePieceToRender == null) {
                            Text(
                                text = "♛",
                                color = if (isWhiteApexCorner || isBlackApexCorner) theme.accent.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.16f),
                                fontSize = if (is0Deg) 13.sp else 10.sp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer(rotationZ = contentRotation)
                            )
                        }

                        // Content container inside tile
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(rotationZ = contentRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render Checker Piece if present, or Ghost Corpse if empty & revived target
                            if (piece != null) {
                                PieceView(
                                    piece = piece,
                                    isSelected = isSelected,
                                    modifier = Modifier.fillMaxSize(if (is0Deg) 0.88f else 0.90f)
                                )
                            } else if (corpsePieceToRender != null) {
                                GhostPieceCorpseView(
                                    piece = corpsePieceToRender,
                                    modifier = Modifier.fillMaxSize(if (is0Deg) 0.82f else 0.85f)
                                )
                            }

                            // Move Target Indicators
                            if (isCandidateDestination) {
                                Canvas(modifier = Modifier.fillMaxSize(0.75f)) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.minDimension / 2f

                                    if (isCaptureMove) {
                                        // Capture target ring
                                        drawCircle(
                                            color = Color(0xFFFF1744).copy(alpha = 0.9f),
                                            radius = radius * pulseScale,
                                            center = center,
                                            style = Stroke(width = 2.6.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color(0xFFFF1744).copy(alpha = 0.45f),
                                            radius = radius * 0.45f,
                                            center = center
                                        )
                                    } else if (state.isBishopTeleportMode) {
                                        // Bishop teleport ring
                                        drawCircle(
                                            color = Superpower.BISHOP.accentColor,
                                            radius = radius * pulseScale,
                                            center = center,
                                            style = Stroke(width = 2.2.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Superpower.BISHOP.accentColor.copy(alpha = 0.45f),
                                            radius = radius * 0.35f,
                                            center = center
                                        )
                                    } else {
                                        // Standard move dot
                                        val dotColor = state.activePower?.accentColor ?: Color(0xFF4CAF50)
                                        drawCircle(
                                            color = dotColor.copy(alpha = 0.9f),
                                            radius = radius * 0.36f * pulseScale,
                                            center = center
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.95f),
                                            radius = radius * 0.15f,
                                            center = center
                                        )
                                    }
                                }
                            }

                            // Queen Transform Aura
                            if (state.isPromotingQueenMode && piece?.player == state.currentTurn && !piece.isQueen) {
                                Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
                                    drawCircle(
                                        color = Superpower.QUEEN.accentColor,
                                        radius = size.minDimension / 2f * pulseScale,
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        style = Stroke(width = 2.2.dp.toPx())
                                    )
                                }
                            }

                            // Coordinate notation (e.g. A7, D4, G1)
                            Text(
                                text = pos.notation(),
                                color = if (isDarkSquare) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.32f),
                                fontSize = if (is0Deg) 8.sp else 6.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(if (is0Deg) 2.dp else 0.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

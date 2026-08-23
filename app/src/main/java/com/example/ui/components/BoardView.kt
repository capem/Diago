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
import androidx.compose.ui.geometry.Offset
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
import com.example.engine.GameState
import com.example.model.BoardTheme
import com.example.model.Piece
import com.example.model.Position
import com.example.model.Superpower

/**
 * Renders the 45° rotated diamond arena for Diagonal Chess on a 7×7 grid.
 * Accommodates 10 pieces per player on 49 squares (ranks 0 to 12).
 */
@Composable
fun BoardView(
    state: GameState,
    theme: BoardTheme,
    onSquareClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false
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

    // Outer Diamond Arena Frame
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .testTag("diagonal_chess_board"),
        shape = RoundedCornerShape(20.dp),
        color = theme.boardBg,
        border = androidx.compose.foundation.BorderStroke(
            3.dp,
            Brush.linearGradient(listOf(theme.accent.copy(alpha = 0.85f), theme.darkTile, theme.accent.copy(alpha = 0.4f)))
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val boardWidth = maxWidth
            val boardHeight = maxHeight
            val boardDimension = minOf(boardWidth, boardHeight)

            // Step between tile centers along diagonal axes (12 steps span apex to apex + tile width)
            val step = (boardDimension - 20.dp) / 14f
            val tileSize = step * 1.4142f

            // Decorative background diamond canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val diamondRadius = (step.toPx() * 6f) + (tileSize.toPx() / 1.4142f) + 2.dp.toPx()

                // Outer diamond perimeter
                val diamondPath = Path().apply {
                    moveTo(cx, cy - diamondRadius)
                    lineTo(cx + diamondRadius, cy)
                    lineTo(cx, cy + diamondRadius)
                    lineTo(cx - diamondRadius, cy)
                    close()
                }

                drawPath(
                    path = diamondPath,
                    color = theme.darkTile.copy(alpha = 0.5f)
                )
                drawPath(
                    path = diamondPath,
                    color = theme.accent.copy(alpha = 0.4f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Neutral center equator line (rank 6)
                drawLine(
                    color = theme.accent.copy(alpha = 0.3f),
                    start = Offset(cx - diamondRadius * 0.95f, cy),
                    end = Offset(cx + diamondRadius * 0.95f, cy),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Render all 49 diamond squares (7 rows x 7 cols)
            for (r in 0..6) {
                for (c in 0..6) {
                    val pos = Position(r, c)
                    val isDarkSquare = (r + c) % 2 == 1
                    val tileColor = if (isDarkSquare) theme.darkTile else theme.lightTile

                    // Coordinate offset in 45-degree diamond projection relative to center (0,0)
                    val u = if (isFlipped) (r - c) else (c - r)
                    val v = if (isFlipped) (6 - (r + c)) else (r + c - 6)

                    val tileOffsetX = step * u
                    val tileOffsetY = step * v

                    val isSelected = state.selectedPos == pos
                    val isLastMoveFrom = lastMove?.from == pos
                    val isLastMoveTo = lastMove?.to == pos
                    val candidateMove = state.candidateMoves.find { it.to == pos }
                    val isCandidateDestination = candidateMove != null
                    val isCaptureMove = candidateMove?.capturedPiece != null
                    val piece = state.board[pos]

                    // Diamond Tile Container
                    Box(
                        modifier = Modifier
                            .size(tileSize)
                            .offset(
                                x = tileOffsetX,
                                y = tileOffsetY
                            )
                            .graphicsLayer(rotationZ = 45f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        if (isLastMoveFrom || isLastMoveTo) theme.accent.copy(alpha = 0.55f)
                                        else tileColor,
                                        if (isDarkSquare) tileColor.copy(alpha = 0.85f) else tileColor.copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else if (isLastMoveTo) 2.dp else 0.75.dp,
                                color = if (isSelected) theme.accent
                                else if (isLastMoveTo) theme.accent.copy(alpha = 0.8f)
                                else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
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
                        // Content container inside tile, un-rotated by -45° to keep pieces and text upright
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(rotationZ = -45f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render Checker Piece if present
                            if (piece != null) {
                                PieceView(
                                    piece = piece,
                                    isSelected = isSelected,
                                    modifier = Modifier.fillMaxSize(0.9f)
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
                                            color = Color(0xFFFF1744).copy(alpha = 0.85f),
                                            radius = radius * pulseScale,
                                            center = center,
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color(0xFFFF1744).copy(alpha = 0.4f),
                                            radius = radius * 0.45f,
                                            center = center
                                        )
                                    } else if (state.isBishopTeleportMode) {
                                        // Bishop teleport ring
                                        drawCircle(
                                            color = Superpower.BISHOP.accentColor,
                                            radius = radius * pulseScale,
                                            center = center,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Superpower.BISHOP.accentColor.copy(alpha = 0.4f),
                                            radius = radius * 0.35f,
                                            center = center
                                        )
                                    } else {
                                        // Standard move dot
                                        val dotColor = state.activePower?.accentColor ?: Color(0xFF4CAF50)
                                        drawCircle(
                                            color = dotColor.copy(alpha = 0.85f),
                                            radius = radius * 0.36f * pulseScale,
                                            center = center
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.9f),
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
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }

                            // Coordinate notation (e.g. A7, D4, G1)
                            Text(
                                text = pos.notation(),
                                color = if (isDarkSquare) Color.White.copy(alpha = 0.26f) else Color.Black.copy(alpha = 0.26f),
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(0.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

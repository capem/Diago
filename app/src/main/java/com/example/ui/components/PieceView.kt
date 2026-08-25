package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Piece
import com.example.model.PlayerSide

@Composable
fun PieceView(
    piece: Piece,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "piece_anim")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val isWhite = piece.player == PlayerSide.WHITE

    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 2.dp.toPx()

            // 1. Selection aura / glow
            if (isSelected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.75f), Color(0xFFFF9100).copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = radius * 1.5f * pulseGlow
                    ),
                    radius = radius * 1.4f,
                    center = center
                )
            }

            // 2. High-Contrast Outer Drop Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.65f),
                radius = radius,
                center = Offset(center.x, center.y + 3.5.dp.toPx())
            )

            // 3. High-Contrast Outer Boundary Ring (Ensures White pieces pop on light tiles & Black pieces pop on dark tiles)
            val outerBoundaryColor = if (isWhite) {
                Color(0xFF2E241A) // Strong dark boundary for white piece
            } else {
                Color(0xFFDED6F2) // Crisp luminous light-silver boundary for black piece
            }
            drawCircle(
                color = outerBoundaryColor,
                radius = radius + 0.8.dp.toPx(),
                center = center
            )

            // 4. Base Checker Gradient (Pure Ivory Porcelain vs Midnight Obsidian)
            val baseColors = if (isWhite) {
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFAF4ED),
                    Color(0xFFEBE0D0),
                    Color(0xFFB59F89)
                )
            } else {
                listOf(
                    Color(0xFF3C3549),
                    Color(0xFF201A2C),
                    Color(0xFF110C1C),
                    Color(0xFF06030B)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = baseColors,
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )

            // 5. Queen Royal Tiara Ring or Regular Checker Rim
            if (piece.isQueen) {
                // Royal Queen Golden Outer Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isWhite) {
                            listOf(Color(0xFFFFDF7A), Color(0xFFD4A017), Color(0xFF8B6508))
                        } else {
                            listOf(Color(0xFFFFF099), Color(0xFFFFD700), Color(0xFFFF9100))
                        },
                        center = center,
                        radius = radius * 0.95f
                    ),
                    radius = radius * 0.88f,
                    center = center,
                    style = Stroke(width = 2.4.dp.toPx())
                )

                // 4 Royal Tiara Jewel Pips at 0, 90, 180, 270 deg
                val jewelRadius = radius * 0.88f
                val jewelPipSize = 2.2.dp.toPx()
                val jewelColor = if (isWhite) Color(0xFF684805) else Color(0xFFFFE57F)
                drawCircle(color = jewelColor, radius = jewelPipSize, center = Offset(center.x, center.y - jewelRadius))
                drawCircle(color = jewelColor, radius = jewelPipSize, center = Offset(center.x, center.y + jewelRadius))
                drawCircle(color = jewelColor, radius = jewelPipSize, center = Offset(center.x - jewelRadius, center.y))
                drawCircle(color = jewelColor, radius = jewelPipSize, center = Offset(center.x + jewelRadius, center.y))
            } else {
                // Regular Outer Rim Groove
                drawCircle(
                    color = if (isWhite) Color(0xFF9E846E) else Color(0xFF7A6899),
                    radius = radius * 0.88f,
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }

            // 6. Inner Concentric Ring
            val innerRingColor = if (isWhite) Color(0xFFD4C2B0) else Color(0xFF282038)
            drawCircle(
                color = innerRingColor,
                radius = radius * 0.65f,
                center = center
            )

            drawCircle(
                color = if (isWhite) Color(0xFF8F7762) else Color(0xFF8A73AC),
                radius = radius * 0.65f,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )

            // 7. Center Bullseye Core / Queen Throne Medallion
            if (piece.isQueen) {
                // High contrast medallion disk behind the Queen Crown
                val medallionBg = if (isWhite) {
                    Color(0xFFF9F1E6) // Bright ivory medallion disk
                } else {
                    Color(0xFF130E20) // Deep obsidian medallion disk
                }
                drawCircle(
                    color = medallionBg,
                    radius = radius * 0.44f,
                    center = center
                )
                // Gold ring around the center crown medallion
                drawCircle(
                    color = if (isWhite) Color(0xFFC4931E) else Color(0xFFFFD700),
                    radius = radius * 0.44f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                val coreColor = if (isWhite) Color(0xFFEBE0D2) else Color(0xFF1B1527)
                drawCircle(
                    color = coreColor,
                    radius = radius * 0.38f,
                    center = center
                )
            }

            // 8. Top Specular Glaze Highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = if (isWhite) 0.6f else 0.35f), Color.Transparent),
                    center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f),
                    radius = radius * 0.55f
                ),
                radius = radius * 0.48f,
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.32f)
            )

            // 9. Revived piece emerald badge border if applicable
            if (piece.isRevived) {
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = radius * 0.98f,
                    center = center,
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
        }

        // 10. Queen Crown Insignia with sharp White vs Black color coding
        if (piece.isQueen) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isWhite) {
                    // White Queen: Deep Regal Gold Crown with dark contrast shadow on bright ivory body
                    Text(
                        text = "♛",
                        color = Color(0xFF261904).copy(alpha = 0.55f),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 1.dp, start = 1.dp)
                    )
                    Text(
                        text = "♛",
                        color = Color(0xFFB8860B), // Dark Goldenrod
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    // Black Queen: Radiant Luminous 24K Gold Crown on deep obsidian body
                    Text(
                        text = "♛",
                        color = Color.Black.copy(alpha = 0.8f),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 1.5.dp)
                    )
                    Text(
                        text = "♛",
                        color = Color(0xFFFFD700), // Vibrant 24K Gold
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * Renders an ethereal, glowing translucent ghost/corpse of the latest captured piece
 * on the exact square where it fell, visible when the player has the Pawn (Revive) superpower.
 */
@Composable
fun GhostPieceCorpseView(
    piece: Piece,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_corpse_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corpse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corpse_scale"
    )

    val isWhite = piece.player == PlayerSide.WHITE

    Box(
        modifier = modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f - 2.dp.toPx()) * pulseScale

            // 1. Ethereal Emerald Soul Halo (Pawn revival color)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = pulseAlpha * 0.85f),
                        Color(0xFF00E676).copy(alpha = pulseAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )

            // 2. Faint Ghostly Checker Body
            val baseColors = if (isWhite) {
                listOf(
                    Color(0xFFFAF0E6).copy(alpha = pulseAlpha * 0.95f),
                    Color(0xFFE0D0C0).copy(alpha = pulseAlpha * 0.80f),
                    Color(0xFF8F7662).copy(alpha = pulseAlpha * 0.50f)
                )
            } else {
                listOf(
                    Color(0xFF5A4D73).copy(alpha = pulseAlpha * 0.95f),
                    Color(0xFF2E2342).copy(alpha = pulseAlpha * 0.80f),
                    Color(0xFF140F20).copy(alpha = pulseAlpha * 0.50f)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = baseColors,
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius * 1.1f
                ),
                radius = radius,
                center = center
            )

            // 3. Glowing Emerald Revival Border
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = (pulseAlpha * 1.3f).coerceAtMost(0.9f)),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        // 4. Ghost Insignia (Crown for Queened piece or Pawn Soul symbol)
        if (piece.isQueen) {
            Text(
                text = "♛",
                color = Color(0xFFFFD700).copy(alpha = (pulseAlpha * 1.8f).coerceAtMost(0.95f)),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(
                text = "♟",
                color = Color(0xFF00E676).copy(alpha = (pulseAlpha * 1.8f).coerceAtMost(0.95f)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

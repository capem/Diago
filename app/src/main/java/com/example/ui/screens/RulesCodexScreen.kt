package com.example.ui.screens

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
import com.example.ui.theme.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.Superpower

@Composable
fun RulesCodexScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0A1A), Color(0xFF161028), Color(0xFF07050E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).testTag("codex_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Diagonal Chess Codex",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Official Rules & Superpower Mechanics",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Objective & Board
                RuleCard(
                    number = "1",
                    title = "Objective & Board",
                    content = "Diagonal Chess is a strategy game played on a 7×7 (45° rotated) diamond board using black and white checker/dame pieces (10 pieces per player, 49 squares total).\n\nEach player has the same set of six superpowers. Each superpower can be used only once per game.\n\nThe goal is to defeat the opponent according to the game's standard capture/winning rules."
                )

                // Section 2: Pieces
                RuleCard(
                    number = "2",
                    title = "Identical Pieces",
                    content = "There are no different physical chess pieces. All pieces are identical dame/checker pieces.\n\nThe chess piece names — King, Queen, Rook, Bishop, Knight and Pawn — refer strictly to the player's one-time superpowers, NOT physical shapes."
                )

                // Section 3: Movement & Jumping Captures
                RuleCard(
                    number = "3",
                    title = "Movement & Checkers-Style Jumping",
                    content = "• Regular Pieces: Move forward one cell at a time towards the opponent's apex.\n• Damed / Queened Pieces: Move only one cell at a time, but gain the permanent ability to move and capture backward in all 4 directions.\n• Captures: Follow checkers-style jumping. A piece leaps over an adjacent opponent piece to land on the empty cell immediately behind it, removing the captured piece.\n• Rotated Board: Because the board is rotated 45°, pieces being jumped are visually adjacent along the movement lines of the diamond arena."
                )

                // Section 4: The 6 Superpowers
                Text(
                    text = "THE 6 SUPERPOWERS",
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Superpower.entries.forEach { power ->
                    SuperpowerRuleCard(power)
                }

                // Section 5: One-Time Rules
                RuleCard(
                    number = "5",
                    title = "One-Time Resource Rule",
                    content = "Each player starts with 1× of each power:\n• 1× King (Double Move)\n• 1× Queen (Promote Any Piece)\n• 1× Rook (Infinite Range)\n• 1× Bishop (Teleport)\n• 1× Knight (Backward Move)\n• 1× Pawn (Revive)\n\nUsing a power permanently consumes it. A player cannot use the same power again during that game. Players do not have to use their powers."
                )

                // Section 6: Strategy
                RuleCard(
                    number = "6",
                    title = "Strategy & Timing",
                    content = "The superpowers are intentionally limited. The key to the game is deciding WHEN to spend a power.\n\nUsing a powerful ability early may provide an advantage, but leaves you vulnerable later. The opponent can also try to bait you into wasting a power."
                )

                // Section 7: Important Rule
                RuleCard(
                    number = "7",
                    title = "Draw & Stalemate Conditions",
                    content = "• Stalemate: If a player on their turn has no legal moves remaining (and is not in a winning position), the game concludes in a Stalemate (Draw).\n• Threefold Repetition: If the exact same board position and player turn repeats 3 times in a match, the game is declared a Draw by Repetition.\n• Timeouts: If playing with a clock and a player runs out of time, their opponent wins on time."
                )

                // Section 8: Board Orientation
                RuleCard(
                    number = "8",
                    title = "Board Perspective & Full Space View",
                    content = "• 45° Diamond Arena: The classic isometric diamond chess perspective.\n• 0° Grid (Max Space): Straight orthogonal grid view that expands tiles by ~41% to utilize 100% of your screen's width and height.\n• Toggle between 45° and 0° anytime during a match via the top header or options menu."
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RuleCard(
    number: String,
    title: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF19142E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = number, color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = content,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun SuperpowerRuleCard(power: Superpower) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1530),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, power.accentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(power.accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = power.emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${power.title} — ${power.subtitle}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = power.description,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

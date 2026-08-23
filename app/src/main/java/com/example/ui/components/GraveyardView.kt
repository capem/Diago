package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Piece
import com.example.model.PlayerSide

@Composable
fun GraveyardView(
    capturedWhitePieces: List<Piece>,
    capturedBlackPieces: List<Piece>,
    activeTurn: PlayerSide,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // White Captured
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "White Lost: ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                if (capturedWhitePieces.isEmpty()) {
                    Text(text = "None", color = Color.Gray, fontSize = 11.sp)
                } else {
                    capturedWhitePieces.forEach { piece ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFAF0E6))
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (piece.isQueen) {
                                Text("♛", fontSize = 8.sp, color = Color(0xFF8B6508))
                            }
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                }
            }

            // Black Captured
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Black Lost: ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                if (capturedBlackPieces.isEmpty()) {
                    Text(text = "None", color = Color.Gray, fontSize = 11.sp)
                } else {
                    capturedBlackPieces.forEach { piece ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2C243B))
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (piece.isQueen) {
                                Text("♛", fontSize = 8.sp, color = Color(0xFFFFD700))
                            }
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                }
            }
        }
    }
}

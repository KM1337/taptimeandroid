package com.taptime.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taptime.game.GameManager
import com.taptime.game.ui.components.NeonButton
import com.taptime.game.ui.components.neonGlow
import com.taptime.game.ui.theme.NeonPalette

@Composable
fun GameOverScreen(
    gameManager: GameManager,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
) {
    val state by gameManager.state.collectAsStateWithLifecycle()
    val accent = gameManager.currentTheme.colors.accent
    val isNewHighScore = state.finalScore == state.highScore && state.finalScore > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "GAME OVER",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = NeonPalette.Red,
                modifier = Modifier.neonGlow(NeonPalette.Red),
            )

            if (isNewHighScore) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NEW HIGH SCORE!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Heavy,
                    color = NeonPalette.Gold,
                    modifier = Modifier.neonGlow(NeonPalette.Gold),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatLine("Score", "${state.finalScore}", Color.White)
                StatLine("Best", "${state.highScore}", NeonPalette.Gold)
                StatLine("Coins Earned", "+${state.coinsEarnedThisRun}", NeonPalette.Green)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!state.hasUsedRevive) {
                NeonButton(
                    label = if (state.isWatchingAd) {
                        "Loading..."
                    } else {
                        "Watch Ad — Revive (+1 Life)"
                    },
                    color = NeonPalette.Green,
                    filled = true,
                    fontSize = 18,
                    enabled = !state.isWatchingAd,
                    onClick = { gameManager.watchAdForRevive() },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            NeonButton(
                label = "↻  RETRY",
                color = accent,
                filled = true,
                fontSize = 20,
                onClick = onRetry,
            )
            Spacer(modifier = Modifier.height(12.dp))
            NeonButton(
                label = "⌂  MAIN MENU",
                color = Color.White.copy(alpha = 0.5f),
                filled = false,
                fontSize = 16,
                onClick = onMenu,
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

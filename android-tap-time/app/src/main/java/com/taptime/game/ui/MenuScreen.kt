package com.taptime.game.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.taptime.game.GameManagerState
import com.taptime.game.ui.components.CyberpunkBackground
import com.taptime.game.ui.components.PulsingGlassButton
import com.taptime.game.ui.components.StatChip
import com.taptime.game.ui.components.neonGlow
import com.taptime.game.ui.theme.NeonPalette

@Composable
fun MenuScreen(
    gameManager: GameManager,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onDailyReward: () -> Unit,
    onHighScore: () -> Unit,
) {
    val state by gameManager.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        CyberpunkBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            TopStatBar(state, gameManager)

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "TAP",
                    fontSize = 82.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonPalette.Cyan,
                    modifier = Modifier.neonGlow(NeonPalette.Cyan),
                )
                Text(
                    text = "TIME",
                    fontSize = 82.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.neonGlow(NeonPalette.Purple),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                PulsingGlassButton(
                    label = "▶  PLAY",
                    color = NeonPalette.Cyan,
                    fontSize = 26,
                    onClick = onPlay,
                )
                PulsingGlassButton(
                    label = "🛍  SHOP",
                    color = NeonPalette.Purple,
                    fontSize = 22,
                    onClick = onShop,
                )
                PulsingGlassButton(
                    label = "🏆  HIGH SCORE",
                    color = NeonPalette.Gold,
                    fontSize = 20,
                    onClick = onHighScore,
                )
                if (state.canClaimDailyReward) {
                    PulsingGlassButton(
                        label = "🎁  DAILY GIFT",
                        color = NeonPalette.Gold,
                        fontSize = 18,
                        onClick = onDailyReward,
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun TopStatBar(state: GameManagerState, gameManager: GameManager) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatChip(text = "🏆 ${state.highScore}", color = NeonPalette.Gold)
        StatChip(text = "◎ ${state.totalCoins}", color = NeonPalette.Gold)

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = { gameManager.setMuted(!state.isMuted) },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
        ) {
            Icon(
                imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Mute",
                tint = if (state.isMuted) Color.White.copy(alpha = 0.3f) else NeonPalette.Cyan,
            )
        }

        StatChip(
            text = gameManager.currentTheme.name,
            color = gameManager.currentTheme.colors.accent.copy(alpha = 0.8f),
        )
    }
}

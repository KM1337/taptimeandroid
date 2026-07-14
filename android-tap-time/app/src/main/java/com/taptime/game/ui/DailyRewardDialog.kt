package com.taptime.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taptime.game.GameManager
import com.taptime.game.ui.components.NeonButton
import com.taptime.game.ui.components.neonGlow
import com.taptime.game.ui.theme.NeonPalette

private val DailyRewards = listOf(100, 200, 300, 400, 500, 750, 1000)

@Composable
fun DailyRewardDialog(
    gameManager: GameManager,
    onDismiss: () -> Unit,
) {
    val state by gameManager.state.collectAsStateWithLifecycle()
    var hasClaimed by remember { mutableStateOf(false) }
    var claimedAmount by remember { mutableIntStateOf(0) }
    var hasDoubled by remember { mutableStateOf(false) }

    val displayDay = when {
        hasClaimed -> state.dailyStreak
        state.canClaimDailyReward -> {
            val last = state.lastDailyClaimEpochMillis
            if (last != null && System.currentTimeMillis() - last >= 48 * 3_600 * 1_000L) {
                1
            } else {
                minOf(state.dailyStreak + 1, 7)
            }
        }
        else -> state.dailyStreak.coerceAtLeast(1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF14141E))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "DAILY REWARD",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = NeonPalette.Gold,
                modifier = Modifier.neonGlow(NeonPalette.Gold),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items((1..7).toList()) { day ->
                    DayCell(
                        day = day,
                        reward = DailyRewards[day - 1],
                        isClaimed = day <= state.dailyStreak && !state.canClaimDailyReward,
                        isCurrent = day == displayDay && state.canClaimDailyReward && !hasClaimed,
                        wasJustClaimed = hasClaimed && day == state.dailyStreak,
                    )
                }
            }

            Text(
                text = "${DailyRewards[(displayDay - 1).coerceIn(0, 6)]} Coins",
                fontSize = 26.sp,
                fontWeight = FontWeight.Heavy,
                color = NeonPalette.Gold,
            )

            when {
                state.canClaimDailyReward && !hasClaimed -> {
                    NeonButton(
                        label = "CLAIM REWARD",
                        color = NeonPalette.Gold,
                        onClick = {
                            claimedAmount = gameManager.claimDailyReward()
                            hasClaimed = true
                        },
                    )
                }
                hasClaimed && !hasDoubled -> {
                    Text(
                        "Claimed +$claimedAmount!",
                        color = NeonPalette.Green,
                        fontWeight = FontWeight.Bold,
                    )
                    NeonButton(
                        label = if (state.isWatchingAd) {
                            "Watching ad..."
                        } else {
                            "Double via Ad! (+$claimedAmount)"
                        },
                        color = NeonPalette.Green,
                        enabled = !state.isWatchingAd,
                        onClick = {
                            gameManager.doubleDailyReward()
                            hasDoubled = true
                        },
                    )
                }
                hasDoubled -> {
                    Text(
                        "Reward doubled! +${claimedAmount * 2} total",
                        color = NeonPalette.Green,
                        fontWeight = FontWeight.Bold,
                    )
                }
                else -> {
                    Text(
                        "Come back tomorrow!",
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }

            NeonButton(
                label = "Close",
                color = Color.White.copy(alpha = 0.5f),
                filled = false,
                fontSize = 16,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    reward: Int,
    isClaimed: Boolean,
    isCurrent: Boolean,
    wasJustClaimed: Boolean,
) {
    val highlighted = isClaimed || wasJustClaimed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isCurrent -> NeonPalette.Gold.copy(alpha = 0.15f)
                    highlighted -> NeonPalette.Gold.copy(alpha = 0.08f)
                    else -> Color.White.copy(alpha = 0.04f)
                },
            )
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = when {
                    isCurrent -> NeonPalette.Gold
                    highlighted -> NeonPalette.Gold.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Day $day",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) NeonPalette.Gold else Color.White.copy(alpha = 0.5f),
        )
        Text(
            "◎ $reward",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) NeonPalette.Gold else Color.White.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun HighScoreDialog(
    highScore: Int,
    totalCoins: Int,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14141E))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "HIGH SCORE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = NeonPalette.Gold,
                modifier = Modifier.neonGlow(NeonPalette.Gold),
            )
            Text("Best Run: $highScore", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Total Coins: $totalCoins", color = NeonPalette.Green, fontSize = 18.sp)
            NeonButton(label = "Close", color = NeonPalette.Cyan, filled = false, fontSize = 16, onClick = onDismiss)
        }
    }
}

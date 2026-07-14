package com.taptime.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taptime.game.ads.AdMobRewardedAdManager
import com.taptime.game.ui.DailyRewardDialog
import com.taptime.game.ui.GameOverScreen
import com.taptime.game.ui.GameScreen
import com.taptime.game.ui.HighScoreDialog
import com.taptime.game.ui.MenuScreen
import com.taptime.game.ui.ShopScreen
import com.taptime.game.ui.theme.TapTimeTheme

class MainActivity : ComponentActivity() {

    private lateinit var gameManager: GameManager
    private lateinit var adManager: AdMobRewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        adManager = AdMobRewardedAdManager(this).also { it.configure() }
        gameManager = GameManager(preferences, adManager)

        setContent {
            TapTimeTheme {
                val state by gameManager.state.collectAsStateWithLifecycle()
                var showDailyReward by remember { mutableStateOf(false) }
                var showHighScore by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = state.gameState, label = "screen") { screen ->
                        when (screen) {
                            GameState.MENU -> MenuScreen(
                                gameManager = gameManager,
                                onPlay = { gameManager.startGame() },
                                onShop = { gameManager.openShop() },
                                onDailyReward = { showDailyReward = true },
                                onHighScore = { showHighScore = true },
                            )
                            GameState.PLAYING -> GameScreen(gameManager = gameManager)
                            GameState.SHOP -> ShopScreen(
                                gameManager = gameManager,
                                onBack = { gameManager.returnToMenu() },
                            )
                            GameState.GAME_OVER -> GameOverScreen(
                                gameManager = gameManager,
                                onRetry = { gameManager.startGame() },
                                onMenu = { gameManager.returnToMenu() },
                            )
                        }
                    }

                    if (showDailyReward) {
                        DailyRewardDialog(
                            gameManager = gameManager,
                            onDismiss = { showDailyReward = false },
                        )
                    }
                    if (showHighScore) {
                        HighScoreDialog(
                            highScore = state.highScore,
                            totalCoins = state.totalCoins,
                            onDismiss = { showHighScore = false },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "tap_time"
    }
}

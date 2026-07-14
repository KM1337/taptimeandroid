package com.taptime.game

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.taptime.game.ads.RewardedAdManager
import com.taptime.game.model.BackgroundCatalog
import com.taptime.game.model.BackgroundTheme
import com.taptime.game.model.GameTheme
import com.taptime.game.model.ThemeCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GameState {
    MENU,
    PLAYING,
    GAME_OVER,
    SHOP,
}

enum class TargetType {
    STANDARD,
    DANGER,
    EXTRA_LIFE,
}

data class TapResult(val points: Int, val coins: Int)

/**
 * Immutable snapshot of all observable game state for Jetpack Compose.
 * Collect [GameManager.state] with `collectAsStateWithLifecycle()`.
 */
@Immutable
data class GameManagerState(
    val score: Int = 0,
    val lives: Int = 3,
    val combo: Int = 0,
    val gameState: GameState = GameState.MENU,
    val hasUsedRevive: Boolean = false,
    val sessionId: Int = 0,
    val isWatchingAd: Boolean = false,
    val needsClear: Boolean = false,
    val isFever: Boolean = false,
    val feverTimeRemaining: Double = 0.0,
    val finalScore: Int = 0,
    val coinsEarnedThisRun: Int = 0,
    val bankedCoins: Int = 0,
    val highScore: Int = 0,
    val selectedThemeId: String = "neon_circle",
    val unlockedThemeIds: Set<String> = emptySet(),
    val selectedBackgroundId: String = "deep_space",
    val unlockedBackgroundIds: Set<String> = emptySet(),
    val dailyStreak: Int = 0,
    val lastDailyClaimEpochMillis: Long? = null,
    val isMuted: Boolean = false,
) {
    val maxLives: Int get() = GameManager.MAX_LIVES

    /** Combo tiers: x1 (0-4), x2 (5-9), x3 (10-19), x4 (20-29), x5 (30+). */
    val comboMultiplier: Int
        get() = when (combo) {
            in 0..4 -> 1
            in 5..9 -> 2
            in 10..19 -> 3
            in 20..29 -> 4
            else -> 5
        }

    val isGameOver: Boolean get() = lives <= 0
    val totalCoins: Int get() = bankedCoins + coinsEarnedThisRun

    val canClaimDailyReward: Boolean
        get() {
            val lastClaim = lastDailyClaimEpochMillis ?: return true
            return System.currentTimeMillis() - lastClaim >= 24 * 3_600 * 1_000L
        }
}

/**
 * Central game state, persistence, and shop logic.
 * Android port of the iOS [GameManager] with [StateFlow] for Compose.
 */
class GameManager(
    private val preferences: SharedPreferences,
    private val adManager: RewardedAdManager,
) {
    private val _state = MutableStateFlow(GameManagerState())
    val state: StateFlow<GameManagerState> = _state.asStateFlow()

    /** Tracks x5 hold time before fever triggers; not part of UI state. */
    private var x5HoldTimer: Double = 0.0

    val currentTheme: GameTheme
        get() = ThemeCatalog.theme(_state.value.selectedThemeId)

    val currentBackground: BackgroundTheme
        get() = BackgroundCatalog.background(_state.value.selectedBackgroundId)

    init {
        loadPersistedData()
    }

    // MARK: - Game Flow

    fun startGame() {
        x5HoldTimer = 0.0
        _state.update { current ->
            current.copy(
                score = 0,
                lives = MAX_LIVES,
                combo = 0,
                coinsEarnedThisRun = 0,
                finalScore = 0,
                hasUsedRevive = false,
                needsClear = false,
                isFever = false,
                feverTimeRemaining = 0.0,
                sessionId = current.sessionId + 1,
                gameState = GameState.PLAYING,
            )
        }
    }

    fun endGame() {
        val score = _state.value.score
        val runCoins = _state.value.coinsEarnedThisRun + score / 10
        val newHighScore = maxOf(_state.value.highScore, score)

        if (score > _state.value.highScore) {
            persistHighScore(newHighScore)
        }

        endFever()

        _state.update { current ->
            current.copy(
                finalScore = score,
                coinsEarnedThisRun = runCoins,
                highScore = newHighScore,
                bankedCoins = current.bankedCoins + runCoins,
                gameState = GameState.GAME_OVER,
            )
        }
        persistCoins()
    }

    fun revive() {
        if (_state.value.hasUsedRevive) return
        _state.update { current ->
            current.copy(
                lives = 1,
                hasUsedRevive = true,
                needsClear = true,
                gameState = GameState.PLAYING,
            )
        }
    }

    fun returnToMenu() {
        endFever()
        _state.update { it.copy(gameState = GameState.MENU) }
    }

    fun openShop() {
        _state.update { it.copy(gameState = GameState.SHOP) }
    }

    fun acknowledgeNeedsClear() {
        _state.update { it.copy(needsClear = false) }
    }

    // MARK: - Scoring

    /**
     * Awards points and combo-based coins. Returns points and coins earned this tap.
     * During fever mode, points and coins are tripled.
     */
    fun handleCorrectTap(basePoints: Int): TapResult {
        val snapshot = _state.value
        val combo = snapshot.combo + 1
        val mult = comboMultiplierFor(combo)
        val feverMult = if (snapshot.isFever) 3 else 1
        val points = basePoints * mult * feverMult
        val coins = mult * feverMult

        _state.update { current ->
            current.copy(
                combo = combo,
                score = current.score + points,
                coinsEarnedThisRun = current.coinsEarnedThisRun + coins,
            )
        }
        return TapResult(points, coins)
    }

    fun handleWrongTap() {
        _state.update { current ->
            current.copy(
                lives = maxOf(0, current.lives - 1),
                combo = 0,
            )
        }
        endFever()
    }

    fun handleMissedStandard() {
        _state.update { current ->
            current.copy(
                lives = maxOf(0, current.lives - 1),
                combo = 0,
            )
        }
        endFever()
    }

    fun gainLife() {
        _state.update { current ->
            current.copy(lives = minOf(MAX_LIVES, current.lives + 1))
        }
    }

    // MARK: - Fever Mode

    /** Called every frame from the game loop. Tracks x5 hold, triggers fever, counts down timer. */
    fun updateFever(dt: Double) {
        val snapshot = _state.value
        if (snapshot.isFever) {
            val remaining = snapshot.feverTimeRemaining - dt
            if (remaining <= 0) {
                endFever()
            } else {
                _state.update { it.copy(feverTimeRemaining = remaining) }
            }
        } else {
            if (snapshot.comboMultiplier == 5) {
                x5HoldTimer += dt
                if (x5HoldTimer >= 5.0) {
                    startFever()
                }
            } else {
                x5HoldTimer = 0.0
            }
        }
    }

    private fun startFever() {
        _state.update { it.copy(isFever = true, feverTimeRemaining = 8.0) }
    }

    fun endFever() {
        if (!_state.value.isFever) {
            x5HoldTimer = 0.0
            return
        }
        x5HoldTimer = 0.0
        _state.update { it.copy(isFever = false, feverTimeRemaining = 0.0, combo = 20) }
    }

    // MARK: - Skin Shop

    fun purchaseTheme(theme: GameTheme): Boolean {
        val snapshot = _state.value
        if (theme.id in snapshot.unlockedThemeIds || snapshot.totalCoins < theme.price) {
            return false
        }

        var bankedCoins = snapshot.bankedCoins - theme.price
        var coinsEarnedThisRun = snapshot.coinsEarnedThisRun
        if (bankedCoins < 0) {
            coinsEarnedThisRun += bankedCoins
            bankedCoins = 0
        }

        val unlockedThemeIds = snapshot.unlockedThemeIds + theme.id
        _state.update { current ->
            current.copy(
                bankedCoins = bankedCoins,
                coinsEarnedThisRun = coinsEarnedThisRun,
                unlockedThemeIds = unlockedThemeIds,
                selectedThemeId = theme.id,
            )
        }
        persistCoins()
        persistUnlockedThemes(unlockedThemeIds)
        persistSelectedTheme(theme.id)
        return true
    }

    fun selectTheme(theme: GameTheme) {
        if (theme.id !in _state.value.unlockedThemeIds) return
        _state.update { it.copy(selectedThemeId = theme.id) }
        persistSelectedTheme(theme.id)
    }

    // MARK: - Background Shop

    fun purchaseBackground(bg: BackgroundTheme): Boolean {
        val snapshot = _state.value
        if (bg.id in snapshot.unlockedBackgroundIds || snapshot.totalCoins < bg.price) {
            return false
        }

        var bankedCoins = snapshot.bankedCoins - bg.price
        var coinsEarnedThisRun = snapshot.coinsEarnedThisRun
        if (bankedCoins < 0) {
            coinsEarnedThisRun += bankedCoins
            bankedCoins = 0
        }

        val unlockedBackgroundIds = snapshot.unlockedBackgroundIds + bg.id
        _state.update { current ->
            current.copy(
                bankedCoins = bankedCoins,
                coinsEarnedThisRun = coinsEarnedThisRun,
                unlockedBackgroundIds = unlockedBackgroundIds,
                selectedBackgroundId = bg.id,
            )
        }
        persistCoins()
        persistUnlockedBackgrounds(unlockedBackgroundIds)
        persistSelectedBackground(bg.id)
        return true
    }

    fun selectBackground(bg: BackgroundTheme) {
        if (bg.id !in _state.value.unlockedBackgroundIds) return
        _state.update { it.copy(selectedBackgroundId = bg.id) }
        persistSelectedBackground(bg.id)
    }

    // MARK: - Daily Reward

    fun claimDailyReward(): Int {
        if (!_state.value.canClaimDailyReward) return 0

        var dailyStreak = _state.value.dailyStreak
        val lastClaim = _state.value.lastDailyClaimEpochMillis
        if (lastClaim != null && System.currentTimeMillis() - lastClaim >= 48 * 3_600 * 1_000L) {
            dailyStreak = 0
        }
        dailyStreak += 1
        if (dailyStreak > 7) dailyStreak = 1

        val reward = DAILY_REWARDS[dailyStreak - 1]
        val now = System.currentTimeMillis()

        _state.update { current ->
            current.copy(
                dailyStreak = dailyStreak,
                bankedCoins = current.bankedCoins + reward,
                lastDailyClaimEpochMillis = now,
            )
        }
        persistDailyReward(dailyStreak, now)
        persistCoins()
        return reward
    }

    fun doubleDailyReward() {
        if (_state.value.isWatchingAd) return
        val reward = DAILY_REWARDS[maxOf(0, _state.value.dailyStreak - 1)]
        _state.update { it.copy(isWatchingAd = true) }
        adManager.showRewardedAd(
            onReward = {
                _state.update { it.copy(bankedCoins = it.bankedCoins + reward) }
                persistCoins()
            },
            onDismiss = { _state.update { it.copy(isWatchingAd = false) } },
            onFail = { _state.update { it.copy(isWatchingAd = false) } },
        )
    }

    // MARK: - Rewarded Ads

    fun watchAdForCoins() {
        if (_state.value.isWatchingAd) return
        _state.update { it.copy(isWatchingAd = true) }
        adManager.showRewardedAd(
            onReward = {
                _state.update { it.copy(bankedCoins = it.bankedCoins + 50) }
                persistCoins()
            },
            onDismiss = { _state.update { it.copy(isWatchingAd = false) } },
            onFail = { _state.update { it.copy(isWatchingAd = false) } },
        )
    }

    fun watchAdForRevive() {
        if (_state.value.isWatchingAd || _state.value.hasUsedRevive) return
        _state.update { it.copy(isWatchingAd = true) }
        adManager.showRewardedAd(
            onReward = { revive() },
            onDismiss = { _state.update { it.copy(isWatchingAd = false) } },
            onFail = { _state.update { it.copy(isWatchingAd = false) } },
        )
    }

    // MARK: - Persistence

    private fun loadPersistedData() {
        val highScore = preferences.getInt(KEY_HIGH_SCORE, 0)
        val bankedCoins = preferences.getInt(KEY_COINS, 0)
        val isMuted = preferences.getBoolean(KEY_MUTED, false)

        val selectedThemeId = preferences.getString(KEY_SELECTED_THEME, null)
            ?: preferences.getString(KEY_SELECTED_SKIN, null)
            ?: "neon_circle"

        val themeKeys = preferences.getStringSet(KEY_UNLOCKED_THEMES, null)
            ?: preferences.getStringSet(KEY_UNLOCKED_SKINS, null)
            ?: emptySet()
        val unlockedThemeIds = themeKeys.toMutableSet().apply { add("neon_circle") }

        val selectedBackgroundId = preferences.getString(KEY_SELECTED_BG, null) ?: "deep_space"
        val unlockedBackgroundIds = (
            preferences.getStringSet(KEY_UNLOCKED_BGS, emptySet()) ?: emptySet()
            ).toMutableSet().apply { add("deep_space") }

        val dailyStreak = preferences.getInt(KEY_DAILY_STREAK, 0)
        val lastDailyClaim = preferences.getLong(KEY_DAILY_LAST, -1L).let { value ->
            if (value >= 0L) value else null
        }

        _state.value = GameManagerState(
            highScore = highScore,
            bankedCoins = bankedCoins,
            isMuted = isMuted,
            selectedThemeId = selectedThemeId,
            unlockedThemeIds = unlockedThemeIds,
            selectedBackgroundId = selectedBackgroundId,
            unlockedBackgroundIds = unlockedBackgroundIds,
            dailyStreak = dailyStreak,
            lastDailyClaimEpochMillis = lastDailyClaim,
        )
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
        preferences.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    private fun persistHighScore(highScore: Int) {
        preferences.edit().putInt(KEY_HIGH_SCORE, highScore).apply()
    }

    private fun persistCoins() {
        preferences.edit().putInt(KEY_COINS, _state.value.bankedCoins).apply()
    }

    private fun persistSelectedTheme(themeId: String) {
        preferences.edit().putString(KEY_SELECTED_THEME, themeId).apply()
    }

    private fun persistUnlockedThemes(themeIds: Set<String>) {
        preferences.edit().putStringSet(KEY_UNLOCKED_THEMES, themeIds).apply()
    }

    private fun persistSelectedBackground(backgroundId: String) {
        preferences.edit().putString(KEY_SELECTED_BG, backgroundId).apply()
    }

    private fun persistUnlockedBackgrounds(backgroundIds: Set<String>) {
        preferences.edit().putStringSet(KEY_UNLOCKED_BGS, backgroundIds).apply()
    }

    private fun persistDailyReward(streak: Int, lastClaimEpochMillis: Long) {
        preferences.edit()
            .putInt(KEY_DAILY_STREAK, streak)
            .putLong(KEY_DAILY_LAST, lastClaimEpochMillis)
            .apply()
    }

    companion object {
        const val MAX_LIVES = 3

        private val DAILY_REWARDS = listOf(100, 200, 300, 400, 500, 750, 1000)

        private const val KEY_HIGH_SCORE = "tt_highscore"
        private const val KEY_COINS = "tt_coins"
        private const val KEY_MUTED = "tt_muted"
        private const val KEY_SELECTED_THEME = "tt_selected_theme"
        private const val KEY_SELECTED_SKIN = "tt_selected_skin"
        private const val KEY_UNLOCKED_THEMES = "tt_unlocked_themes"
        private const val KEY_UNLOCKED_SKINS = "tt_unlocked_skins"
        private const val KEY_SELECTED_BG = "tt_selected_bg"
        private const val KEY_UNLOCKED_BGS = "tt_unlocked_bgs"
        private const val KEY_DAILY_STREAK = "tt_daily_streak"
        private const val KEY_DAILY_LAST = "tt_daily_last"

        private fun comboMultiplierFor(combo: Int): Int = when (combo) {
            in 0..4 -> 1
            in 5..9 -> 2
            in 10..19 -> 3
            in 20..29 -> 4
            else -> 5
        }
    }
}

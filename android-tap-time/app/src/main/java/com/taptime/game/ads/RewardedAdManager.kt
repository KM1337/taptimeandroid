package com.taptime.game.ads

/**
 * Android counterpart to iOS [AdManager]. Inject a concrete implementation
 * (e.g. Google AdMob) when constructing [com.taptime.game.GameManager].
 */
interface RewardedAdManager {
    fun showRewardedAd(
        onReward: () -> Unit,
        onDismiss: () -> Unit,
        onFail: () -> Unit,
    )
}

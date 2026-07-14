package com.taptime.game.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Google AdMob implementation of [RewardedAdManager].
 * Uses Google's test ad unit ID in debug; replace for production.
 */
class AdMobRewardedAdManager(
    private val activity: Activity,
) : RewardedAdManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    private var rewardCallback: (() -> Unit)? = null
    private var dismissCallback: (() -> Unit)? = null
    private var failCallback: (() -> Unit)? = null

    fun configure() {
        MobileAds.initialize(activity)
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        if (isLoading) return
        isLoading = true

        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            },
        )
    }

    override fun showRewardedAd(
        onReward: () -> Unit,
        onDismiss: () -> Unit,
        onFail: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewardedAd()
            onFail()
            return
        }

        rewardCallback = onReward
        dismissCallback = onDismiss
        failCallback = onFail

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                dismissCallback?.invoke()
                rewardCallback = null
                dismissCallback = null
                failCallback = null
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                failCallback?.invoke()
                rewardCallback = null
                dismissCallback = null
                failCallback = null
                loadRewardedAd()
            }
        }

        ad.show(activity) {
            rewardCallback?.invoke()
            rewardCallback = null
        }
    }

    companion object {
        // Google test rewarded ad unit — replace with your production unit ID.
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}

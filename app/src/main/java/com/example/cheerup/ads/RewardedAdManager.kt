package com.example.cheerup.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cheerup.privacy.ConsentManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import java.lang.ref.WeakReference

sealed class RewardedAdState {
    data object Idle : RewardedAdState()
    data object Loading : RewardedAdState()
    data object Ready : RewardedAdState()
    data object Presenting : RewardedAdState()
    data class Failed(val message: String) : RewardedAdState()

    val isBusy: Boolean
        get() = this == Loading || this == Presenting
}

class RewardedAdManager private constructor() {

    var arcadeState: RewardedAdState by mutableStateOf(RewardedAdState.Idle)
        private set

    var wheelsState: RewardedAdState by mutableStateOf(RewardedAdState.Idle)
        private set

    var isPresentingAd: Boolean by mutableStateOf(false)
        private set

    var state: RewardedAdState
        get() = arcadeState
        internal set(value) {
            arcadeState = value
        }

    private var hasEarnedRewardForCurrentAd: Boolean = false
    private var isSDKStarted: Boolean = false
    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    private var onRewardEarnedHandler: (() -> Unit)? = null
    private var onDismissedWithoutRewardHandler: (() -> Unit)? = null

    private var arcadeRewardedAd: RewardedAd? = null
    private var wheelsRewardedInterstitialAd: RewardedInterstitialAd? = null
    private var wheelsFallbackRewardedAd: RewardedAd? = null

    fun attachActivity(activity: Activity) {
        appContext = activity.applicationContext
        currentActivityRef = WeakReference(activity)
    }

    fun detachActivity(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    // MARK: - SDK Initialization

    fun startSDKIfNeeded(context: Context? = appContext) {
        if (isSDKStarted) return
        val ctx = context?.applicationContext ?: return
        appContext = ctx
        isSDKStarted = true
        try {
            MobileAds.initialize(ctx) {}
        } catch (_: Throwable) {
            // Ignored in JVM unit test environment
        }
    }

    // MARK: - Preload (Never auto-presents)

    fun preloadAllIfNeeded(context: Context? = appContext) {
        preloadArcadeAdIfNeeded(context)
        preloadWheelsAdIfNeeded(context)
    }

    fun preloadIfNeeded() {
        preloadArcadeAdIfNeeded()
    }

    /** Preloads arcade rewarded ad (Tap, Rush, Gravity, Ping Pong) */
    fun preloadArcadeAdIfNeeded(context: Context? = appContext) {
        startSDKIfNeeded(context)
        if (isPresentingAd) return
        if (arcadeState == RewardedAdState.Loading || arcadeState == RewardedAdState.Ready) return

        val ctx = context?.applicationContext ?: appContext ?: return
        arcadeState = RewardedAdState.Loading

        runOnMain {
            try {
                val request = ConsentManager.shared.buildAdRequest(ctx)
                RewardedAd.load(
                    ctx,
                    AdMobConfig.activeArcadeRewardedUnitID,
                    request,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            arcadeRewardedAd = ad
                            arcadeState = RewardedAdState.Ready
                        }

                        override fun onAdFailedToLoad(loadError: LoadAdError) {
                            arcadeRewardedAd = null
                            arcadeState = RewardedAdState.Failed(loadError.message)
                        }
                    }
                )
            } catch (t: Throwable) {
                arcadeRewardedAd = null
                arcadeState = RewardedAdState.Failed(t.message ?: "Failed to load ad")
            }
        }
    }

    /** Preloads Wheels ad (Every 5 Spins & Chess end of session) with RewardedAd fallback */
    fun preloadWheelsAdIfNeeded(context: Context? = appContext) {
        startSDKIfNeeded(context)
        if (isPresentingAd) return
        if (wheelsState == RewardedAdState.Loading || wheelsState == RewardedAdState.Ready) return

        val ctx = context?.applicationContext ?: appContext ?: return
        wheelsState = RewardedAdState.Loading

        runOnMain {
            try {
                val request = ConsentManager.shared.buildAdRequest(ctx)
                RewardedInterstitialAd.load(
                    ctx,
                    AdMobConfig.activeWheelsUnitID,
                    request,
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            wheelsRewardedInterstitialAd = ad
                            wheelsFallbackRewardedAd = null
                            wheelsState = RewardedAdState.Ready
                        }

                        override fun onAdFailedToLoad(loadError: LoadAdError) {
                            // Fallback to standard RewardedAd if unit was created with Rewarded format
                            RewardedAd.load(
                                ctx,
                                AdMobConfig.activeWheelsUnitID,
                                ConsentManager.shared.buildAdRequest(ctx),
                                object : RewardedAdLoadCallback() {
                                    override fun onAdLoaded(fallbackAd: RewardedAd) {
                                        wheelsRewardedInterstitialAd = null
                                        wheelsFallbackRewardedAd = fallbackAd
                                        wheelsState = RewardedAdState.Ready
                                    }

                                    override fun onAdFailedToLoad(fallbackError: LoadAdError) {
                                        wheelsRewardedInterstitialAd = null
                                        wheelsFallbackRewardedAd = null
                                        wheelsState = RewardedAdState.Failed(fallbackError.message)
                                    }
                                }
                            )
                        }
                    }
                )
            } catch (t: Throwable) {
                wheelsRewardedInterstitialAd = null
                wheelsFallbackRewardedAd = null
                wheelsState = RewardedAdState.Failed(t.message ?: "Failed to load Wheels ad")
            }
        }
    }

    // MARK: - Arcade Games Ad Presentation (Tap, Rush, Gravity, Ping Pong)

    fun presentRewardedAd(
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: (() -> Unit)? = null
    ) {
        if (isPresentingAd || arcadeState == RewardedAdState.Presenting) return
        startSDKIfNeeded()

        val loadedAd = arcadeRewardedAd
        if (loadedAd != null) {
            presentLoadedArcadeAd(loadedAd, onRewardEarned, onDismissedWithoutReward)
            return
        }

        val ctx = appContext
        val activity = currentActivityRef?.get()
        if (ctx == null || activity == null) {
            arcadeState = RewardedAdState.Failed("Unable to find active window.")
            onDismissedWithoutReward?.invoke()
            return
        }

        if (arcadeState == RewardedAdState.Loading) return
        arcadeState = RewardedAdState.Loading

        runOnMain {
            try {
                RewardedAd.load(
                    ctx,
                    AdMobConfig.activeArcadeRewardedUnitID,
                    ConsentManager.shared.buildAdRequest(ctx),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            arcadeRewardedAd = ad
                            presentLoadedArcadeAd(ad, onRewardEarned, onDismissedWithoutReward)
                        }

                        override fun onAdFailedToLoad(loadError: LoadAdError) {
                            arcadeRewardedAd = null
                            arcadeState = RewardedAdState.Failed(loadError.message)
                            onDismissedWithoutReward?.invoke()
                        }
                    }
                )
            } catch (t: Throwable) {
                arcadeRewardedAd = null
                arcadeState = RewardedAdState.Failed(t.message ?: "Failed to load ad")
                onDismissedWithoutReward?.invoke()
            }
        }
    }

    // MARK: - Wheels Section Ad Presentation (Every 5 Spins & Chess End of Session)

    fun presentWheelsAd(
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: (() -> Unit)? = null
    ) {
        if (isPresentingAd || wheelsState == RewardedAdState.Presenting) return
        startSDKIfNeeded()

        wheelsRewardedInterstitialAd?.let { ad ->
            presentLoadedWheelsInterstitialAd(ad, onRewardEarned, onDismissedWithoutReward)
            return
        }

        wheelsFallbackRewardedAd?.let { ad ->
            presentLoadedWheelsRewardedAd(ad, onRewardEarned, onDismissedWithoutReward)
            return
        }

        val ctx = appContext
        val activity = currentActivityRef?.get()
        if (ctx == null || activity == null) {
            wheelsState = RewardedAdState.Failed("Unable to find active window.")
            onDismissedWithoutReward?.invoke()
            return
        }

        if (wheelsState == RewardedAdState.Loading) return
        wheelsState = RewardedAdState.Loading

        runOnMain {
            try {
                RewardedInterstitialAd.load(
                    ctx,
                    AdMobConfig.activeWheelsUnitID,
                    ConsentManager.shared.buildAdRequest(ctx),
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            wheelsRewardedInterstitialAd = ad
                            presentLoadedWheelsInterstitialAd(ad, onRewardEarned, onDismissedWithoutReward)
                        }

                        override fun onAdFailedToLoad(loadError: LoadAdError) {
                            // Fallback to standard RewardedAd
                            RewardedAd.load(
                                ctx,
                                AdMobConfig.activeWheelsUnitID,
                                ConsentManager.shared.buildAdRequest(ctx),
                                object : RewardedAdLoadCallback() {
                                    override fun onAdLoaded(fallbackAd: RewardedAd) {
                                        wheelsFallbackRewardedAd = fallbackAd
                                        presentLoadedWheelsRewardedAd(fallbackAd, onRewardEarned, onDismissedWithoutReward)
                                    }

                                    override fun onAdFailedToLoad(fallbackError: LoadAdError) {
                                        wheelsState = RewardedAdState.Failed(fallbackError.message)
                                        onDismissedWithoutReward?.invoke()
                                    }
                                }
                            )
                        }
                    }
                )
            } catch (t: Throwable) {
                wheelsState = RewardedAdState.Failed(t.message ?: "Failed to load Wheels ad")
                onDismissedWithoutReward?.invoke()
            }
        }
    }

    private fun presentLoadedArcadeAd(
        ad: RewardedAd,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: (() -> Unit)?
    ) {
        val activity = currentActivityRef?.get()
        if (activity == null || activity.isFinishing) {
            arcadeState = RewardedAdState.Failed("Unable to find active window.")
            onDismissedWithoutReward?.invoke()
            return
        }

        isPresentingAd = true
        hasEarnedRewardForCurrentAd = false
        onRewardEarnedHandler = onRewardEarned
        onDismissedWithoutRewardHandler = onDismissedWithoutReward
        arcadeState = RewardedAdState.Presenting

        ad.fullScreenContentCallback = fullScreenCallback
        ad.show(activity) {
            runOnMain {
                markRewardEarned()
            }
        }
    }

    private fun presentLoadedWheelsInterstitialAd(
        ad: RewardedInterstitialAd,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: (() -> Unit)?
    ) {
        val activity = currentActivityRef?.get()
        if (activity == null || activity.isFinishing) {
            wheelsState = RewardedAdState.Failed("Unable to find active window.")
            onDismissedWithoutReward?.invoke()
            return
        }

        isPresentingAd = true
        hasEarnedRewardForCurrentAd = false
        onRewardEarnedHandler = onRewardEarned
        onDismissedWithoutRewardHandler = onDismissedWithoutReward
        wheelsState = RewardedAdState.Presenting

        ad.fullScreenContentCallback = fullScreenCallback
        ad.show(activity) {
            runOnMain {
                markRewardEarned()
            }
        }
    }

    private fun presentLoadedWheelsRewardedAd(
        ad: RewardedAd,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: (() -> Unit)?
    ) {
        val activity = currentActivityRef?.get()
        if (activity == null || activity.isFinishing) {
            wheelsState = RewardedAdState.Failed("Unable to find active window.")
            onDismissedWithoutReward?.invoke()
            return
        }

        isPresentingAd = true
        hasEarnedRewardForCurrentAd = false
        onRewardEarnedHandler = onRewardEarned
        onDismissedWithoutRewardHandler = onDismissedWithoutReward
        wheelsState = RewardedAdState.Presenting

        ad.fullScreenContentCallback = fullScreenCallback
        ad.show(activity) {
            runOnMain {
                markRewardEarned()
            }
        }
    }

    private val fullScreenCallback = object : FullScreenContentCallback() {
        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            runOnMain {
                completePresentation(didFailToPresent = true, errorMessage = adError.message)
            }
        }

        override fun onAdDismissedFullScreenContent() {
            runOnMain {
                completePresentation(didFailToPresent = false)
            }
        }
    }

    // MARK: - Reward & Lifecycle State Helpers (Internal for Unit Testing, matching iOS)

    fun markRewardEarned(): Boolean {
        if (!isPresentingAd || hasEarnedRewardForCurrentAd) {
            return false
        }
        hasEarnedRewardForCurrentAd = true
        return true
    }

    fun completePresentation(didFailToPresent: Boolean = false, errorMessage: String? = null) {
        val earned = !didFailToPresent && hasEarnedRewardForCurrentAd
        val rewardHandler = onRewardEarnedHandler
        val dismissHandler = onDismissedWithoutRewardHandler

        isPresentingAd = false
        hasEarnedRewardForCurrentAd = false
        onRewardEarnedHandler = null
        onDismissedWithoutRewardHandler = null

        arcadeRewardedAd = null
        wheelsRewardedInterstitialAd = null
        wheelsFallbackRewardedAd = null

        if (didFailToPresent) {
            val msg = errorMessage ?: "Ad failed to present."
            arcadeState = RewardedAdState.Failed(msg)
            wheelsState = RewardedAdState.Failed(msg)
            dismissHandler?.invoke()
        } else {
            arcadeState = RewardedAdState.Idle
            wheelsState = RewardedAdState.Idle
            if (earned) {
                rewardHandler?.invoke()
            } else {
                dismissHandler?.invoke()
            }
        }
    }

    fun beginTestPresentation(
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: (() -> Unit)? = null
    ): Boolean {
        if (isPresentingAd) return false
        isPresentingAd = true
        hasEarnedRewardForCurrentAd = false
        arcadeState = RewardedAdState.Presenting
        wheelsState = RewardedAdState.Presenting
        onRewardEarnedHandler = onRewardEarned
        onDismissedWithoutRewardHandler = onDismissedWithoutReward
        return true
    }

    private fun runOnMain(block: () -> Unit) {
        try {
            val mainLooper = Looper.getMainLooper()
            if (mainLooper != null && Looper.myLooper() == mainLooper) {
                block()
            } else if (mainLooper != null) {
                Handler(mainLooper).post(block)
            } else {
                block()
            }
        } catch (_: Throwable) {
            block()
        }
    }

    companion object {
        val shared: RewardedAdManager by lazy { RewardedAdManager() }
    }
}

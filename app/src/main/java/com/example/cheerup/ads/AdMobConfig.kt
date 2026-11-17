package com.example.cheerup.ads

import com.example.cheerup.BuildConfig

object AdMobConfig {
    /**
     * Android AdMob Application ID provided via Gradle configuration (`gradle.properties` -> `BuildConfig` / `manifestPlaceholders`).
     */
    val appId: String
        get() = BuildConfig.ADMOB_APP_ID

    // MARK: - Arcade Games (Tap, Rush, Gravity, Ping Pong) — Rewarded Video Ad
    /** Production Android Rewarded Ad Unit ID for Arcade game-over continuation (from BuildConfig). */
    val arcadeRewardedAdUnitID: String
        get() = BuildConfig.ADMOB_ARCADE_REWARDED_UNIT_ID

    /** Official Google Android Test Rewarded Ad Unit ID. */
    const val testArcadeRewardedAdUnitID: String = "ca-app-pub-3940256099942544/5224354917"

    // MARK: - Wheels Section (Every 5 Spins) & Chess End-of-Session Ad
    /** Production Android Ad Unit ID for Wheels & Chess (from BuildConfig). */
    val wheelsAdUnitID: String
        get() = BuildConfig.ADMOB_WHEELS_UNIT_ID

    /** Official Google Android Test Rewarded Interstitial Ad Unit ID. */
    const val testWheelsAdUnitID: String = "ca-app-pub-3940256099942544/5354046379"

    // MARK: - Adaptive Banner Ad Unit ID (from BuildConfig, defaults to Google's official test Adaptive Banner ID)
    val bannerAdUnitID: String
        get() = BuildConfig.ADMOB_BANNER_UNIT_ID

    const val testBannerAdUnitID: String = "ca-app-pub-3940256099942544/9214589741"

    val activeArcadeRewardedUnitID: String
        get() = if (BuildConfig.USE_TEST_ADS) testArcadeRewardedAdUnitID else arcadeRewardedAdUnitID

    val activeWheelsUnitID: String
        get() = if (BuildConfig.USE_TEST_ADS) testWheelsAdUnitID else wheelsAdUnitID

    val activeBannerUnitID: String
        get() = if (BuildConfig.USE_TEST_ADS) testBannerAdUnitID else bannerAdUnitID

    // Backward compatibility aliases matching iOS AdMobConfig
    val rewardedAdUnitID: String get() = arcadeRewardedAdUnitID
    val productionRewardedAdUnitID: String get() = arcadeRewardedAdUnitID
    val testRewardedAdUnitID: String get() = testArcadeRewardedAdUnitID
}

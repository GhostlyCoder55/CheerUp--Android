package com.example.cheerup.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cheerup.privacy.ConsentManager
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Reusable Anchored Adaptive Banner Composable following official Google AdMob Android Banner guidelines.
 * Note: CheerUp! iOS uses Rewarded & Rewarded Interstitial ads exclusively (no banner overlays during gameplay),
 * so this component is provided for optional non-gameplay placement if ever enabled.
 */
@Composable
fun AdaptiveBannerAdView(
    adUnitId: String = AdMobConfig.testBannerAdUnitID,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val adWidth = configuration.screenWidthDp.coerceAtLeast(320)

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth))
                loadAd(ConsentManager.shared.buildAdRequest(context))
            }
        }
    )
}

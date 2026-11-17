package com.example.cheerup

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.leaderboard.PlayGamesManager
import com.example.cheerup.notifications.NotificationManager
import com.example.cheerup.presentation.navigation.CheerUpRootScreen
import com.example.cheerup.privacy.ConsentManager
import com.example.cheerup.ui.theme.CheerUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Single, unified CheerUp! Launch Screen (Android 12+/13+ system splash screen)
        val launchStartMs = SystemClock.uptimeMillis()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
            val icon = splashScreenView.iconView
            if (icon != null) {
                val scaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.08f)
                val scaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.08f)
                AnimatorSet().apply {
                    playTogether(fadeOut, scaleX, scaleY)
                    duration = 420L
                    interpolator = DecelerateInterpolator()
                    doOnEnd { splashScreenView.remove() }
                    start()
                }
            } else {
                fadeOut.apply {
                    duration = 380L
                    interpolator = DecelerateInterpolator()
                    doOnEnd { splashScreenView.remove() }
                    start()
                }
            }
        }

        // Initialize core singletons with ApplicationContext
        LanguageManager.shared.initialize(applicationContext)
        ScoreManager.shared.initialize(applicationContext)
        AudioManager.shared.initialize(applicationContext)
        HapticsManager.shared.initialize(applicationContext)
        NotificationManager.shared.initialize(applicationContext)
        PlayGamesManager.shared.initialize(applicationContext)

        RewardedAdManager.shared.attachActivity(this)
        PlayGamesManager.shared.attachActivity(this)

        // Gather UMP consent (GDPR/CCPA/UK) before initializing Google Mobile Ads SDK
        ConsentManager.shared.gatherConsent(this) {
            if (ConsentManager.shared.canRequestAds) {
                RewardedAdManager.shared.startSDKIfNeeded(applicationContext)
                RewardedAdManager.shared.preloadAllIfNeeded(applicationContext)
            }
        }

        // Authenticate Google Play Games Services v2 silently
        PlayGamesManager.shared.authenticateLocalPlayer(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            CheerUpTheme {
                CheerUpRootScreen()
            }
        }

        // Hold the single launch screen cleanly for ~900ms while Compose renders its first frame behind it
        val contentView: View = findViewById(android.R.id.content)
        contentView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (SystemClock.uptimeMillis() - launchStartMs >= 900L) {
                        contentView.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        RewardedAdManager.shared.attachActivity(this)
        PlayGamesManager.shared.attachActivity(this)
    }

    override fun onDestroy() {
        RewardedAdManager.shared.detachActivity(this)
        PlayGamesManager.shared.detachActivity(this)
        super.onDestroy()
    }
}

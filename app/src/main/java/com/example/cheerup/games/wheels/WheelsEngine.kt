package com.example.cheerup.games.wheels

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.MascotMood
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class WheelsEngine(context: Context? = null) {

    // Wheel Motion
    var rotation: Double by mutableDoubleStateOf(0.0)
    var pointerDeflection: Double by mutableDoubleStateOf(0.0)
    var isSpinning: Boolean by mutableStateOf(false)

    // Mascot & Dialogue
    var mascotMood: MascotMood by mutableStateOf(MascotMood.HAPPY)
    var speechText: String by mutableStateOf(WheelsData.idleLines.random())

    // Result & Interactive States
    var selectedOutcome: WheelOutcome? by mutableStateOf(null)
    var showResultSheet: Boolean by mutableStateOf(false)
    var countdownRemaining: Int by mutableIntStateOf(0)
    var isTimerActive: Boolean by mutableStateOf(false)
    var isFlipped: Boolean by mutableStateOf(false)

    // Confetti Particles
    var particles: List<WheelsConfettiParticle> by mutableStateOf(emptyList())
    var frameTick: Long by mutableStateOf(0L)

    // Spin Tracking (Non-skip video ad triggers every 5 spins)
    var spinCount: Int by mutableIntStateOf(0)
        private set

    private var prefs: SharedPreferences? = null

    private var spinDuration: Double = 4.2
    private var spinElapsedTime: Double = 0.0
    private var startAngle: Double = 0.0
    private var targetAngle: Double = 0.0
    private var lastTickSegment: Int = -1
    private var timerCountdownInterval: Double = 0.0
    private var lastFrameNanos: Long? = null

    init {
        if (context != null) {
            val sp = context.applicationContext.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sp
            spinCount = sp.getInt(SPIN_COUNT_KEY, 0)
        }
    }

    fun resetSpinCount() {
        spinCount = 0
        prefs?.edit()?.putInt(SPIN_COUNT_KEY, 0)?.apply()
    }

    fun spin() {
        if (isSpinning) return

        RewardedAdManager.shared.preloadWheelsAdIfNeeded()

        showResultSheet = false
        isFlipped = false
        isTimerActive = false
        countdownRemaining = 0
        lastFrameNanos = null

        val outcome = pickOutcome()
        selectedOutcome = outcome

        val targetSegmentIndex = Random.nextInt(0, 12)
        val segmentCenter = (targetSegmentIndex + 0.5) * 30.0
        val targetModulo = (270.0 - segmentCenter) % 360.0
        val targetBase = if (targetModulo < 0) targetModulo + 360.0 else targetModulo

        val jitter = Random.nextDouble(-8.0, 8.0)
        val fullRotations = Random.nextInt(5, 8) * 360.0
        val currentNorm = rotation % 360.0
        val currentPos = if (currentNorm < 0) currentNorm + 360.0 else currentNorm
        val angleDiff = (targetBase + jitter - currentPos) % 360.0
        val normalizedDiff = if (angleDiff < 0) angleDiff + 360.0 else angleDiff

        startAngle = rotation
        targetAngle = startAngle + fullRotations + normalizedDiff

        isSpinning = true
        spinElapsedTime = 0.0
        spinDuration = 4.2
        lastTickSegment = -1

        mascotMood = MascotMood.WOW
        speechText = WheelsData.spinLines.random()
        HapticsManager.shared.mediumImpact()
    }

    fun updateFrameNanos(frameTimeNanos: Long) {
        val dt = lastFrameNanos?.let { prev ->
            ((frameTimeNanos - prev) / 1_000_000_000.0).coerceIn(0.0, 0.1)
        } ?: (1.0 / 60.0)
        lastFrameNanos = frameTimeNanos
        tick(dt)
    }

    fun tick(dt: Double) {
        if (isSpinning) {
            spinElapsedTime += dt
            val progress = min(spinElapsedTime / spinDuration, 1.0)

            // Quartic ease-out deceleration: 1 - (1 - p)^4
            val eased = 1.0 - (1.0 - progress).pow(4.0)
            val current = startAngle + (targetAngle - startAngle) * eased
            rotation = current

            val positiveAngle = (270.0 - current) % 360.0
            val normalized = if (positiveAngle < 0) positiveAngle + 360.0 else positiveAngle
            val currentSegment = (floor(normalized / 30.0).toInt()) % 12

            if (currentSegment != lastTickSegment) {
                lastTickSegment = currentSegment
                if (progress < 0.985) {
                    AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                    HapticsManager.shared.lightTap()
                    pointerDeflection = -20.0
                    scheduleAfter(65L) {
                        pointerDeflection = 0.0
                    }
                }
            }

            if (progress >= 1.0) {
                isSpinning = false
                pointerDeflection = 0.0
                finishSpin()
            }
        }

        if (particles.isNotEmpty()) {
            val updated = particles.toMutableList()
            for (p in updated) {
                p.vy += 12.0f * dt.toFloat()
                p.x += p.vx * dt.toFloat()
                p.y += p.vy * dt.toFloat()
                p.rotation += p.vr * dt
                p.opacity = max(0.0, p.opacity - 0.22 * dt)
            }
            updated.removeAll { it.y > 1000f || it.opacity <= 0.05 }
            particles = updated
            frameTick += 1
        }

        if (isTimerActive && countdownRemaining > 0) {
            timerCountdownInterval += dt
            if (timerCountdownInterval >= 1.0) {
                timerCountdownInterval = 0.0
                countdownRemaining -= 1
                if (countdownRemaining > 0) {
                    AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                    HapticsManager.shared.lightTap()
                } else {
                    isTimerActive = false
                    AudioManager.shared.play(AudioManager.SoundEffect.SUCCESS_TRUMPET)
                    HapticsManager.shared.success()
                }
            }
        }
    }

    private fun finishSpin() {
        val outcome = selectedOutcome ?: return

        spinCount += 1
        prefs?.edit()?.putInt(SPIN_COUNT_KEY, spinCount)?.apply()
        val shouldShowAd = (spinCount % 5 == 0)

        mascotMood = outcome.mood
        when (outcome.type) {
            WheelOutcomeType.HAPPY -> {
                speechText = WheelsData.happyLines.random()
                HapticsManager.shared.success()
                spawnConfetti(70)
            }
            WheelOutcomeType.CHAOS -> {
                speechText = WheelsData.chaosLines.random()
                AudioManager.shared.play(AudioManager.SoundEffect.POP)
                HapticsManager.shared.heavyImpact()
                if (outcome.titleEn == "Upside Down") {
                    isFlipped = true
                    scheduleAfter(4000L) {
                        isFlipped = false
                    }
                }
                spawnConfetti(35)
            }
        }

        if (outcome.timerSeconds != null) {
            countdownRemaining = outcome.timerSeconds
            isTimerActive = !shouldShowAd
            timerCountdownInterval = 0.0
        }

        scheduleAfter(200L) {
            showResultSheet = true

            if (shouldShowAd) {
                scheduleAfter(1200L) {
                    RewardedAdManager.shared.presentWheelsAd(
                        onRewardEarned = {
                            showResultSheet = true
                            if (outcome.timerSeconds != null && countdownRemaining > 0) {
                                isTimerActive = true
                            }
                            RewardedAdManager.shared.preloadWheelsAdIfNeeded()
                        },
                        onDismissedWithoutReward = {
                            showResultSheet = true
                            if (outcome.timerSeconds != null && countdownRemaining > 0) {
                                isTimerActive = true
                            }
                            RewardedAdManager.shared.preloadWheelsAdIfNeeded()
                        }
                    )
                }
            }
        }
    }

    fun spinAgain() {
        showResultSheet = false
        isTimerActive = false
        scheduleAfter(200L) {
            spin()
        }
    }

    private fun spawnConfetti(count: Int) {
        val colors = WheelsData.segments.map { it.color } + listOf(Color.White, Color(0xFFFFD84A))
        val newParticles = ArrayList<WheelsConfettiParticle>(count)
        for (i in 0 until count) {
            newParticles.add(
                WheelsConfettiParticle(
                    x = Random.nextDouble(40.0, 350.0).toFloat(),
                    y = Random.nextDouble(180.0, 320.0).toFloat(),
                    vx = Random.nextDouble(-220.0, 220.0).toFloat(),
                    vy = Random.nextDouble(-420.0, -120.0).toFloat(),
                    rotation = Random.nextDouble(0.0, 360.0),
                    vr = Random.nextDouble(-8.0, 8.0),
                    size = Random.nextDouble(7.0, 14.0).toFloat(),
                    color = colors.random(),
                    opacity = 1.0
                )
            )
        }
        particles = particles + newParticles
        frameTick += 1
    }

    private fun pickOutcome(): WheelOutcome {
        val roll = Random.nextInt(1, 101)
        val category = if (roll <= 50) WheelOutcomeType.HAPPY else WheelOutcomeType.CHAOS
        val candidates = WheelsData.outcomes.filter { it.type == category }
        return candidates.randomOrNull() ?: WheelsData.outcomes[0]
    }

    private fun scheduleAfter(delayMs: Long, action: () -> Unit) {
        try {
            val looper = Looper.getMainLooper()
            if (looper != null) {
                Handler(looper).postDelayed(action, delayMs)
            } else {
                action()
            }
        } catch (_: Throwable) {
            action()
        }
    }

    companion object {
        const val SPIN_COUNT_KEY = "cheerup.wheels.spinCount"
    }
}

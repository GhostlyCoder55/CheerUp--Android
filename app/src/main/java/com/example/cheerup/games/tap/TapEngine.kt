package com.example.cheerup.games.tap

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.AppLanguage
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.leaderboard.PlayGamesManager
import com.example.cheerup.presentation.theme.CheerUpColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class TapEngine {

    var lives: Int by mutableIntStateOf(3)
    var round: Int by mutableIntStateOf(0)
    var streak: Int by mutableIntStateOf(0)
    var highScore: Int by mutableIntStateOf(ScoreManager.shared.highScore(GameIdentifier.TAP))
    var isOver: Boolean by mutableStateOf(false)
    var isLive: Boolean by mutableStateOf(false)
    var hasUsedRewardedContinue: Boolean by mutableStateOf(false)

    private var hasSubmittedScoreForCurrentEnding: Boolean = false
    private var sessionGeneration: Int = 0

    // Mascot & Dialogue
    var mascotMood: MascotMood by mutableStateOf(MascotMood.HAPPY)
    var speechText: String by mutableStateOf("Hi! I'm Cheery. Don't let me down.")

    // Instruction Card
    var instructionPrefix: String by mutableStateOf("Ready?")
    var instructionWord: String by mutableStateOf("")
    var instructionColor: Color by mutableStateOf(Color.White)
    var instructionTag: String by mutableStateOf("")
    var isPsychAnimation: Boolean by mutableStateOf(false)

    // Countdown Progress (1.0 -> 0.0)
    var progress: Double by mutableDoubleStateOf(1.0)
    private var timeLimit: Double = 3.6
    private var timeElapsed: Double = 0.0

    // Grid of Cubes & FX
    var cubeOrder: List<TapCube> by mutableStateOf(emptyList())
    var particles: List<TapPixelParticle> by mutableStateOf(emptyList())
    var burstRings: List<TapBurstRing> by mutableStateOf(emptyList())
    var frameTick: Long by mutableStateOf(0L)
    private var lastFrameNanos: Long? = null

    // Milestone Rule Banner
    var bannerText: String? by mutableStateOf(null)
    var showBanner: Boolean by mutableStateOf(false)

    // Target Verification Closure
    private var isCorrectAnswer: ((Int) -> Boolean)? = null
    private var lastTargetColorId: Int? = null

    private val cubes = listOf(
        TapCube(id = 0, name = "RED", color = CheerUpColors.Tap.red, hex = 0xFF4B5C),
        TapCube(id = 1, name = "BLUE", color = CheerUpColors.Tap.blue, hex = 0x3D7BFF),
        TapCube(id = 2, name = "GREEN", color = CheerUpColors.Tap.green, hex = 0x2EDB8A),
        TapCube(id = 3, name = "YELLOW", color = CheerUpColors.Tap.yellow, hex = 0xFFC83D)
    )

    private val cubesFr = listOf("ROUGE", "BLEU", "VERT", "JAUNE")
    private val cubesEs = listOf("ROJO", "AZUL", "VERDE", "AMARILLO")

    init {
        cubeOrder = cubes
    }

    private fun cubeName(forId: Int): String {
        return when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> cubesFr[forId]
            AppLanguage.SPANISH -> cubesEs[forId]
            AppLanguage.ENGLISH -> cubes[forId].name
        }
    }

    private val cheers: List<String>
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> listOf("Bravo !", "Top !", "Super !", "Génial !", "Parfait !")
            AppLanguage.SPANISH -> listOf("¡Genial!", "¡Bien!", "¡Perfecto!", "¡Súper!", "¡Excelente!")
            AppLanguage.ENGLISH -> listOf("Nice!", "Boom!", "Smooth.", "Yes yes yes!", "Look at you go.")
        }

    private val oopsMessages: List<String>
        get() = when (LanguageManager.shared.currentLanguage) {
            AppLanguage.FRENCH -> listOf("Oups.", "Pas facile !", "Presque !", "Aïe.")
            AppLanguage.SPANISH -> listOf("¡Ups!", "¡Difícil!", "¡Casi!", "¡Ánimo!")
            AppLanguage.ENGLISH -> listOf("Oof.", "Colours are hard, okay?", "I believed in you.", "That's a no from me.")
        }

    private val milestones: Map<Int, Pair<TapRuleType, String>>
        get() = mapOf(
            6 to (TapRuleType.INK to LanguageManager.shared.tr(
                "Follow the ink color!",
                "Suivez la couleur de l'encre !",
                "¡Sigue el color de la tinta!"
            )),
            12 to (TapRuleType.NOT to LanguageManager.shared.tr(
                "Some colors are now banned!",
                "Des couleurs sont bannies !",
                "¡Colores prohibidos!"
            )),
            18 to (TapRuleType.PREV to LanguageManager.shared.tr(
                "Memory test. Don't blink!",
                "Test mémoire. Attention !",
                "¡Prueba de memoria!"
            )),
            25 to (TapRuleType.LIE to LanguageManager.shared.tr(
                "Watch out for psychs!",
                "Attention aux pièges !",
                "¡Cuidado con las trampas!"
            ))
        )

    fun startGame() {
        sessionGeneration += 1
        lives = 3
        round = 0
        streak = 0
        isOver = false
        isLive = false
        hasUsedRewardedContinue = false
        hasSubmittedScoreForCurrentEnding = false
        lastTargetColorId = null
        particles = emptyList()
        burstRings = emptyList()
        lastFrameNanos = null
        cubeOrder = cubes
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Let's go! Tap fast, think faster.",
            "C'est parti ! Touchez vite.",
            "¡Vamos! Toca rápido."
        )
        AudioManager.shared.play(AudioManager.SoundEffect.POP)

        val gen = sessionGeneration
        scheduleAfter(600L) {
            if (sessionGeneration == gen) {
                nextRound()
            }
        }
    }

    /** Grants one additional life/attempt (1 heart) after a rewarded video ad and resumes from the current round. */
    fun continueAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        hasSubmittedScoreForCurrentEnding = false
        sessionGeneration += 1

        lives = 1
        isOver = false
        isLive = false
        progress = 1.0
        timeElapsed = 0.0
        showBanner = false
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Second chance! Let's go!",
            "Seconde chance ! C'est parti !",
            "¡Segunda oportunidad! ¡Vamos!"
        )
        AudioManager.shared.play(AudioManager.SoundEffect.POP)

        val gen = sessionGeneration
        scheduleAfter(500L) {
            if (sessionGeneration == gen) {
                nextRound(advanceRound = false)
            }
        }
    }

    fun nextRound(advanceRound: Boolean = true) {
        if (isOver) return
        if (advanceRound || round == 0) {
            round += 1
        }
        instructionTag = ""
        isPsychAnimation = false

        val milestone = milestones[round]
        if (milestone != null) {
            AudioManager.shared.play(AudioManager.SoundEffect.NEXT_LEVEL)
            bannerText = milestone.second
            showBanner = true
            mascotMood = MascotMood.WOW

            val gen = sessionGeneration
            scheduleAfter(1800L) {
                if (sessionGeneration == gen && !isOver) {
                    showBanner = false
                    scheduleAfter(250L) {
                        if (sessionGeneration == gen && !isOver) {
                            setupRound(forcedRule = milestone.first)
                        }
                    }
                }
            }
        } else {
            setupRound(forcedRule = null)
        }
    }

    private fun setupRound(forcedRule: TapRuleType?) {
        if (isOver) return

        val pool = mutableListOf<TapRuleType>()
        if (round < 6) {
            pool.add(TapRuleType.TAP)
        } else {
            pool.addAll(listOf(TapRuleType.INK, TapRuleType.INK, TapRuleType.INK, TapRuleType.INK))
            if (round >= 12) {
                pool.add(TapRuleType.NOT)
                pool.add(TapRuleType.NOT)
            }
            if (round >= 18 && lastTargetColorId != null) {
                pool.add(TapRuleType.PREV)
            }
            if (round >= 25) {
                pool.add(TapRuleType.LIE)
            }
        }

        var rule = forcedRule ?: if (round == 1) TapRuleType.TAP else (pool.randomOrNull() ?: TapRuleType.TAP)
        if (rule == TapRuleType.PREV && lastTargetColorId == null) {
            rule = if (round < 6) TapRuleType.TAP else TapRuleType.INK
        }

        cubeOrder = if (round >= 20) cubes.shuffled() else cubes

        val targetId = Random.nextInt(0, 4)
        var extraTime = 0.0
        var lieFakeId: Int? = null

        when (rule) {
            TapRuleType.TAP -> {
                instructionPrefix = LanguageManager.shared.tr("Tap", "Touchez", "Toca")
                instructionWord = cubeName(targetId)
                instructionColor = cubes[targetId].color
                isCorrectAnswer = { c -> c == targetId }
                lastTargetColorId = targetId
            }
            TapRuleType.NOT -> {
                instructionPrefix = LanguageManager.shared.tr("Anything but", "Sauf", "Menos")
                instructionWord = cubeName(targetId)
                instructionColor = cubes[targetId].color
                isCorrectAnswer = { c -> c != targetId }
                lastTargetColorId = targetId
                extraTime = 0.5
            }
            TapRuleType.INK -> {
                val inkId = (targetId + 1 + Random.nextInt(0, 3)) % 4
                instructionPrefix = LanguageManager.shared.tr("Tap ink:", "Encre :", "Tinta:")
                instructionWord = cubeName(targetId)
                instructionColor = cubes[inkId].color
                isCorrectAnswer = { c -> c == inkId }
                lastTargetColorId = inkId
                extraTime = 0.6
            }
            TapRuleType.PREV -> {
                val prevId = lastTargetColorId ?: targetId
                instructionPrefix = LanguageManager.shared.tr("Tap last round's", "Dernière", "Anterior")
                instructionWord = LanguageManager.shared.tr("COLOR", "COULEUR", "COLOR")
                instructionColor = Color.White
                instructionTag = ""
                isCorrectAnswer = { c -> c == prevId }
                extraTime = 2.0
            }
            TapRuleType.LIE -> {
                lieFakeId = (targetId + 1 + Random.nextInt(0, 3)) % 4
                isCorrectAnswer = { c -> c == targetId }
                lastTargetColorId = targetId
                extraTime = 0.6
            }
        }

        val baseTime = if (round == 1) 5.5 else max(2.0, 4.4 - round * 0.04)
        timeLimit = baseTime + extraTime
        timeElapsed = 0.0
        progress = 1.0

        val fakeId = lieFakeId
        if (fakeId != null) {
            instructionPrefix = LanguageManager.shared.tr("Tap", "Touchez", "Toca")
            instructionWord = cubeName(fakeId)
            instructionColor = cubes[fakeId].color
            isLive = false

            val gen = sessionGeneration
            scheduleAfter(750L) {
                if (sessionGeneration == gen && !isOver) {
                    instructionWord = cubeName(targetId)
                    instructionColor = cubes[targetId].color
                    instructionTag = LanguageManager.shared.tr("Psych!", "Raté !", "¡Caíste!")
                    isPsychAnimation = true
                    AudioManager.shared.play(AudioManager.SoundEffect.SWITCH_TOGGLE)
                    isLive = true
                }
            }
        } else {
            isLive = true
        }
    }

    fun selectCube(
        cube: TapCube,
        tapPoint: Offset = Offset.Zero,
        targetBounds: Rect? = null
    ) {
        if (!isLive || isOver) return
        isLive = false

        val check = isCorrectAnswer
        if (check != null && check(cube.id)) {
            spawnConfetti(point = tapPoint, bounds = targetBounds, color = cube.color)
            streak += 1
            if (streak % 5 == 0) {
                AudioManager.shared.play(AudioManager.SoundEffect.COMBO)
            } else {
                AudioManager.shared.play(AudioManager.SoundEffect.POP)
            }
            HapticsManager.shared.lightTap()

            mascotMood = if (Random.nextDouble() < 0.25) MascotMood.WOW else MascotMood.HAPPY
            if (Random.nextDouble() < 0.5) {
                speechText = cheers.random()
            }

            val gen = sessionGeneration
            scheduleAfter(380L) {
                if (sessionGeneration == gen) {
                    nextRound()
                }
            }
        } else {
            handleMistake(oopsMessages.random())
        }
    }

    private fun handleMistake(say: String) {
        streak = 0
        lives = max(0, lives - 1)
        AudioManager.shared.play(AudioManager.SoundEffect.ERROR)
        HapticsManager.shared.heavyImpact()
        mascotMood = MascotMood.SAD
        speechText = say

        if (lives <= 0) {
            isOver = true
            val gen = sessionGeneration
            scheduleAfter(800L) {
                if (sessionGeneration == gen && isOver) {
                    finishGame()
                }
            }
        } else {
            val gen = sessionGeneration
            scheduleAfter(750L) {
                if (sessionGeneration == gen) {
                    nextRound()
                }
            }
        }
    }

    fun resetFrameTiming() {
        lastFrameNanos = null
    }

    fun updateFrameNanos(frameTimeNanos: Long) {
        val dt = lastFrameNanos?.let { prev ->
            ((frameTimeNanos - prev) / 1_000_000_000.0).coerceIn(0.0, 0.05)
        } ?: (1.0 / 60.0)
        lastFrameNanos = frameTimeNanos
        tick(dt)
    }

    fun tick(dt: Double = 0.05) {
        if (burstRings.isNotEmpty()) {
            val updatedRings = burstRings.toMutableList()
            for (r in updatedRings) {
                r.age += dt
                val t = min(1.0, r.age / r.duration)
                val eased = 1.0 - (1.0 - t).pow(3.0)
                r.radius = (r.startRadius + (r.maxRadius - r.startRadius) * eased).toFloat()
                r.opacity = (0.75 * (1.0 - eased)).toFloat()
            }
            updatedRings.removeAll { it.age >= it.duration }
            burstRings = updatedRings
            frameTick += 1
        }

        if (particles.isNotEmpty()) {
            val updatedParticles = particles.toMutableList()
            for (p in updatedParticles) {
                p.age += dt
                val damping = p.drag.pow(dt).toFloat()
                p.vx *= damping
                p.vy *= damping
                p.vy += p.gravity * dt.toFloat()

                p.swayPhase += p.swaySpeed * dt
                val flutter = sin(p.swayPhase).toFloat() * p.swayAmplitude
                p.x += (p.vx + flutter) * dt.toFloat()
                p.y += p.vy * dt.toFloat()

                p.rotation += p.vr * dt
                p.tilt += p.tiltSpeed * dt

                val lifeProgress = p.age / p.lifetime
                if (lifeProgress >= 0.58) {
                    val fadeProgress = min(1.0, (lifeProgress - 0.58) / 0.42)
                    p.opacity = max(0f, (1.0 - fadeProgress.pow(1.5)).toFloat())
                }
            }
            updatedParticles.removeAll { it.age >= it.lifetime || it.opacity <= 0.01f }
            particles = updatedParticles
            frameTick += 1
        }

        if (!isLive || isOver) return

        timeElapsed += dt
        progress = max(0.0, 1.0 - (timeElapsed / timeLimit))

        if (progress <= 0.0) {
            isLive = false
            handleMistake(
                LanguageManager.shared.tr(
                    "Too slow! The colours got bored.",
                    "Trop lent ! Les couleurs s'ennuient.",
                    "¡Muy lento! Los colores se aburrieron."
                )
            )
            if (lives <= 0) {
                finishGame()
            }
        }
    }

    private fun spawnConfetti(point: Offset, bounds: Rect?, color: Color) {
        val box = if (bounds != null && bounds.width > 10f && bounds.height > 10f) {
            bounds
        } else {
            val fallbackHalf = 180f
            Rect(
                left = point.x - fallbackHalf,
                top = point.y - fallbackHalf,
                right = point.x + fallbackHalf,
                bottom = point.y + fallbackHalf
            )
        }

        val w = box.width
        val h = box.height
        val cx = box.center.x
        val cy = box.center.y
        // Normalize physics and piece sizes relative to the tapped color cube dimensions
        val unit = max(1.0f, w / 160f)

        val ringStart = min(w, h) * 0.16f
        val ringMax = max(w, h) * 0.62f
        val updatedRings = (
            burstRings + TapBurstRing(
                x = cx,
                y = cy,
                color = color,
                radius = ringStart,
                startRadius = ringStart,
                maxRadius = ringMax,
                duration = 0.42
            )
        ).takeLast(6)
        burstRings = updatedRings

        val palette = listOf(
            color,
            Color.White,
            Color(0xFFFFC83D),
            Color(0xFF2EDB8A),
            Color(0xFF3D7BFF),
            Color(0xFFFF4B5C),
            Color(0xFFFF7AB6)
        )

        val newParticles = ArrayList<TapPixelParticle>(64)

        // Layer 1: Stratified 6x6 grid across the ENTIRE selected box bounds so all 4 quadrants & edges fill evenly
        val cols = 6
        val rows = 6
        val padX = w * 0.06f
        val padY = h * 0.06f
        val innerW = w - padX * 2f
        val innerH = h - padY * 2f
        val cellW = innerW / cols
        val cellH = innerH / rows

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val px = box.left + padX + (col + Random.nextDouble(0.15, 0.85).toFloat()) * cellW
                val py = box.top + padY + (row + Random.nextDouble(0.15, 0.85).toFloat()) * cellH
                val outAngle = atan2((py - cy).toDouble(), (px - cx).toDouble()) + Random.nextDouble(-0.35, 0.35)
                val outSpeed = Random.nextDouble(95.0, 260.0).toFloat() * unit
                val shape = if (Random.nextBoolean()) TapConfettiShape.RIBBON else TapConfettiShape.SQUARE

                newParticles.add(
                    TapPixelParticle(
                        x = px,
                        y = py,
                        vx = cos(outAngle).toFloat() * outSpeed,
                        vy = sin(outAngle).toFloat() * outSpeed - Random.nextDouble(65.0, 155.0).toFloat() * unit,
                        rotation = Random.nextDouble(0.0, 360.0),
                        vr = Random.nextDouble(-280.0, 280.0),
                        tilt = Random.nextDouble(0.0, 2.0 * PI),
                        tiltSpeed = Random.nextDouble(7.0, 15.0),
                        swayPhase = Random.nextDouble(0.0, 2.0 * PI),
                        swaySpeed = Random.nextDouble(5.5, 9.5),
                        swayAmplitude = Random.nextDouble(16.0, 34.0).toFloat() * unit,
                        drag = Random.nextDouble(0.06, 0.14),
                        gravity = Random.nextDouble(340.0, 480.0).toFloat() * unit,
                        size = Random.nextDouble(7.5, 11.5).toFloat() * unit,
                        aspect = if (shape == TapConfettiShape.RIBBON) Random.nextDouble(0.42, 0.58).toFloat() else 0.88f,
                        shape = shape,
                        color = palette.random(),
                        lifetime = Random.nextDouble(1.00, 1.45)
                    )
                )
            }
        }

        // Layer 2: Full-width celebratory canopy ribbons lifting cleanly across the box top & sides
        val ribbonCount = 16
        for (i in 0 until ribbonCount) {
            val frac = (i + Random.nextDouble(0.15, 0.85).toFloat()) / ribbonCount.toFloat()
            val px = box.left + padX + frac * innerW
            val py = box.top + padY + Random.nextDouble(0.05, 0.65).toFloat() * innerH
            val spreadAngle = (frac - 0.5f) * 1.15f - (PI / 2.0).toFloat()
            val speed = Random.nextDouble(190.0, 340.0).toFloat() * unit

            newParticles.add(
                TapPixelParticle(
                    x = px,
                    y = py,
                    vx = cos(spreadAngle.toDouble()).toFloat() * speed,
                    vy = sin(spreadAngle.toDouble()).toFloat() * speed,
                    rotation = Random.nextDouble(0.0, 360.0),
                    vr = Random.nextDouble(-320.0, 320.0),
                    tilt = Random.nextDouble(0.0, 2.0 * PI),
                    tiltSpeed = Random.nextDouble(8.0, 16.0),
                    swayPhase = Random.nextDouble(0.0, 2.0 * PI),
                    swaySpeed = Random.nextDouble(5.0, 9.0),
                    swayAmplitude = Random.nextDouble(20.0, 40.0).toFloat() * unit,
                    drag = Random.nextDouble(0.06, 0.13),
                    gravity = Random.nextDouble(320.0, 450.0).toFloat() * unit,
                    size = Random.nextDouble(8.5, 12.5).toFloat() * unit,
                    aspect = Random.nextDouble(0.40, 0.55).toFloat(),
                    shape = TapConfettiShape.RIBBON,
                    color = palette.random(),
                    lifetime = Random.nextDouble(1.10, 1.55)
                )
            )
        }

        // Layer 3: Full-box diamond sparkle glints across perimeter & quadrants
        for (i in 0 until 12) {
            val qx = (i % 4) / 4f + Random.nextDouble(0.04, 0.21).toFloat()
            val qy = (i / 4) / 3f + Random.nextDouble(0.06, 0.26).toFloat()
            val px = box.left + padX + qx * innerW
            val py = box.top + padY + qy * innerH
            val angle = atan2((py - cy).toDouble(), (px - cx).toDouble())
            val speed = Random.nextDouble(80.0, 190.0).toFloat() * unit

            newParticles.add(
                TapPixelParticle(
                    x = px,
                    y = py,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed - 45f * unit,
                    rotation = Random.nextDouble(0.0, 360.0),
                    vr = Random.nextDouble(-200.0, 200.0),
                    tilt = Random.nextDouble(0.0, 2.0 * PI),
                    tiltSpeed = Random.nextDouble(10.0, 18.0),
                    swayPhase = 0.0,
                    swaySpeed = 6.0,
                    swayAmplitude = 10f * unit,
                    drag = 0.09,
                    gravity = 240f * unit,
                    size = Random.nextDouble(5.5, 8.5).toFloat() * unit,
                    aspect = 1.0f,
                    shape = TapConfettiShape.DIAMOND,
                    color = if (Random.nextBoolean()) Color.White else Color(0xFFFFC83D),
                    lifetime = Random.nextDouble(0.60, 0.90)
                )
            )
        }

        particles = (particles + newParticles).takeLast(200)
        frameTick += 1
    }

    internal fun finishGame() {
        if (hasSubmittedScoreForCurrentEnding) return
        hasSubmittedScoreForCurrentEnding = true
        val sc = max(0, round - 1)
        ScoreManager.shared.updateHighScore(GameIdentifier.TAP, sc)
        highScore = ScoreManager.shared.highScore(GameIdentifier.TAP)
        PlayGamesManager.shared.submitScore(sc, GameIdentifier.TAP)
        HapticsManager.shared.heavyImpact()
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
            // In pure JVM unit tests without Android main looper, execute synchronously
            action()
        }
    }
}

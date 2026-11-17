package com.example.cheerup.games.rush

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cheerup.R
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.presentation.components.CheerUpCircleIconButton
import com.example.cheerup.presentation.components.CheerUpMascotView
import com.example.cheerup.presentation.components.CheerUpPrimaryButton
import com.example.cheerup.presentation.components.CheerUpSecondaryButton
import com.example.cheerup.presentation.components.CheerUpSymbolIcon
import com.example.cheerup.presentation.components.GameResultView
import com.example.cheerup.presentation.components.SpeechBubbleShape
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private data class RushStar(
    var x: Float,
    var y: Float,
    val speed: Float,
    val pixelSize: Float,
    val color: Color
)

private data class RushDebrisParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val w: Float,
    val h: Float,
    val color: Color,
    var rotation: Float = 0f,
    val vr: Float = 0f,
    var age: Float = 0f,
    val duration: Float = 0.45f
)

private data class RushShockwaveRing(
    val x: Float,
    val y: Float,
    val baseRadius: Float,
    val targetScale: Float,
    val color: Color,
    val lineWidth: Float,
    var age: Float = 0f,
    val duration: Float = 0.42f
)

private data class RushFloatingText(
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var age: Float = 0f,
    val duration: Float = 0.55f
)

@Composable
fun RushScreen(
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun handleExit() {
        RushAudioController.shared.stopBackgroundMusic()
        onExit()
    }

    BackHandler(onBack = ::handleExit)

    val shipFrames = listOf(
        ImageBitmap.imageResource(R.drawable.rush_ship_0),
        ImageBitmap.imageResource(R.drawable.rush_ship_1),
        ImageBitmap.imageResource(R.drawable.rush_ship_2),
        ImageBitmap.imageResource(R.drawable.rush_ship_3),
        ImageBitmap.imageResource(R.drawable.rush_ship_4),
        ImageBitmap.imageResource(R.drawable.rush_ship_5),
        ImageBitmap.imageResource(R.drawable.rush_ship_6),
        ImageBitmap.imageResource(R.drawable.rush_ship_7)
    )
    val flameShipFrames = listOf(
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_0),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_1),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_2),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_3),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_4),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_5),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_6),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_7),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_8),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_9),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_10),
        ImageBitmap.imageResource(R.drawable.rush_flame_ship_11)
    )
    val asteroidFrames = listOf(
        ImageBitmap.imageResource(R.drawable.rush_asteroid_0),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_1),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_2),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_3),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_4),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_5),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_6),
        ImageBitmap.imageResource(R.drawable.rush_asteroid_7)
    )

    val engine = remember { RushEngine() }
    val textMeasurer = rememberTextMeasurer()

    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(ScoreManager.shared.highScore(GameIdentifier.RUSH)) }
    val activeEffectsUI = remember { mutableStateListOf<ActiveRushEffect>() }

    var isPaused by remember { mutableStateOf(false) }
    var isCrashing by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var hasUsedRewardedContinue by remember { mutableStateOf(false) }
    var showDragHint by remember { mutableStateOf(true) }

    var mascotMood by remember { mutableStateOf(MascotMood.HAPPY) }
    var speechText by remember {
        mutableStateOf(
            LanguageManager.shared.tr(
                "Survive! Every 3 seconds is 1 point!",
                "Survivez ! Chaque 3 secondes vaut 1 point !",
                "¡Sobrevive! ¡Cada 3 segundos es 1 punto!"
            )
        )
    }

    var bannerText by remember { mutableStateOf<String?>(null) }
    var bannerColor by remember { mutableStateOf(Color(0xFFFF2E93)) }
    var bannerJob by remember { mutableStateOf<Job?>(null) }

    val stars = remember { ArrayList<RushStar>(42) }
    val debris = remember { ArrayList<RushDebrisParticle>(96) }
    val shockwaves = remember { ArrayList<RushShockwaveRing>(8) }
    val floatingTexts = remember { ArrayList<RushFloatingText>(12) }

    var targetShipPos by remember { mutableStateOf<Offset?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var shipFrameIndex by remember { mutableIntStateOf(0) }
    var flameFrameIndex by remember { mutableIntStateOf(0) }
    var shipAnimTimer by remember { mutableFloatStateOf(0f) }
    var thrusterTimer by remember { mutableFloatStateOf(0f) }
    var shakeTimer by remember { mutableFloatStateOf(0f) }
    var shakeIntensity by remember { mutableFloatStateOf(0f) }
    var impactFlashAlpha by remember { mutableFloatStateOf(0f) }
    var frameCounter by remember { mutableIntStateOf(0) }

    var logicalScale by remember { mutableFloatStateOf(1f) }
    var logicalHeight by remember { mutableFloatStateOf(852f) }

    fun showBanner(text: String, color: Color) {
        bannerJob?.cancel()
        bannerColor = color
        bannerText = text
        bannerJob = scope.launch {
            delay(1500L)
            bannerText = null
        }
    }

    fun spawnPixelBurst(x: Float, y: Float, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextDouble(0.0, 2.0 * PI)
            val speed = Random.nextDouble(60.0, 175.0).toFloat()
            debris.add(
                RushDebrisParticle(
                    x = x,
                    y = y,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed,
                    w = 5f,
                    h = 5f,
                    color = color,
                    duration = 0.32f
                )
            )
        }
    }

    fun triggerRocketCrashFX(shipX: Float, shipY: Float, meteorX: Float, meteorY: Float) {
        val midX = (shipX + meteorX) * 0.5f
        val midY = (shipY + meteorY) * 0.5f

        shakeIntensity = 9.5f
        shakeTimer = 0.25f
        impactFlashAlpha = 0.32f

        // 2 Expanding Shockwave Rings (White core + glowing orange outer)
        shockwaves.add(RushShockwaveRing(midX, midY, 14f, 4.8f, Color.White, 3.0f, duration = 0.36f))
        shockwaves.add(RushShockwaveRing(midX, midY, 16f, 6.2f, Color(0xFFF97316), 2.5f, duration = 0.52f))

        // 14 Shattered Meteor Chunks
        val meteorChunkColors = listOf(Color(0xFF475569), Color(0xFF94A3B8), Color(0xFF1E293B), Color(0xFFFF2E93))
        for (i in 0 until 14) {
            val angle = Random.nextDouble(0.0, 2.0 * PI)
            val speed = Random.nextDouble(55.0, 165.0).toFloat()
            val cSize = if (i % 2 == 0) 7f else 5f
            debris.add(
                RushDebrisParticle(
                    x = meteorX,
                    y = meteorY,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed - 35f,
                    w = cSize,
                    h = cSize,
                    color = meteorChunkColors[i % meteorChunkColors.size],
                    vr = Random.nextDouble(-360.0, 360.0).toFloat(),
                    duration = 0.68f
                )
            )
        }

        // 5 Spinning Broken Rocket Debris Parts
        data class Part(val color: Color, val w: Float, val h: Float, val angle: Double)
        val rocketParts = listOf(
            Part(Color(0xFFF8FAFC), 10f, 12f, PI * 0.45),
            Part(Color(0xFFFF2E93), 12f, 6f, PI * 0.85),
            Part(Color(0xFFFF2E93), 12f, 6f, PI * 0.15),
            Part(Color(0xFF22D3EE), 7f, 7f, PI * 0.65),
            Part(Color(0xFFF8FAFC), 8f, 8f, -PI * 0.40)
        )
        for (part in rocketParts) {
            val speed = Random.nextDouble(70.0, 145.0).toFloat()
            debris.add(
                RushDebrisParticle(
                    x = shipX,
                    y = shipY,
                    vx = cos(part.angle).toFloat() * speed,
                    vy = sin(part.angle).toFloat() * speed - 38f,
                    w = part.w,
                    h = part.h,
                    color = part.color,
                    vr = Random.nextDouble(-420.0, 420.0).toFloat(),
                    duration = 0.78f
                )
            )
        }

        // 26 Fiery Pixel Fireball Plumes
        val firePalette = listOf(Color(0xFFFEF08A), Color(0xFFFFC83D), Color(0xFFF97316), Color(0xFFFF2E93))
        for (i in 0 until 26) {
            val angle = Random.nextDouble(0.0, 2.0 * PI)
            val speed = Random.nextDouble(35.0, 150.0).toFloat()
            val px = Random.nextDouble(5.0, 9.0).toFloat()
            debris.add(
                RushDebrisParticle(
                    x = midX,
                    y = midY,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed,
                    w = px,
                    h = px,
                    color = firePalette[i % firePalette.size],
                    duration = Random.nextDouble(0.32, 0.62).toFloat()
                )
            )
        }
    }

    fun rebuildStars(w: Float, h: Float) {
        stars.clear()
        val palette = listOf(
            Color(0xFFF8FAFC),
            Color(0xFF22D3EE),
            Color(0xFFFF2E93),
            Color(0xFFA855F7),
            Color(0xFF64748B)
        )
        for (i in 0 until 42) {
            val depth = (i % 3 + 1)
            val px = if (depth == 3) 3f else 2f
            val alpha = when (depth) {
                1 -> 0.28f
                2 -> 0.52f
                else -> 0.82f
            }
            stars.add(
                RushStar(
                    x = Random.nextDouble(8.0, max(16.0, (w - 8f).toDouble())).toFloat(),
                    y = Random.nextDouble(8.0, max(16.0, (h - 8f).toDouble())).toFloat(),
                    speed = depth * 48f,
                    pixelSize = px,
                    color = palette[i % palette.size].copy(alpha = alpha)
                )
            )
        }
    }

    fun startNewGame() {
        engine.reset()
        if (stars.isEmpty()) {
            rebuildStars(engine.worldWidth, engine.worldHeight)
        }
        debris.clear()
        shockwaves.clear()
        floatingTexts.clear()
        score = 0
        highScore = ScoreManager.shared.highScore(GameIdentifier.RUSH)
        activeEffectsUI.clear()
        targetShipPos = Offset(engine.shipX, engine.shipY)
        isPaused = false
        isCrashing = false
        isGameOver = false
        hasUsedRewardedContinue = false
        bannerText = null
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Survive! Every 3 seconds is 1 point!",
            "Survivez ! Chaque 3 secondes vaut 1 point !",
            "¡Sobrevive! ¡Cada 3 segundos es 1 punto!"
        )
        RushAudioController.shared.startBackgroundMusic(context)
    }

    fun continueAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        engine.continueAfterReward()
        debris.clear()
        shockwaves.clear()
        floatingTexts.clear()
        targetShipPos = Offset(engine.shipX, engine.shipY)
        isPaused = false
        isCrashing = false
        isGameOver = false
        bannerText = null
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Back in flight! 3s shield active!",
            "De retour en vol ! Bouclier 3s actif !",
            "¡De vuelta en vuelo! ¡Escudo 3s activo!"
        )
        RushAudioController.shared.startBackgroundMusic(context)
    }

    LaunchedEffect(Unit) {
        RushAudioController.shared.initializeIfNeeded(context)
        startNewGame()
        delay(4000L)
        showDragHint = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (!isGameOver && !isPaused && !isCrashing) {
                    isPaused = true
                    RushAudioController.shared.pauseBackgroundMusic()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!RewardedAdManager.shared.isPresentingAd) {
                RushAudioController.shared.stopBackgroundMusic()
            }
        }
    }

    LaunchedEffect(isPaused, isGameOver) {
        if (!isPaused && !isGameOver) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    val dt = if (lastNanos == 0L) 1.0 / 60.0 else ((nanos - lastNanos) / 1_000_000_000.0).coerceIn(0.0, 0.05)
                    lastNanos = nanos
                    val dtF = dt.toFloat()

                    // Update visual particles & shockwaves even during 0.85s crash sequence
                    if (shakeTimer > 0f) shakeTimer = max(0f, shakeTimer - dtF)
                    if (impactFlashAlpha > 0f) impactFlashAlpha = max(0f, impactFlashAlpha - dtF * 1.2f)

                    for (i in shockwaves.indices.reversed()) {
                        val sw = shockwaves[i]
                        sw.age += dtF
                        if (sw.age >= sw.duration) shockwaves.removeAt(i)
                    }
                    for (i in debris.indices.reversed()) {
                        val d = debris[i]
                        d.age += dtF
                        d.x += d.vx * dtF
                        d.y += d.vy * dtF
                        d.rotation += d.vr * dtF
                        if (d.age >= d.duration) debris.removeAt(i)
                    }
                    for (i in floatingTexts.indices.reversed()) {
                        val ft = floatingTexts[i]
                        ft.age += dtF
                        ft.y += 42f * dtF
                        if (ft.age >= ft.duration) floatingTexts.removeAt(i)
                    }

                    // Scroll parallax starfield
                    val isFlame = engine.isFlameBoostActive
                    val starMult = if (isFlame) 3.2f else if (engine.isSlowTimeActive) 0.45f else 1.0f
                    for (st in stars) {
                        st.y -= st.speed * starMult * dtF
                        if (st.y < -12f) {
                            st.y = engine.worldHeight + 12f
                            st.x = Random.nextDouble(8.0, max(16.0, (engine.worldWidth - 8f).toDouble())).toFloat()
                        }
                    }

                    if (!isCrashing && !engine.isGameOver) {
                        val events = engine.update(dt, targetShipPos)

                        shipAnimTimer += dtF
                        if (isFlame) {
                            if (shipAnimTimer >= 0.045f) {
                                shipAnimTimer = 0f
                                flameFrameIndex = (flameFrameIndex + 1) % 12
                            }
                        } else {
                            if (shipAnimTimer >= 0.065f) {
                                shipAnimTimer = 0f
                                shipFrameIndex = (shipFrameIndex + 1) % 8
                            }
                        }

                        thrusterTimer += dtF
                        val thrusterInterval = if (isFlame) 0.022f else 0.045f
                        if (thrusterTimer >= thrusterInterval) {
                            thrusterTimer = 0f
                            val tColor = when {
                                isFlame -> Color(0xFFFF5500)
                                engine.isSlowTimeActive -> Color(0xFF22D3EE)
                                else -> Color(0xFFFF2E93)
                            }
                            debris.add(
                                RushDebrisParticle(
                                    x = engine.shipX + Random.nextDouble(-4.0, 4.0).toFloat(),
                                    y = engine.shipY - 26f,
                                    vx = Random.nextDouble(-25.0, 25.0).toFloat(),
                                    vy = -120f,
                                    w = if (isFlame) 5f else 4f,
                                    h = if (isFlame) 6f else 4f,
                                    color = tColor,
                                    duration = 0.22f
                                )
                            )
                        }

                        for (ev in events) {
                            when (ev) {
                                is RushEvent.ScoreChanged -> {
                                    score = ev.newScore
                                    if (score > highScore) {
                                        highScore = score
                                        ScoreManager.shared.updateHighScore(GameIdentifier.RUSH, score)
                                    }
                                }
                                is RushEvent.NearMiss -> {
                                    if (!engine.isFlameBoostActive) {
                                        HapticsManager.shared.lightTap()
                                        RushAudioController.shared.playNearMiss()
                                        spawnPixelBurst(ev.x, ev.y, Color(0xFF22D3EE), 9)
                                        floatingTexts.add(
                                            RushFloatingText(
                                                text = LanguageManager.shared.tr("CLOSE CALL!", "C'ÉTAIT JUSTE !", "¡POR POCO!"),
                                                x = engine.shipX,
                                                y = engine.shipY + 26f,
                                                color = Color(0xFF22D3EE)
                                            )
                                        )
                                        mascotMood = if (ev.streak >= 3) MascotMood.PARTY else MascotMood.WOW
                                        speechText = if (ev.streak >= 3) {
                                            LanguageManager.shared.tr(
                                                "Incredible flying! ${ev.streak} close dodges in a row!",
                                                "Vol incroyable ! ${ev.streak} esquives serrées de suite !",
                                                "¡Vuelo increíble! ¡${ev.streak} esquivas seguidas!"
                                            )
                                        } else {
                                            LanguageManager.shared.tr(
                                                "Whoa, close call! Keep surviving!",
                                                "Waouh, c'était juste ! Continuez de survivre !",
                                                "¡Guau, por poco! ¡Sigue sobreviviendo!"
                                            )
                                        }
                                    }
                                }
                                is RushEvent.PowerUpCollected -> {
                                    AudioManager.shared.play(AudioManager.SoundEffect.NEXT_LEVEL)
                                    spawnPixelBurst(ev.x, ev.y, ev.type.color, 16)
                                    when (ev.type) {
                                        RushPowerUpType.SHIELD -> {
                                            HapticsManager.shared.mediumImpact()
                                            mascotMood = MascotMood.HAPPY
                                            speechText = LanguageManager.shared.tr(
                                                "Shield Orb collected! Blocks 1 meteor hit!",
                                                "Orbe Bouclier ! Bloque 1 impact de météore !",
                                                "¡Orbe Escudo! ¡Bloquea 1 impacto!"
                                            )
                                            floatingTexts.add(RushFloatingText(ev.type.badgeTitle, ev.x, ev.y + 18f, ev.type.color))
                                        }
                                        RushPowerUpType.SLOW_TIME -> {
                                            HapticsManager.shared.mediumImpact()
                                            mascotMood = MascotMood.SUS
                                            speechText = LanguageManager.shared.tr(
                                                "Slow Orb collected! Meteors slowed down!",
                                                "Orbe Ralenti ! Météores ralentis !",
                                                "¡Orbe Lento! ¡Meteoros más lentos!"
                                            )
                                            floatingTexts.add(RushFloatingText(ev.type.badgeTitle, ev.x, ev.y + 18f, ev.type.color))
                                        }
                                        RushPowerUpType.FLAME_BOOST -> {
                                            floatingTexts.clear()
                                            HapticsManager.shared.heavyImpact()
                                            shakeIntensity = 6.5f
                                            shakeTimer = 0.16f
                                            spawnPixelBurst(ev.x, ev.y, Color(0xFFFEF08A), 18)
                                            mascotMood = MascotMood.PARTY
                                            speechText = LanguageManager.shared.tr(
                                                "FLAME ORB! 5s Intangible & Max Speed!",
                                                "ORBE FLAMME ! 5s Intangible & Vitesse Max !",
                                                "¡ORBE LLAMA! ¡5s Intangible y Velocidad Máx!"
                                            )
                                            showBanner(
                                                LanguageManager.shared.tr("FLAME BOOST! 5S INTANGIBLE!", "TURBO FLAMME ! 5S INTANGIBLE !", "¡TURBO LLAMA! ¡5S INTANGIBLE!"),
                                                Color(0xFFFF5500)
                                            )
                                        }
                                    }
                                }
                                is RushEvent.ShieldAbsorbedHit -> {
                                    HapticsManager.shared.heavyImpact()
                                    RushAudioController.shared.playShieldBreak()
                                    shakeIntensity = 5.5f
                                    shakeTimer = 0.15f
                                    shockwaves.add(RushShockwaveRing(engine.shipX, engine.shipY, 22f, 3.4f, Color(0xFF22D3EE), 2.5f, duration = 0.34f))
                                    spawnPixelBurst(ev.x, ev.y, Color(0xFF22D3EE), 16)
                                    spawnPixelBurst(ev.x, ev.y, Color(0xFF94A3B8), 10)
                                    floatingTexts.add(
                                        RushFloatingText(
                                            text = LanguageManager.shared.tr("SHIELD SAVED!", "BOUCLIER !", "¡ESCUDO!"),
                                            x = engine.shipX,
                                            y = engine.shipY + 28f,
                                            color = Color(0xFF22D3EE)
                                        )
                                    )
                                    mascotMood = MascotMood.WOW
                                    speechText = LanguageManager.shared.tr(
                                        "Phew! Your Shield shattered that meteor!",
                                        "Ouf ! Le bouclier a pulvérisé ce météore !",
                                        "¡Uf! ¡Tu escudo destruyó ese meteoro!"
                                    )
                                    showBanner(
                                        LanguageManager.shared.tr("SHIELD SAVED YOU!", "SAUVÉ PAR LE BOUCLIER !", "¡SALVADO POR EL ESCUDO!"),
                                        Color(0xFF22D3EE)
                                    )
                                }
                                is RushEvent.SpecialEventWarning -> {
                                    HapticsManager.shared.selection()
                                    RushAudioController.shared.playWarning()
                                    mascotMood = MascotMood.DIZZY
                                    speechText = ev.kind.warningText
                                    showBanner(ev.kind.warningText, CheerUpColors.fromHex(ev.kind.colorHex))
                                }
                                is RushEvent.SpecialEventStarted -> {}
                                is RushEvent.GameOver -> {
                                    HapticsManager.shared.error()
                                    AudioManager.shared.play(if (ev.isNewBest) AudioManager.SoundEffect.WIN else AudioManager.SoundEffect.ERROR)
                                    RushAudioController.shared.stopBackgroundMusic()
                                    isCrashing = true
                                    triggerRocketCrashFX(engine.shipX, engine.shipY, ev.impactX, ev.impactY)
                                    score = ev.finalScore
                                    highScore = ScoreManager.shared.highScore(GameIdentifier.RUSH)
                                    mascotMood = if (ev.isNewBest) MascotMood.PARTY else MascotMood.SAD
                                    speechText = if (ev.isNewBest) {
                                        LanguageManager.shared.tr("New cosmic record! Legendary pilot!", "Nouveau record cosmique ! Pilote de légende !", "¡Nuevo récord cósmico! ¡Piloto legendario!")
                                    } else {
                                        LanguageManager.shared.tr("Hull breached! Ready for another flight?", "Coque touchée ! Prêt pour un autre vol ?", "¡Nave impactada! ¿Listo para otro vuelo?")
                                    }
                                    scope.launch {
                                        delay(850L)
                                        if (isCrashing) {
                                            isCrashing = false
                                            isGameOver = true
                                        }
                                    }
                                }
                            }
                        }

                        activeEffectsUI.clear()
                        activeEffectsUI.addAll(engine.activeEffects.map { it.copy() })
                    }

                    frameCounter += 1
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF131B2E),
                        Color(0xFF0B101D),
                        Color(0xFF060911)
                    )
                )
            )
            .systemBarsPadding()
    ) {
        // 60/120 FPS Space Dodge Canvas + 1-Finger Drag Input
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPaused, isGameOver, isCrashing) {
                    if (isPaused || isGameOver || isCrashing) return@pointerInput
                    awaitEachGesture {
                        val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                        val scale = if (logicalScale > 0f) logicalScale else 1f
                        val lX = down.position.x / scale
                        val lY = logicalHeight - (down.position.y / scale)
                        val dist = hypot(lX - engine.shipX, lY - engine.shipY)
                        dragOffset = if (dist < 64f) {
                            Offset(engine.shipX - lX, max(28f, engine.shipY - lY))
                        } else {
                            Offset(engine.shipX - lX, engine.shipY - lY)
                        }
                        targetShipPos = Offset(lX + dragOffset.x, lY + dragOffset.y)

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                val curLX = change.position.x / scale
                                val curLY = logicalHeight - (change.position.y / scale)
                                targetShipPos = Offset(curLX + dragOffset.x, curLY + dragOffset.y)
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val _frame = frameCounter
            val wLogical = 393f
            val scale = size.width / wLogical
            val hLogical = size.height / scale
            logicalScale = scale
            logicalHeight = hLogical

            if (abs(engine.worldWidth - wLogical) > 1f || abs(engine.worldHeight - hLogical) > 1f) {
                engine.updateWorldSize(wLogical, hLogical)
                if (stars.isEmpty()) rebuildStars(wLogical, hLogical)
            }

            val shakeX = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 2f * shakeIntensity * scale else 0f
            val shakeY = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 2f * shakeIntensity * scale else 0f

            fun toScreen(lx: Float, ly: Float): Offset =
                Offset(lx * scale + shakeX, (hLogical - ly) * scale + shakeY)

            val isFlameBoost = engine.isFlameBoostActive

            // 1. Starfield
            for (st in stars) {
                val p = toScreen(st.x, st.y)
                val sw = st.pixelSize * scale
                val sh = st.pixelSize * scale * (if (isFlameBoost) 4.2f else 1.0f)
                drawRect(
                    color = st.color,
                    topLeft = Offset(p.x - sw * 0.5f, p.y - sh * 0.5f),
                    size = Size(sw, sh)
                )
            }

            // 2. Collectibles (Little Glowing Power-Up Orbs)
            for (col in engine.collectibles) {
                val p = toScreen(col.x, col.y)
                val auraR = (if (col.type == RushPowerUpType.FLAME_BOOST) 18f else 15f) * scale
                drawCircle(
                    color = col.type.color.copy(alpha = if (col.type == RushPowerUpType.FLAME_BOOST) 0.28f else 0.16f),
                    radius = auraR,
                    center = p
                )
                drawCircle(
                    color = col.type.color.copy(alpha = 0.85f),
                    radius = auraR,
                    center = p,
                    style = Stroke(width = 2f * scale)
                )
                drawCircle(
                    color = col.type.color,
                    radius = 10f * scale,
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * scale,
                    center = p + Offset(-2.5f * scale, -2.5f * scale)
                )
            }

            // 3. Meteors (56x56 logical pt)
            val meteorPx = (56f * scale).toInt()
            for (meteor in engine.asteroids) {
                val p = toScreen(meteor.x, meteor.y)
                val bmp = asteroidFrames[meteor.variantIndex.coerceIn(0, 7)]
                withTransform({
                    translate(left = p.x, top = p.y)
                    rotate(degrees = (-meteor.rotation * 180f / PI.toFloat()), pivot = Offset.Zero)
                }) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(-meteorPx / 2, -meteorPx / 2),
                        dstSize = IntSize(meteorPx, meteorPx),
                        filterQuality = FilterQuality.None
                    )
                }
            }

            // 4. Player Spaceship (hidden during crash sequence)
            if (!isCrashing && !engine.isGameOver) {
                val sp = toScreen(engine.shipX, engine.shipY)
                val shipBmp = if (isFlameBoost) {
                    flameShipFrames[flameFrameIndex.coerceIn(0, 11)]
                } else {
                    shipFrames[shipFrameIndex.coerceIn(0, 7)]
                }
                val shipW = ((if (isFlameBoost) 52f else 44f) * scale).toInt()
                val shipH = ((if (isFlameBoost) 89f else 74f) * scale).toInt()
                // Anchor on solid hull center (anchorPoint y = 0.74 in SpriteKit means top is 0.26 * H above shipY)
                val offsetTop = -(shipH * 0.26f).toInt()
                val shipAlpha = if (isFlameBoost) {
                    0.76f + 0.24f * abs(sin(engine.elapsedTime * 24.0)).toFloat()
                } else if (engine.invulnerabilityTimer > 0 && ((engine.invulnerabilityTimer * 12).toInt() % 2 == 0)) {
                    0.45f
                } else {
                    1.0f
                }

                withTransform({
                    translate(left = sp.x, top = sp.y)
                    rotate(degrees = (engine.shipTilt * 0.28f * 180f / PI.toFloat()), pivot = Offset.Zero)
                }) {
                    drawImage(
                        image = shipBmp,
                        dstOffset = IntOffset(-shipW / 2, offsetTop),
                        dstSize = IntSize(shipW, shipH),
                        alpha = shipAlpha,
                        filterQuality = FilterQuality.None
                    )
                    if (engine.hasShield) {
                        drawCircle(
                            color = Color(0xFF22D3EE).copy(alpha = 0.14f),
                            radius = 24f * scale,
                            center = Offset.Zero
                        )
                        drawCircle(
                            color = Color(0xFF22D3EE).copy(alpha = 0.88f),
                            radius = 24f * scale,
                            center = Offset.Zero,
                            style = Stroke(width = 2f * scale)
                        )
                    }
                }
            }

            // 5. Shockwave Rings & Debris Particles
            for (sw in shockwaves) {
                val progress = (sw.age / sw.duration).coerceIn(0f, 1f)
                val currentScale = 1f + (sw.targetScale - 1f) * progress
                val alpha = (1f - progress) * 0.92f
                val center = toScreen(sw.x, sw.y)
                drawCircle(
                    color = sw.color.copy(alpha = alpha),
                    radius = sw.baseRadius * currentScale * scale,
                    center = center,
                    style = Stroke(width = sw.lineWidth * scale)
                )
            }

            for (d in debris) {
                val progress = (d.age / d.duration).coerceIn(0f, 1f)
                val alpha = (1f - progress).coerceIn(0f, 1f)
                val p = toScreen(d.x, d.y)
                val dw = d.w * scale
                val dh = d.h * scale
                withTransform({
                    translate(left = p.x, top = p.y)
                    rotate(degrees = d.rotation, pivot = Offset.Zero)
                }) {
                    drawRect(
                        color = d.color.copy(alpha = alpha),
                        topLeft = Offset(-dw * 0.5f, -dh * 0.5f),
                        size = Size(dw, dh)
                    )
                }
            }

            // 6. Floating Callout Texts
            for (ft in floatingTexts) {
                val p = toScreen(ft.x, ft.y)
                val alpha = (1f - ft.age / ft.duration).coerceIn(0f, 1f)
                val layout = textMeasurer.measure(
                    text = ft.text,
                    style = TextStyle(
                        color = ft.color.copy(alpha = alpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(p.x - layout.size.width * 0.5f, p.y - layout.size.height * 0.5f)
                )
            }

            // 7. Impact Flash Overlay
            if (impactFlashAlpha > 0f) {
                drawRect(
                    color = Color(0xFFFF2E93).copy(alpha = impactFlashAlpha),
                    topLeft = Offset.Zero,
                    size = size
                )
            }
        }

        // HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpCircleIconButton(
                    symbol = "xmark",
                    size = 36.dp,
                    iconSize = 14.dp,
                    onClick = {
                        HapticsManager.shared.selection()
                        handleExit()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Survival Scoring Rule Pill (3s = 1pt)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFFFF2E93).copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "timer",
                        tint = Color(0xFFFF2E93),
                        size = 11.dp
                    )
                    Text(
                        text = LanguageManager.shared.tr("3 SEC • 1 PT", "3 SEC • 1 PT", "3 SEG • 1 PT"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CheerUpColors.lightGrey,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                CheerUpCircleIconButton(
                    symbol = "pause.fill",
                    size = 36.dp,
                    iconSize = 14.dp,
                    onClick = {
                        HapticsManager.shared.selection()
                        if (!isGameOver && !isCrashing) {
                            isPaused = true
                            RushAudioController.shared.pauseBackgroundMusic()
                        }
                    }
                )
            }

            // MiniCheerUp! Top-Left Mascot Stage + Score Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpMascotView(mood = mascotMood, size = 52.dp)

                val bubbleShape = remember { SpeechBubbleShape() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .shadow(5.dp, bubbleShape)
                        .clip(bubbleShape)
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = speechText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.deepNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val scoreShape = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .clip(scoreShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                        .border(1.5.dp, Color(0xFFFF2E93).copy(alpha = 0.55f), scoreShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LanguageManager.shared.tr("SCORE", "SCORE", "PUNTOS"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF2E93)
                    )
                    Text(
                        text = "$score",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Surprise Event / Shield Save Callout Pill
            bannerText?.let { banner ->
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF090D16).copy(alpha = 0.92f))
                        .border(1.5.dp, bannerColor.copy(alpha = 0.75f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "bolt.fill",
                        tint = bannerColor,
                        size = 12.dp
                    )
                    Text(
                        text = banner,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = bannerColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showDragHint && !isPaused && !isGameOver) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF090D16).copy(alpha = 0.82f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "hand.point.up.left.fill",
                        tint = CheerUpColors.lightGrey.copy(alpha = 0.9f),
                        size = 13.dp
                    )
                    Text(
                        text = LanguageManager.shared.tr(
                            "Touch and drag anywhere to pilot your ship",
                            "Touchez et glissez pour piloter votre vaisseau",
                            "Toca y desliza para pilotar tu nave"
                        ),
                        style = CheerUpFont.caption,
                        color = CheerUpColors.lightGrey.copy(alpha = 0.9f)
                    )
                }
            }

            // Bottom Active Power-Ups Bar
            if (activeEffectsUI.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeEffectsUI.forEach { effect ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                                .border(1.5.dp, effect.type.color.copy(alpha = 0.65f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(effect.type.color)
                            )
                            Text(
                                text = effect.type.badgeTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width((32f * effect.progress.toFloat()).coerceAtLeast(2f).dp)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(effect.type.color)
                                )
                            }
                            Text(
                                text = "${max(1, ceil(effect.remaining).toInt())}s",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CheerUpColors.lightGrey
                            )
                        }
                    }
                }
            }
        }

        // Pause Modal Overlay
        if (isPaused && !isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenPadding)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(CheerUpColors.darkBlue)
                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.5f), cardShape)
                        .padding(Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    Text(
                        text = LanguageManager.shared.tr("PAUSED", "PAUSE", "PAUSA"),
                        style = CheerUpFont.title,
                        color = CheerUpColors.lightGrey
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CheerUpPrimaryButton(
                            title = LanguageManager.shared.tr("Resume", "Reprendre", "Continuar"),
                            icon = "play.fill",
                            action = {
                                HapticsManager.shared.selection()
                                isPaused = false
                                RushAudioController.shared.startBackgroundMusic(context)
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("Restart", "Recommencer", "Reiniciar"),
                            icon = "arrow.clockwise",
                            action = {
                                HapticsManager.shared.selection()
                                startNewGame()
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("Arcade", "Arcade", "Arcade"),
                            icon = "square.grid.2x2.fill",
                            action = {
                                HapticsManager.shared.selection()
                                handleExit()
                            }
                        )
                    }
                }
            }
        }

        // Game Over Screen
        if (isGameOver) {
            GameResultView(
                gameTitle = "RUSH",
                score = score,
                highScore = highScore,
                accentColor = Color(0xFFFF2E93),
                onReplay = { startNewGame() },
                onExit = ::handleExit,
                onWatchAdContinue = if (hasUsedRewardedContinue) null else {
                    { continueAfterReward() }
                }
            )
        }
    }
}

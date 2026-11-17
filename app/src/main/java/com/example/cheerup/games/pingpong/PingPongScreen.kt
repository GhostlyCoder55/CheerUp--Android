package com.example.cheerup.games.pingpong

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.games.pingpong.PingPongPixelArt.drawPowerUpPixelIcon
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class PingPongSpark(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    var age: Float = 0f,
    val duration: Float = 0.28f
)

private data class PingPongFloatingLabel(
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var age: Float = 0f,
    val duration: Float = 0.55f
)

@Composable
fun PingPongScreen(
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val engine = remember { PingPongEngine() }
    val textMeasurer = rememberTextMeasurer()

    var mode by remember { mutableStateOf(PingPongMode.ENDLESS) }
    var playerScore by remember { mutableIntStateOf(0) }
    var cpuScore by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(ScoreManager.shared.highScore(GameIdentifier.PING_PONG)) }
    val activePowerUpsUI = remember { mutableStateListOf<ActivePowerUpState>() }

    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var playerWon by remember { mutableStateOf(false) }
    var hasUsedRewardedContinue by remember { mutableStateOf(false) }
    var showDragHint by remember { mutableStateOf(true) }

    var mascotMood by remember { mutableStateOf(MascotMood.HAPPY) }
    var speechText by remember {
        mutableStateOf(
            LanguageManager.shared.tr(
                "Let's rally! Hit power ups on the table!",
                "C'est parti ! Visez les bonus sur la table !",
                "¡Vamos! ¡Golpea los potenciadores!"
            )
        )
    }

    var targetPlayerX by remember { mutableStateOf<Float?>(null) }
    var frameCounter by remember { mutableIntStateOf(0) }
    var shakeTimer by remember { mutableFloatStateOf(0f) }
    var shakeIntensity by remember { mutableFloatStateOf(0f) }

    val trailHistory = remember { ArrayList<Offset>(20) }
    val extraTrailHistory = remember { ArrayList<Offset>(20) }
    val sparks = remember { ArrayList<PingPongSpark>(64) }
    val floatingLabels = remember { ArrayList<PingPongFloatingLabel>(12) }

    var courtOriginX by remember { mutableFloatStateOf(0f) }
    var courtOriginY by remember { mutableFloatStateOf(0f) }
    var courtScale by remember { mutableFloatStateOf(1f) }

    fun announceSpawnedPowerUp(type: PingPongPowerUpType) {
        mascotMood = MascotMood.WOW
        speechText = when (type) {
            PingPongPowerUpType.GIANT_PADDLE -> LanguageManager.shared.tr("Ooh! Giant Paddle spotted!", "Oh ! Raquette Géante en vue !", "¡Mira! ¡Pala Gigante a la vista!")
            PingPongPowerUpType.SPEED_BOOST -> LanguageManager.shared.tr("Speed Boost on the table!", "Turbo Vitesse sur la table !", "¡Turbo Velocidad en la mesa!")
            PingPongPowerUpType.SLOW_MOTION -> LanguageManager.shared.tr("Slow Motion spawned! Grab it!", "Ralenti apparu ! À saisir !", "¡Cámara Lenta! ¡Atrápala!")
            PingPongPowerUpType.STICKY_PADDLE -> LanguageManager.shared.tr("Sticky Paddle! Catch & aim!", "Raquette Aimant ! Visez bien !", "¡Pala Imán! ¡Atrapa y apunta!")
            PingPongPowerUpType.DOUBLE_POINTS -> LanguageManager.shared.tr("2X Points! Hit it for double score!", "Points Doublés ! Visez bien !", "¡Puntos Dobles en juego!")
            PingPongPowerUpType.SHIELD -> LanguageManager.shared.tr("A Shield appeared! Safety net!", "Un Bouclier est apparu !", "¡Apareció un Escudo protector!")
            PingPongPowerUpType.CHAOS_BALL -> LanguageManager.shared.tr("Chaos Ball incoming! Watch out!", "Balle Chaos en vue ! Attention !", "¡Bola Caos en la mesa!")
            PingPongPowerUpType.DOUBLE_BALL -> LanguageManager.shared.tr("Double Ball on the table!", "Double Balle sur la table !", "¡Doble Bola en la mesa!")
        }
    }

    fun announceCollectedPowerUp(type: PingPongPowerUpType) {
        when (type) {
            PingPongPowerUpType.GIANT_PADDLE -> {
                mascotMood = MascotMood.HAPPY
                speechText = LanguageManager.shared.tr("GIANT PADDLE! Nothing gets past us!", "RAQUETTE GÉANTE ! Rien ne passe !", "¡PALA GIGANTE! ¡Nada pasará!")
            }
            PingPongPowerUpType.SPEED_BOOST -> {
                mascotMood = MascotMood.PARTY
                speechText = LanguageManager.shared.tr("SPEED BOOST! Supercharged returns!", "TURBO VITESSE ! Retours ultra rapides !", "¡TURBO ACTIVO! ¡A toda velocidad!")
            }
            PingPongPowerUpType.SLOW_MOTION -> {
                mascotMood = MascotMood.HAPPY
                speechText = LanguageManager.shared.tr("SLOW MOTION! Easy pickings now.", "RALENTI ACTIVÉ ! Tout en douceur.", "¡CÁMARA LENTA! Qué tranquilidad.")
            }
            PingPongPowerUpType.STICKY_PADDLE -> {
                mascotMood = MascotMood.SUS
                speechText = LanguageManager.shared.tr("STICKY PADDLE! Tap to launch!", "AIMANT ACTIVÉ ! Touchez pour lancer !", "¡PALA IMÁN! ¡Toca para lanzar!")
            }
            PingPongPowerUpType.DOUBLE_POINTS -> {
                mascotMood = MascotMood.PARTY
                speechText = LanguageManager.shared.tr("DOUBLE POINTS! Rack 'em up!", "POINTS DOUBLÉS ! Faites grimper le score !", "¡PUNTOS DOBLES! ¡A sumar!")
            }
            PingPongPowerUpType.SHIELD -> {
                mascotMood = MascotMood.HAPPY
                speechText = LanguageManager.shared.tr("SHIELD UP! One free save ready!", "BOUCLIER ACTIF ! Un arrêt garanti !", "¡ESCUDO ACTIVO! ¡Una salvada extra!")
            }
            PingPongPowerUpType.CHAOS_BALL -> {
                mascotMood = MascotMood.DIZZY
                speechText = LanguageManager.shared.tr("CHAOS BALL! Wild bounces ahead!", "BALLE CHAOS ! Rebonds imprévisibles !", "¡BOLA CAOS! ¡Rebotes locos!")
            }
            PingPongPowerUpType.DOUBLE_BALL -> {
                mascotMood = MascotMood.PARTY
                speechText = LanguageManager.shared.tr("DOUBLE BALL! Twice the action!", "DOUBLE BALLE ! Deux fois plus d'action !", "¡DOBLE BOLA! ¡Doble acción!")
            }
        }
    }

    fun spawnSparks(at: Offset, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextDouble(0.0, 2.0 * PI)
            val speed = Random.nextDouble(60.0, 190.0).toFloat()
            sparks.add(
                PingPongSpark(
                    x = at.x,
                    y = at.y,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed,
                    size = Random.nextDouble(4.0, 7.0).toFloat(),
                    color = color
                )
            )
        }
    }

    fun startNewMatch(newMode: PingPongMode = mode) {
        mode = newMode
        engine.reset(newMode)
        playerScore = 0
        cpuScore = 0
        highScore = ScoreManager.shared.highScore(GameIdentifier.PING_PONG)
        activePowerUpsUI.clear()
        trailHistory.clear()
        extraTrailHistory.clear()
        sparks.clear()
        floatingLabels.clear()
        targetPlayerX = null
        isPaused = false
        isGameOver = false
        playerWon = false
        hasUsedRewardedContinue = false
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Let's rally! Hit power ups on the table!",
            "C'est parti ! Visez les bonus sur la table !",
            "¡Vamos! ¡Golpea los potenciadores!"
        )
    }

    fun continueMatchAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        engine.continueAfterReward()
        cpuScore = engine.cpuScore
        trailHistory.clear()
        extraTrailHistory.clear()
        sparks.clear()
        floatingLabels.clear()
        targetPlayerX = null
        isPaused = false
        isGameOver = false
        playerWon = false
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Second chance! Ready to rally!",
            "Seconde chance ! Prêt pour l'échange !",
            "¡Segunda oportunidad! ¡Listo para jugar!"
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (!isGameOver && !isPaused) {
                    isPaused = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        delay(4000L)
        showDragHint = false
    }

    LaunchedEffect(isPaused, isGameOver) {
        if (!isPaused && !isGameOver) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    val dt = if (lastNanos == 0L) 1.0 / 60.0 else ((nanos - lastNanos) / 1_000_000_000.0).coerceIn(0.0, 0.05)
                    lastNanos = nanos

                    val events = engine.update(dt, targetPlayerX)

                    // Update trail
                    if (trailHistory.isNotEmpty()) {
                        val prev = trailHistory.last()
                        if (hypot(engine.ballPos.x - prev.x, engine.ballPos.y - prev.y) > 90f) {
                            trailHistory.clear()
                        }
                    }
                    trailHistory.add(engine.ballPos)
                    if (trailHistory.size > 18) trailHistory.removeAt(0)

                    if (engine.hasExtraBall) {
                        extraTrailHistory.add(engine.extraBallPos)
                        if (extraTrailHistory.size > 18) extraTrailHistory.removeAt(0)
                    } else {
                        extraTrailHistory.clear()
                    }

                    // Update sparks & floating labels
                    val dtF = dt.toFloat()
                    if (shakeTimer > 0f) {
                        shakeTimer = max(0f, shakeTimer - dtF)
                    }
                    for (i in sparks.indices.reversed()) {
                        val sp = sparks[i]
                        sp.age += dtF
                        sp.x += sp.vx * dtF
                        sp.y += sp.vy * dtF
                        if (sp.age >= sp.duration) sparks.removeAt(i)
                    }
                    for (i in floatingLabels.indices.reversed()) {
                        val fl = floatingLabels[i]
                        fl.age += dtF
                        fl.y += 45f * dtF
                        if (fl.age >= fl.duration) floatingLabels.removeAt(i)
                    }

                    // Process events
                    for (ev in events) {
                        when (ev) {
                            is PingPongEvent.PaddleHit -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                                if (ev.isPlayer) {
                                    if (ev.isSmash) {
                                        HapticsManager.shared.mediumImpact()
                                        shakeIntensity = 5f
                                        shakeTimer = 0.14f
                                    } else {
                                        HapticsManager.shared.lightTap()
                                    }
                                }
                                spawnSparks(
                                    ev.position,
                                    if (ev.isPlayer) Color(0xFF00A896) else Color(0xFFFF4B5C),
                                    if (ev.isSmash) 12 else 7
                                )
                            }
                            is PingPongEvent.WallBounce -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.BOUNCE)
                                spawnSparks(ev.position, Color(0xFF9BA8AB), 5)
                            }
                            is PingPongEvent.ShieldSaved -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.SWITCH_TOGGLE)
                                HapticsManager.shared.mediumImpact()
                                shakeIntensity = 6f
                                shakeTimer = 0.16f
                                spawnSparks(ev.position, Color(0xFF00D2FF), 16)
                            }
                            is PingPongEvent.PowerUpSpawned -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.POP)
                                announceSpawnedPowerUp(ev.item.type)
                            }
                            is PingPongEvent.PowerUpCollected -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.NEXT_LEVEL)
                                HapticsManager.shared.mediumImpact()
                                spawnSparks(ev.position, ev.type.color, 16)
                                announceCollectedPowerUp(ev.type)
                            }
                            is PingPongEvent.PlayerScored -> {
                                HapticsManager.shared.lightTap()
                                val clampedY = ev.position.y.coerceIn(70f, PingPongEngine.courtHeight - 50f)
                                floatingLabels.add(
                                    PingPongFloatingLabel(
                                        text = "+${ev.pointsAdded}",
                                        x = ev.position.x,
                                        y = clampedY,
                                        color = Color(0xFF2EDB8A)
                                    )
                                )
                            }
                            is PingPongEvent.CpuScored -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.ERROR)
                                HapticsManager.shared.heavyImpact()
                                shakeIntensity = 8.5f
                                shakeTimer = 0.18f
                                spawnSparks(Offset(ev.position.x, 36f), Color(0xFFFF4B5C), 18)
                            }
                            is PingPongEvent.BallMissedLine -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.POP)
                                spawnSparks(ev.position, Color(0xFFFFC83D), 12)
                            }
                            is PingPongEvent.StickyCaught -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.SLIME_STICK)
                                HapticsManager.shared.lightTap()
                                floatingLabels.add(
                                    PingPongFloatingLabel(
                                        text = "CAUGHT!",
                                        x = ev.position.x,
                                        y = ev.position.y + 18f,
                                        color = Color(0xFFFFC83D)
                                    )
                                )
                            }
                            is PingPongEvent.StickyLaunched -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                                HapticsManager.shared.mediumImpact()
                                spawnSparks(ev.position, Color(0xFFFFC83D), 10)
                            }
                            is PingPongEvent.DifficultyIncreased -> {
                                AudioManager.shared.play(AudioManager.SoundEffect.NEXT_LEVEL)
                            }
                            is PingPongEvent.Callout -> {}
                            is PingPongEvent.GameOver -> {
                                if (ev.isNewBest || ev.playerWon) {
                                    AudioManager.shared.play(AudioManager.SoundEffect.WIN)
                                    HapticsManager.shared.success()
                                } else {
                                    AudioManager.shared.play(AudioManager.SoundEffect.ERROR)
                                    HapticsManager.shared.error()
                                }
                                playerScore = ev.finalScore
                                playerWon = ev.playerWon
                                highScore = ScoreManager.shared.highScore(GameIdentifier.PING_PONG)
                                mascotMood = if (ev.playerWon || ev.isNewBest) MascotMood.PARTY else MascotMood.SAD
                                speechText = if (ev.playerWon || ev.isNewBest) {
                                    LanguageManager.shared.tr("New record! You're a table legend!", "Nouveau record ! Quelle légende !", "¡Nuevo récord! ¡Increíble!")
                                } else {
                                    LanguageManager.shared.tr("Good rally! One more match?", "Bel échange ! On rejoue ?", "¡Buen partido! ¿Otra vez?")
                                }
                                isGameOver = true
                            }
                        }
                    }

                    if (playerScore != engine.playerScore) {
                        playerScore = engine.playerScore
                        if (playerScore > highScore) {
                            highScore = playerScore
                            ScoreManager.shared.updateHighScore(GameIdentifier.PING_PONG, playerScore)
                        }
                    }
                    if (cpuScore != engine.cpuScore) {
                        cpuScore = engine.cpuScore
                    }
                    activePowerUpsUI.clear()
                    activePowerUpsUI.addAll(engine.activePowerUps.map { it.copy() })

                    frameCounter += 1
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
            .systemBarsPadding()
    ) {
        // 60/120 FPS Table Canvas + One-Handed Horizontal Drag Input
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPaused, isGameOver) {
                    if (isPaused || isGameOver) return@pointerInput
                    awaitEachGesture {
                        val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                        val scale = if (courtScale > 0f) courtScale else 1f
                        targetPlayerX = (down.position.x - courtOriginX) / scale
                        if (engine.isBallStuckToPlayer) {
                            engine.releaseStuckBall()
                            AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                            HapticsManager.shared.mediumImpact()
                        }
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                targetPlayerX = (change.position.x - courtOriginX) / scale
                            } else {
                                if (engine.isBallStuckToPlayer) {
                                    engine.releaseStuckBall()
                                    AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                                    HapticsManager.shared.mediumImpact()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val _frame = frameCounter
            val topReserved = 136.dp.toPx()
            val bottomReserved = 56.dp.toPx()
            val availableH = max(300f, size.height - topReserved - bottomReserved)
            val availableW = max(260f, size.width - 12.dp.toPx())

            val cw = PingPongEngine.courtWidth
            val ch = PingPongEngine.courtHeight
            val scale = min(availableW / cw, availableH / ch)
            courtScale = scale

            val scaledW = cw * scale
            val scaledH = ch * scale
            val baseOriginX = (size.width - scaledW) * 0.5f
            val baseOriginY = topReserved + (availableH - scaledH) * 0.5f
            courtOriginX = baseOriginX
            courtOriginY = baseOriginY

            val shakeX = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 2f * shakeIntensity * scale else 0f
            val shakeY = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 2f * shakeIntensity * scale else 0f
            val ox = baseOriginX + shakeX
            val oy = baseOriginY + shakeY

            // Helper to map court (x, y) [bottom-up] to screen Canvas (x, y) [top-down]
            fun toScreen(cx: Float, cy: Float): Offset =
                Offset(ox + cx * scale, oy + (ch - cy) * scale)

            val inset = PingPongEngine.wallInset

            // Outer Table Shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(ox + (inset - 4f) * scale, oy + 30f * scale),
                size = Size((cw - (inset - 4f) * 2f) * scale, (ch - 48f) * scale),
                cornerRadius = CornerRadius(18f * scale, 18f * scale)
            )

            // Main Table Surface (#11212D)
            val tableTopLeft = Offset(ox + inset * scale, oy + 24f * scale)
            val tableSize = Size((cw - inset * 2f) * scale, (ch - 48f) * scale)
            drawRoundRect(
                color = Color(0xFF11212D),
                topLeft = tableTopLeft,
                size = tableSize,
                cornerRadius = CornerRadius(16f * scale, 16f * scale)
            )
            drawRoundRect(
                color = Color(0xFF253745),
                topLeft = tableTopLeft,
                size = tableSize,
                cornerRadius = CornerRadius(16f * scale, 16f * scale),
                style = Stroke(width = 4f * scale)
            )

            // Center vertical line
            drawLine(
                color = Color(0xFF253745).copy(alpha = 0.55f),
                start = toScreen(cw * 0.5f, 36f),
                end = toScreen(cw * 0.5f, ch - 36f),
                strokeWidth = 2f * scale
            )

            // Left & right bumper rails
            for (xPos in listOf(inset, cw - inset)) {
                drawLine(
                    color = Color(0xFF4A5C6A).copy(alpha = 0.85f),
                    start = toScreen(xPos, 44f),
                    end = toScreen(xPos, ch - 44f),
                    strokeWidth = 3.5f * scale
                )
            }

            // Horizontal Dashed Net at center Y
            val dashW = 10f
            val gapW = 6f
            var curX = inset + 8f
            while (curX < cw - inset - 8f) {
                val p = toScreen(curX, PingPongEngine.netY + 2f)
                drawRect(
                    color = Color(0xFF9BA8AB).copy(alpha = 0.7f),
                    topLeft = p,
                    size = Size(dashW * scale, 4f * scale)
                )
                curX += dashW + gapW
            }

            // Net posts
            for (postX in listOf(inset - 2f, cw - inset + 2f)) {
                val p = toScreen(postX - 4f, PingPongEngine.netY + 7f)
                drawRect(
                    color = Color(0xFFCCD0CF),
                    topLeft = p,
                    size = Size(8f * scale, 14f * scale)
                )
            }

            // Shield Bottom Energy Barrier
            if (engine.hasShieldActive) {
                drawLine(
                    color = Color(0xFF00D2FF).copy(alpha = 0.9f),
                    start = toScreen(inset + 6f, 34f),
                    end = toScreen(cw - inset - 6f, 34f),
                    strokeWidth = 4.5f * scale
                )
            }

            // Spawned Field Power-Ups
            for (item in engine.spawnedPowerUps) {
                val remaining = item.lifetime - item.age
                val alpha = if (remaining < 2.0 && ((remaining * 8.0).toInt() % 2 == 0)) 0.45f else 1.0f
                val center = toScreen(item.position.x, item.position.y)
                val r = item.radius * scale
                drawCircle(
                    color = Color(0xFF06141B).copy(alpha = 0.92f * alpha),
                    radius = r,
                    center = center
                )
                drawCircle(
                    color = item.type.color.copy(alpha = alpha),
                    radius = r,
                    center = center,
                    style = Stroke(width = 2.5f * scale)
                )
                drawPowerUpPixelIcon(
                    type = item.type,
                    center = center,
                    iconSize = 22f * scale,
                    alpha = alpha
                )
            }

            // CPU Opponent Paddle (#FF4B5C with tracking pixel eyes)
            val cpuW = PingPongEngine.cpuPaddleWidth
            val padH = PingPongEngine.paddleHeight
            val cpuTopLeft = toScreen(engine.cpuX - cpuW * 0.5f, PingPongEngine.cpuY + padH * 0.5f)
            val cpuSize = Size(cpuW * scale, padH * scale)
            drawRoundRect(
                color = Color(0xFFFF4B5C),
                topLeft = cpuTopLeft,
                size = cpuSize,
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = cpuTopLeft,
                size = cpuSize,
                cornerRadius = CornerRadius(6f * scale, 6f * scale),
                style = Stroke(width = 1.5f * scale)
            )
            val eyeShift = max(-3f, min(3f, (engine.ballPos.x - engine.cpuX) * 0.04f))
            val cpuCenter = toScreen(engine.cpuX, PingPongEngine.cpuY)
            drawRect(
                color = Color.Black,
                topLeft = Offset(cpuCenter.x + (-8f + eyeShift - 2f) * scale, cpuCenter.y - 2f * scale),
                size = Size(4f * scale, 6f * scale)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(cpuCenter.x + (8f + eyeShift - 2f) * scale, cpuCenter.y - 2f * scale),
                size = Size(4f * scale, 6f * scale)
            )

            // Player Paddle (#00A896 or #2EDB8A when Giant)
            val pW = engine.currentPlayerPaddleWidth
            val pTopLeft = toScreen(engine.playerX - pW * 0.5f, PingPongEngine.playerY + padH * 0.5f)
            val pSize = Size(pW * scale, padH * scale)
            val paddleColor = if (pW > PingPongEngine.basePaddleWidth) Color(0xFF2EDB8A) else Color(0xFF00A896)
            drawRoundRect(
                color = paddleColor,
                topLeft = pTopLeft,
                size = pSize,
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = pTopLeft,
                size = pSize,
                cornerRadius = CornerRadius(6f * scale, 6f * scale),
                style = Stroke(width = 1.5f * scale)
            )
            // Sticky Paddle Gold Strip
            if (engine.isPowerUpActive(PingPongPowerUpType.STICKY_PADDLE)) {
                drawRoundRect(
                    color = Color(0xFFFFC83D),
                    topLeft = Offset(pTopLeft.x + pSize.width * 0.04f, pTopLeft.y),
                    size = Size(pSize.width * 0.92f, 4f * scale),
                    cornerRadius = CornerRadius(2f * scale, 2f * scale)
                )
            }

            // Ball Color based on active power-up
            val ballColor = when {
                engine.isChaosBall -> Color(0xFFA855F7)
                engine.isPowerUpActive(PingPongPowerUpType.SPEED_BOOST) -> Color(0xFFFF4B5C)
                engine.isPowerUpActive(PingPongPowerUpType.SLOW_MOTION) -> Color(0xFF3D7BFF)
                else -> Color(0xFFF5FAFF)
            }

            // Ball Trails
            val ballSpeed = hypot(engine.ballVel.x, engine.ballVel.y)
            if (ballSpeed > 280f && !engine.isGameOver && engine.serveDelayTimer <= 0) {
                for (i in 0 until 6) {
                    val idx = trailHistory.size - 1 - (i + 1) * 2
                    if (idx >= 0) {
                        val tp = toScreen(trailHistory[idx].x, trailHistory[idx].y)
                        val trailR = PingPongEngine.ballRadius * scale * (1f - i * 0.11f)
                        val alpha = max(0.06f, 0.34f - i * 0.05f)
                        drawCircle(color = ballColor.copy(alpha = alpha), radius = trailR, center = tp)
                    }
                }
            }

            // Main Ball
            val ballScreenPos = toScreen(engine.ballPos.x, engine.ballPos.y)
            val bRad = PingPongEngine.ballRadius * scale
            drawCircle(color = ballColor, radius = bRad, center = ballScreenPos)
            drawCircle(
                color = Color.White,
                radius = bRad * 0.42f,
                center = ballScreenPos + Offset(-bRad * 0.25f, -bRad * 0.25f)
            )

            // Extra Ball (Double Ball Power-Up)
            if (engine.hasExtraBall && !engine.isGameOver && engine.serveDelayTimer <= 0) {
                val extraSpeed = hypot(engine.extraBallVel.x, engine.extraBallVel.y)
                if (extraSpeed > 280f) {
                    for (i in 0 until 6) {
                        val idx = extraTrailHistory.size - 1 - (i + 1) * 2
                        if (idx >= 0) {
                            val tp = toScreen(extraTrailHistory[idx].x, extraTrailHistory[idx].y)
                            val trailR = PingPongEngine.ballRadius * scale * (1f - i * 0.11f)
                            val alpha = max(0.06f, 0.34f - i * 0.05f)
                            drawCircle(color = ballColor.copy(alpha = alpha), radius = trailR, center = tp)
                        }
                    }
                }
                val extraScreenPos = toScreen(engine.extraBallPos.x, engine.extraBallPos.y)
                drawCircle(color = ballColor, radius = bRad, center = extraScreenPos)
                drawCircle(
                    color = Color.White,
                    radius = bRad * 0.42f,
                    center = extraScreenPos + Offset(-bRad * 0.25f, -bRad * 0.25f)
                )
            }

            // Sparks
            for (sp in sparks) {
                val p = toScreen(sp.x, sp.y)
                val alpha = (1f - sp.age / sp.duration).coerceIn(0f, 1f)
                val s = sp.size * scale * alpha
                drawRect(
                    color = sp.color.copy(alpha = alpha),
                    topLeft = Offset(p.x - s * 0.5f, p.y - s * 0.5f),
                    size = Size(s, s)
                )
            }

            // Floating Score Labels
            for (fl in floatingLabels) {
                val p = toScreen(fl.x, fl.y)
                val alpha = (1f - fl.age / fl.duration).coerceIn(0f, 1f)
                val layout = textMeasurer.measure(
                    text = fl.text,
                    style = TextStyle(
                        color = fl.color.copy(alpha = alpha),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(p.x - layout.size.width * 0.5f, p.y - layout.size.height * 0.5f)
                )
            }
        }

        // Top & Bottom HUD Overlay
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
                        onExit()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Mode Switcher Pill (Endless / Classic)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CheerUpColors.darkBlue.copy(alpha = 0.9f))
                        .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.6f), CircleShape)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PingPongMode.entries.forEach { modeItem ->
                        val isSelected = mode == modeItem
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) CheerUpColors.lightGrey else Color.Transparent)
                                .clickable {
                                    if (modeItem != mode) {
                                        HapticsManager.shared.selection()
                                        AudioManager.shared.play(AudioManager.SoundEffect.SWITCH_TOGGLE)
                                        startNewMatch(modeItem)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (modeItem == PingPongMode.ENDLESS) {
                                    LanguageManager.shared.tr("ENDLESS", "SANS FIN", "SIN FIN")
                                } else {
                                    LanguageManager.shared.tr("CLASSIC", "CLASSIQUE", "CLÁSICO")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) CheerUpColors.deepNavy else CheerUpColors.mutedSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                CheerUpCircleIconButton(
                    symbol = "pause.fill",
                    size = 36.dp,
                    iconSize = 14.dp,
                    onClick = {
                        HapticsManager.shared.selection()
                        if (!isGameOver) isPaused = true
                    }
                )
            }

            // MiniCheerUp! Mascot Stage + Compact Scoreboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpMascotView(mood = mascotMood, size = 54.dp)

                val bubbleShape = remember { SpeechBubbleShape() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .shadow(5.dp, bubbleShape)
                        .clip(bubbleShape)
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = speechText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.deepNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val scoreShape = RoundedCornerShape(14.dp)
                Row(
                    modifier = Modifier
                        .clip(scoreShape)
                        .background(CheerUpColors.darkBlue.copy(alpha = 0.92f))
                        .border(1.5.dp, Color(0xFF00A896).copy(alpha = 0.45f), scoreShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = LanguageManager.shared.tr("SCORE", "SCORE", "PUNTOS"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00A896)
                        )
                        Text(
                            text = "$playerScore",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    if (mode == PingPongMode.CLASSIC) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(CheerUpColors.blueGrey)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CPU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF4B5C)
                            )
                            Text(
                                text = "$cpuScore",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = CheerUpColors.lightGrey
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showDragHint && playerScore == 0 && !isPaused && !isGameOver) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                        .clip(CircleShape)
                        .background(CheerUpColors.deepNavy.copy(alpha = 0.75f))
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
                            "Drag horizontally to move paddle",
                            "Glissez pour bouger la raquette",
                            "Desliza para mover la pala"
                        ),
                        style = CheerUpFont.caption,
                        color = CheerUpColors.lightGrey.copy(alpha = 0.9f)
                    )
                }
            }

            // Active Power-Ups Bar at Bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activePowerUpsUI.forEach { item ->
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(CheerUpColors.darkBlue.copy(alpha = 0.92f))
                            .border(1.5.dp, item.type.color.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawPowerUpPixelIcon(
                                type = item.type,
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                iconSize = size.width
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width((40f * item.progress.toFloat()).coerceAtLeast(2f).dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(item.type.color)
                            )
                        }

                        Text(
                            text = "${ceil(item.remaining).toInt()}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CheerUpColors.lightGrey
                        )
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
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("Restart", "Recommencer", "Reiniciar"),
                            icon = "arrow.clockwise",
                            action = {
                                HapticsManager.shared.selection()
                                startNewMatch()
                            }
                        )
                        CheerUpSecondaryButton(
                            title = LanguageManager.shared.tr("Arcade", "Arcade", "Arcade"),
                            icon = "square.grid.2x2.fill",
                            action = {
                                HapticsManager.shared.selection()
                                onExit()
                            }
                        )
                    }
                }
            }
        }

        // Game Over Result Overlay
        if (isGameOver) {
            GameResultView(
                gameTitle = "PING PONG",
                score = playerScore,
                highScore = highScore,
                accentColor = Color(0xFF00A896),
                onReplay = { startNewMatch() },
                onExit = onExit,
                onWatchAdContinue = if (playerWon || hasUsedRewardedContinue) null else {
                    { continueMatchAfterReward() }
                }
            )
        }
    }
}

package com.example.cheerup.games.gravity

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.games.gravity.GravityPixelArt.drawMoteSprite
import com.example.cheerup.games.gravity.GravityPixelArt.drawSerpentBody
import com.example.cheerup.games.gravity.GravityPixelArt.drawSerpentHead
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class GravityParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    var age: Float = 0f,
    val duration: Float = 0.28f
)

private const val GRAVITY_TUTORIAL_SEEN_KEY = "cheerup.gravity.tutorialSeen"

@Composable
fun GravityScreen(
    onExit: () -> Unit
) {
    val context = LocalContext.current

    fun handleExit() {
        GravityAudioController.shared.stopBackgroundMusic()
        onExit()
    }

    BackHandler(onBack = ::handleExit)

    val engine = remember { GravityEngine() }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(ScoreManager.shared.highScore(GameIdentifier.GRAVITY)) }
    var orientation by remember { mutableStateOf(GravityOrientation.SOUTH) }
    val activeEffectsUI = remember { mutableStateListOf<ActiveElementalEffect>() }

    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var hasUsedRewardedContinue by remember { mutableStateOf(false) }

    var mascotMood by remember { mutableStateOf(MascotMood.HAPPY) }
    var speechText by remember {
        mutableStateOf(
            LanguageManager.shared.tr(
                "Tap the arrows below! Crossing glowing edges rotates gravity!",
                "Touchez les flèches en bas ! Franchir un bord tourne le monde !",
                "¡Toca las flechas abajo! ¡Cruzar bordes gira la gravedad!"
            )
        )
    }

    val particles = remember { ArrayList<GravityParticle>(48) }
    val boardRotationAnim = remember { Animatable(0f) }
    var frameCounter by remember { mutableIntStateOf(0) }

    fun spawnElementalBurst(coord: GridCoord, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextDouble(0.0, 2.0 * PI)
            val speed = Random.nextDouble(2.5, 6.5).toFloat()
            particles.add(
                GravityParticle(
                    x = coord.x + 0.5f,
                    y = coord.y + 0.5f,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed,
                    size = Random.nextDouble(0.22, 0.36).toFloat(),
                    color = color
                )
            )
        }
    }

    fun startNewGame() {
        engine.reset()
        score = 0
        highScore = ScoreManager.shared.highScore(GameIdentifier.GRAVITY)
        orientation = GravityOrientation.SOUTH
        activeEffectsUI.clear()
        particles.clear()
        isPaused = false
        isGameOver = false
        hasUsedRewardedContinue = false
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Tap the arrows below! Crossing glowing edges rotates gravity!",
            "Touchez les flèches en bas ! Franchir un bord tourne le monde !",
            "¡Toca las flechas abajo! ¡Cruzar bordes gira la gravedad!"
        )
        GravityAudioController.shared.startBackgroundMusic(context)
    }

    fun continueAfterReward() {
        if (hasUsedRewardedContinue) return
        hasUsedRewardedContinue = true
        engine.continueAfterReward()
        isPaused = false
        isGameOver = false
        mascotMood = MascotMood.HAPPY
        speechText = LanguageManager.shared.tr(
            "Second chance! Guide the serpent!",
            "Seconde chance ! Guidez le serpent !",
            "¡Segunda oportunidad! ¡Guía a la serpiente!"
        )
        GravityAudioController.shared.startBackgroundMusic(context)
    }

    LaunchedEffect(Unit) {
        GravityAudioController.shared.initializeIfNeeded(context)
        val sp = context.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        if (!sp.getBoolean(GRAVITY_TUTORIAL_SEEN_KEY, false)) {
            showTutorial = true
            isPaused = true
        } else {
            startNewGame()
        }
    }

    LaunchedEffect(orientation) {
        val target = orientation.canvasRotationDegrees
        val current = boardRotationAnim.value
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        boardRotationAnim.animateTo(
            targetValue = current + diff,
            animationSpec = tween(durationMillis = 420)
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (!isGameOver && !isPaused) {
                        isPaused = true
                        GravityAudioController.shared.pauseBackgroundMusic()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!RewardedAdManager.shared.isPresentingAd) {
                GravityAudioController.shared.stopBackgroundMusic()
            }
        }
    }

    LaunchedEffect(isPaused, isGameOver, showTutorial) {
        if (!isPaused && !isGameOver && !showTutorial) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    val dt = if (lastNanos == 0L) 1.0 / 60.0 else ((nanos - lastNanos) / 1_000_000_000.0).coerceIn(0.0, 0.05)
                    lastNanos = nanos

                    val events = engine.update(dt)
                    val dtF = dt.toFloat()
                    for (i in particles.indices.reversed()) {
                        val p = particles[i]
                        p.age += dtF
                        p.x += p.vx * dtF
                        p.y += p.vy * dtF
                        if (p.age >= p.duration) particles.removeAt(i)
                    }

                    for (ev in events) {
                        when (ev) {
                            GravityEvent.Moved -> {}
                            is GravityEvent.MoteCollected -> {
                                GravityAudioController.shared.playMoteSound(ev.type)
                                HapticsManager.shared.lightTap()
                                spawnElementalBurst(ev.coord, ev.type.color, 14)
                                when (ev.type) {
                                    ElementalMoteType.FIRE -> {
                                        mascotMood = MascotMood.PARTY
                                        speechText = LanguageManager.shared.tr(
                                            "Speed boost & double points! (+${ev.pointsAdded})",
                                            "Vitesse + points doublés ! (+${ev.pointsAdded})",
                                            "¡Velocidad y puntos dobles! (+${ev.pointsAdded})"
                                        )
                                    }
                                    ElementalMoteType.WATER -> {
                                        mascotMood = MascotMood.HAPPY
                                        speechText = LanguageManager.shared.tr(
                                            "Calm, slower movement. (+${ev.pointsAdded})",
                                            "Mouvement calme et ralenti. (+${ev.pointsAdded})",
                                            "¡Movimiento lento y preciso! (+${ev.pointsAdded})"
                                        )
                                    }
                                    ElementalMoteType.EARTH -> {
                                        mascotMood = MascotMood.HAPPY
                                        speechText = LanguageManager.shared.tr(
                                            "Yum! +1 length. (+${ev.pointsAdded})",
                                            "Miam ! +1 longueur. (+${ev.pointsAdded})",
                                            "¡Bien! +1 de largo. (+${ev.pointsAdded})"
                                        )
                                    }
                                    ElementalMoteType.WIND -> {
                                        mascotMood = MascotMood.SUS
                                        speechText = LanguageManager.shared.tr(
                                            "Pass safely through your tail!",
                                            "Traversez votre propre corps !",
                                            "¡Atraviesa tu propia cola!"
                                        )
                                    }
                                }
                            }
                            is GravityEvent.GravityShifted -> {
                                GravityAudioController.shared.playGravityShiftSound()
                                HapticsManager.shared.mediumImpact()
                                orientation = ev.newOrientation
                            }
                            is GravityEvent.WindPhasedThroughSelf -> {
                                HapticsManager.shared.lightTap()
                                spawnElementalBurst(ev.coord, ElementalMoteType.WIND.color, 12)
                                mascotMood = MascotMood.PARTY
                                speechText = LanguageManager.shared.tr(
                                    "Ghosted right through your tail!",
                                    "Vous avez traversé votre queue !",
                                    "¡Atravesaste tu propia cola!"
                                )
                            }
                            is GravityEvent.GameOver -> {
                                GravityAudioController.shared.stopBackgroundMusic()
                                if (ev.isNewBest) {
                                    AudioManager.shared.play(AudioManager.SoundEffect.WIN)
                                    HapticsManager.shared.success()
                                } else {
                                    AudioManager.shared.play(AudioManager.SoundEffect.ERROR)
                                    HapticsManager.shared.heavyImpact()
                                }
                                engine.segments.firstOrNull()?.let { head ->
                                    spawnElementalBurst(head, Color(0xFFF59E0B), 22)
                                }
                                score = ev.finalScore
                                highScore = ScoreManager.shared.highScore(GameIdentifier.GRAVITY)
                                mascotMood = if (ev.isNewBest) MascotMood.PARTY else MascotMood.SAD
                                speechText = if (ev.isNewBest) {
                                    LanguageManager.shared.tr("New record in the enchanted forest!", "Nouveau record dans la forêt enchantée !", "¡Nuevo récord en el bosque encantado!")
                                } else {
                                    LanguageManager.shared.tr("Tangled in the roots! Try again?", "Emmêlé dans les racines ! On rejoue ?", "¡Enredado! ¿Otra partida?")
                                }
                                isGameOver = true
                            }
                        }
                    }

                    if (score != engine.score) {
                        score = engine.score
                        if (score > highScore) {
                            highScore = score
                            ScoreManager.shared.updateHighScore(GameIdentifier.GRAVITY, score)
                        }
                    }
                    if (orientation != engine.orientation) {
                        orientation = engine.orientation
                    }
                    activeEffectsUI.clear()
                    activeEffectsUI.addAll(engine.activeEffects.map { it.copy() })

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
                        Color(0xFF10281C),
                        Color(0xFF091912),
                        Color(0xFF050E0A)
                    )
                )
            )
            .systemBarsPadding()
    ) {
        // Enchanted Torus Board Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val _frame = frameCounter
            val topReserved = 166.dp.toPx()
            val bottomReserved = 212.dp.toPx()
            val availableH = max(200f, size.height - topReserved - bottomReserved)
            val availableW = max(200f, size.width - 28.dp.toPx())

            val fitSide = min(availableW, availableH) * 0.94f
            val cols = GravityEngine.gridColumns
            val rows = GravityEngine.gridRows
            val tileSize = fitSide / cols
            val boardExtent = cols * tileSize

            val pivotX = size.width * 0.5f
            val pivotY = topReserved + availableH * 0.5f

            withTransform({
                translate(left = pivotX, top = pivotY)
                rotate(degrees = boardRotationAnim.value, pivot = Offset.Zero)
            }) {
                val half = boardExtent * 0.5f
                val boardTopLeft = Offset(-half, -half)

                // Helper to map grid (gx, gy) [y=0 at bottom, y=16 at top] to local canvas offset around (0,0)
                fun gridCenterToLocal(gx: Float, gy: Float): Offset =
                    Offset(
                        x = -half + (gx + 0.5f) * tileSize,
                        y = half - (gy + 0.5f) * tileSize
                    )

                // Outer frame shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.48f),
                    topLeft = Offset(-half - 8.dp.toPx(), -half - 8.dp.toPx()),
                    size = Size(boardExtent + 16.dp.toPx(), boardExtent + 16.dp.toPx()),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )

                // Checkerboard deep moss & earthy forest floor tiles
                val tileDark = Color(0xFF0B1E15)
                val tileLight = Color(0xFF10281C)
                drawRect(
                    color = tileDark,
                    topLeft = boardTopLeft,
                    size = Size(boardExtent, boardExtent)
                )

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if ((r + c) % 2 == 1) {
                            drawRect(
                                color = tileLight,
                                topLeft = Offset(
                                    x = -half + c * tileSize,
                                    y = half - (r + 1) * tileSize
                                ),
                                size = Size(tileSize, tileSize)
                            )
                        }
                    }
                }

                // Glowing Torus Portal Border Frame
                drawRoundRect(
                    color = Color(0xFF3E2C1C),
                    topLeft = Offset(-half - 2.dp.toPx(), -half - 2.dp.toPx()),
                    size = Size(boardExtent + 4.dp.toPx(), boardExtent + 4.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )

                // Glowing rune dashes along the 4 torus wrap edges
                val dashCount = 9
                val step = boardExtent / dashCount
                for (i in 0 until dashCount step 2) {
                    val offset = -half + i * step + step * 0.22f
                    val len = step * 0.55f
                    // Left & Right gravity shift runes (Cyan)
                    drawRect(
                        color = Color(0xFF22D3EE).copy(alpha = 0.55f),
                        topLeft = Offset(-half, offset),
                        size = Size(3.dp.toPx(), len)
                    )
                    drawRect(
                        color = Color(0xFF22D3EE).copy(alpha = 0.55f),
                        topLeft = Offset(half - 3.dp.toPx(), offset),
                        size = Size(3.dp.toPx(), len)
                    )
                    // Top & Bottom gravity shift runes (Purple)
                    drawRect(
                        color = Color(0xFFA855F7).copy(alpha = 0.55f),
                        topLeft = Offset(offset, -half),
                        size = Size(len, 3.dp.toPx())
                    )
                    drawRect(
                        color = Color(0xFFA855F7).copy(alpha = 0.55f),
                        topLeft = Offset(offset, half - 3.dp.toPx()),
                        size = Size(len, 3.dp.toPx())
                    )
                }

                // Elemental Motes
                for (mote in engine.spawnedMotes) {
                    val center = gridCenterToLocal(mote.coord.x.toFloat(), mote.coord.y.toFloat())
                    drawCircle(
                        color = mote.type.color.copy(alpha = 0.22f),
                        radius = tileSize * 0.52f,
                        center = center
                    )
                    drawCircle(
                        color = mote.type.color.copy(alpha = 0.65f),
                        radius = tileSize * 0.52f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawMoteSprite(
                        type = mote.type,
                        center = center,
                        spriteSize = tileSize * 0.95f
                    )
                }

                // Serpent Segments (interpolated smoothly unless wrapping across torus boundary)
                val segs = engine.segments
                val prevSegs = engine.previousSegments
                val t = engine.stepProgress
                val windActive = engine.isWindPhaseActive
                val fireActive = engine.isFireBoostActive
                val waterActive = engine.isWaterCalmActive

                for (i in segs.indices.reversed()) {
                    val cur = segs[i]
                    val prev = if (i < prevSegs.size) prevSegs[i] else cur
                    val dx = abs(cur.x - prev.x)
                    val dy = abs(cur.y - prev.y)
                    val pos = if (dx <= 1 && dy <= 1) {
                        val ix = prev.x + (cur.x - prev.x) * t
                        val iy = prev.y + (cur.y - prev.y) * t
                        gridCenterToLocal(ix, iy)
                    } else {
                        gridCenterToLocal(cur.x.toFloat(), cur.y.toFloat())
                    }

                    if (i == 0) {
                        drawSerpentHead(
                            center = pos,
                            spriteSize = tileSize * 1.18f,
                            direction = engine.moveDirection,
                            isWindPhase = windActive,
                            isFireBoost = fireActive,
                            isWaterCalm = waterActive
                        )
                    } else {
                        val isTail = (i == segs.lastIndex)
                        val scaleFactor = if (isTail) 0.78f else 0.96f
                        drawSerpentBody(
                            center = pos,
                            spriteSize = tileSize * scaleFactor,
                            isEven = (i % 2 == 0),
                            isWindPhase = windActive,
                            isFireBoost = fireActive,
                            isWaterCalm = waterActive
                        )
                    }
                }

                // Elemental Burst Particles
                for (p in particles) {
                    val alpha = (1f - p.age / p.duration).coerceIn(0f, 1f)
                    val center = gridCenterToLocal(p.x - 0.5f, p.y - 0.5f)
                    val s = p.size * tileSize * alpha
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(center.x - s * 0.5f, center.y - s * 0.5f),
                        size = Size(s, s)
                    )
                }
            }
        }

        // HUD Overlay (Top Bar, MiniCheerUp! Stage, Quick Orb Instructions, and Bottom D-Pad Buttons)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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

                // Live Gravity Compass Pill
                val compassDeg by animateFloatAsState(
                    targetValue = orientation.compassDegrees,
                    animationSpec = spring(dampingRatio = 0.75f),
                    label = "compassDeg"
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0B1E15).copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "safari.fill",
                        tint = Color(0xFF22D3EE),
                        size = 13.dp,
                        modifier = Modifier.graphicsLayer { rotationZ = compassDeg }
                    )
                    Text(
                        text = orientation.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CheerUpColors.lightGrey,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheerUpCircleIconButton(
                        symbol = "questionmark",
                        size = 36.dp,
                        iconSize = 13.dp,
                        onClick = {
                            HapticsManager.shared.selection()
                            isPaused = true
                            GravityAudioController.shared.pauseBackgroundMusic()
                            showTutorial = true
                        }
                    )
                    CheerUpCircleIconButton(
                        symbol = "pause.fill",
                        size = 36.dp,
                        iconSize = 14.dp,
                        onClick = {
                            HapticsManager.shared.selection()
                            if (!isGameOver) {
                                isPaused = true
                                GravityAudioController.shared.pauseBackgroundMusic()
                            }
                        }
                    )
                }
            }

            // MiniCheerUp! Mascot Stage + Score Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpMascotView(mood = mascotMood, size = 50.dp)

                val bubbleShape = remember { SpeechBubbleShape() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .shadow(5.dp, bubbleShape)
                        .clip(bubbleShape)
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
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
                        .background(Color(0xFF0B1E15).copy(alpha = 0.94f))
                        .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f), scoreShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LanguageManager.shared.tr("SCORE", "SCORE", "PUNTOS"),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "$score",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Quick Small Instructions of Orb Powers & Gravity Rule
            val stripShape = RoundedCornerShape(12.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding)
                    .clip(stripShape)
                    .background(Color(0xFF0B1E15).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF3E2C1C).copy(alpha = 0.65f), stripShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ElementalMoteType.entries.forEach { mote ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(mote.color)
                            )
                            Text(
                                text = when (mote) {
                                    ElementalMoteType.FIRE -> LanguageManager.shared.tr("Fire: Fast/2x", "Feu: Vite/2x", "Fuego: 2x")
                                    ElementalMoteType.WATER -> LanguageManager.shared.tr("Water: Slow", "Eau: Lent", "Agua: Lento")
                                    ElementalMoteType.EARTH -> LanguageManager.shared.tr("Earth: +1 Size", "Terre: +1", "Tierra: +1")
                                    ElementalMoteType.WIND -> LanguageManager.shared.tr("Wind: Ghost", "Vent: Fantôme", "Viento: Fase")
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CheerUpColors.lightGrey.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

                Text(
                    text = LanguageManager.shared.tr(
                        "Eat orbs to grow • Crossing borders rotates gravity • Avoid your tail",
                        "Mangez les orbes • Les bords tournent la gravité • Évitez votre queue",
                        "Come orbes • Cruzar bordes gira la gravedad • Evita tu cola"
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = CheerUpColors.mutedSilver.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Active Elemental Timers Row
            if (activeEffectsUI.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeEffectsUI.forEach { effect ->
                        Row(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B1E15).copy(alpha = 0.95f))
                                .border(1.5.dp, effect.type.color.copy(alpha = 0.65f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(effect.type.color)
                            )
                            Text(
                                text = effect.type.shortBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width((18f * effect.progress.toFloat()).coerceAtLeast(2f).dp)
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(effect.type.color)
                                )
                            }
                            Text(
                                text = "${ceil(effect.remaining).toInt()}s",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CheerUpColors.lightGrey,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Tactile Cross (+) Directional Pad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GravityDPadButton(
                    symbol = "chevron.up",
                    onClick = {
                        if (!isPaused && !isGameOver && !showTutorial) {
                            AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                            HapticsManager.shared.selection()
                            engine.queueScreenDirection(GravityDirection.UP)
                        }
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GravityDPadButton(
                        symbol = "chevron.left",
                        onClick = {
                            if (!isPaused && !isGameOver && !showTutorial) {
                                AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                                HapticsManager.shared.selection()
                                engine.queueScreenDirection(GravityDirection.LEFT)
                            }
                        }
                    )

                    val hubShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(hubShape)
                            .background(Color(0xFF0A1B13))
                            .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.3f), hubShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.45f))
                        )
                    }

                    GravityDPadButton(
                        symbol = "chevron.right",
                        onClick = {
                            if (!isPaused && !isGameOver && !showTutorial) {
                                AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                                HapticsManager.shared.selection()
                                engine.queueScreenDirection(GravityDirection.RIGHT)
                            }
                        }
                    )
                }

                GravityDPadButton(
                    symbol = "chevron.down",
                    onClick = {
                        if (!isPaused && !isGameOver && !showTutorial) {
                            AudioManager.shared.play(AudioManager.SoundEffect.TAP)
                            HapticsManager.shared.selection()
                            engine.queueScreenDirection(GravityDirection.DOWN)
                        }
                    }
                )
            }
        }

        // Interactive How-To-Play Tutorial Modal
        if (showTutorial) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenPadding)
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(Color(0xFF0B1E15))
                        .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.45f), cardShape)
                        .padding(Spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheerUpMascotView(mood = MascotMood.WOW, size = 48.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "GRAVITY",
                                style = CheerUpFont.title2,
                                color = CheerUpColors.lightGrey
                            )
                            Text(
                                text = LanguageManager.shared.tr("Enchanted Torus Serpent", "Le Serpent du Tore Enchanté", "Serpiente del Toro Encantado"),
                                style = CheerUpFont.caption,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF07140E).copy(alpha = 0.75f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GravityTutorialRow(
                            symbol = "square.grid.2x2.fill",
                            color = Color(0xFF10B981),
                            title = LanguageManager.shared.tr("Steer with Buttons Below", "Dirigez avec les Boutons", "Controla con los Botones"),
                            detail = LanguageManager.shared.tr(
                                "Use the 4 arrow buttons below the board to steer the serpent toward glowing orbs.",
                                "Utilisez les 4 flèches sous le plateau pour guider le serpent vers les orbes.",
                                "Usa los 4 botones de flecha debajo del tablero para guiar a la serpiente."
                            )
                        )
                        GravityTutorialRow(
                            symbol = "arrow.triangle.2.circlepath",
                            color = Color(0xFF22D3EE),
                            title = LanguageManager.shared.tr("Crossing Edges Shifts Gravity", "Les Bords Changent la Gravité", "Cruzar Bordes Cambia la Gravedad"),
                            detail = LanguageManager.shared.tr(
                                "Wrapping across a glowing rune border rotates the world's perspective!",
                                "Franchir une bordure runique fait pivoter la perspective du monde !",
                                "¡Cruzar un borde rúnico rota la perspectiva del mundo!"
                            )
                        )
                        GravityTutorialRow(
                            symbol = "star.fill",
                            color = Color(0xFFA855F7),
                            title = LanguageManager.shared.tr("4 Elemental Orbs", "4 Orbes Élémentaires", "4 Orbes Elementales"),
                            detail = LanguageManager.shared.tr(
                                "Fire boosts speed & 2x points (+2), Water slows time (+1), Earth is the common +1 length orb, Wind (+2) phases through your tail.",
                                "Feu accélère (2x, +2), Eau ralentit (+1), Terre est l'orbe courant (+1), Vent (+2) traverse votre queue.",
                                "Fuego acelera (2x, +2), Agua calma (+1), Tierra es el orbe común (+1), Viento (+2) atraviesa tu cola."
                            )
                        )
                    }

                    CheerUpPrimaryButton(
                        title = LanguageManager.shared.tr("Got It, Let's Play!", "Compris, Jouer !", "¡Entendido, A Jugar!"),
                        icon = "play.fill",
                        action = {
                            HapticsManager.shared.selection()
                            context.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(GRAVITY_TUTORIAL_SEEN_KEY, true)
                                .apply()
                            showTutorial = false
                            if (score == 0 && engine.motesCollectedCount == 0) {
                                startNewGame()
                            } else {
                                isPaused = false
                                GravityAudioController.shared.startBackgroundMusic(context)
                            }
                        }
                    )
                }
            }
        }

        // Pause Modal Overlay
        if (isPaused && !showTutorial && !isGameOver) {
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
                                GravityAudioController.shared.startBackgroundMusic(context)
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
                gameTitle = "GRAVITY",
                score = score,
                highScore = highScore,
                accentColor = Color(0xFF10B981),
                onReplay = { startNewGame() },
                onExit = ::handleExit,
                onWatchAdContinue = if (hasUsedRewardedContinue) null else {
                    { continueAfterReward() }
                }
            )
        }
    }
}

@Composable
private fun GravityDPadButton(
    symbol: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffsetY by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessHigh),
        label = "dpadPressY"
    )
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 56.dp)
            .graphicsLayer {
                translationY = pressOffsetY * density
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 4.dp)
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A4230), Color(0xFF0E261C))
                    )
                )
                .border(1.8.dp, Color(0xFF10B981).copy(alpha = 0.65f), shape),
            contentAlignment = Alignment.Center
        ) {
            CheerUpSymbolIcon(
                symbol = symbol,
                tint = CheerUpColors.lightGrey,
                size = 24.dp
            )
        }
    }
}

@Composable
private fun GravityTutorialRow(
    symbol: String,
    color: Color,
    title: String,
    detail: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        CheerUpSymbolIcon(
            symbol = symbol,
            tint = color,
            size = 18.dp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CheerUpColors.lightGrey
            )
            Text(
                text = detail,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CheerUpColors.mutedSilver
            )
        }
    }
}

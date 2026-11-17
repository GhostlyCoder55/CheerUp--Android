package com.example.cheerup.games.tap

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.presentation.components.CheerUpCircleIconButton
import com.example.cheerup.presentation.components.CheerUpMascotView
import com.example.cheerup.presentation.components.GameResultView
import com.example.cheerup.presentation.components.PixelHeartView
import com.example.cheerup.presentation.components.SpeechBubbleShape
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.Spacing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

@Composable
fun TapScreen(
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val engine = remember { TapEngine() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    var pausedByLifecycle by remember { mutableStateOf(false) }
    var hasStartedSession by remember { mutableStateOf(false) }
    val cubeBoundsInRoot = remember { mutableStateMapOf<Int, Rect>() }
    var confettiOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isForeground = false
                    engine.resetFrameTiming()
                    if (engine.isLive) {
                        pausedByLifecycle = true
                        engine.isLive = false
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    isForeground = true
                    if (pausedByLifecycle && !engine.isOver) {
                        pausedByLifecycle = false
                        engine.resetFrameTiming()
                        engine.isLive = true
                    } else {
                        pausedByLifecycle = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStartedSession) {
            hasStartedSession = true
            engine.startGame()
        }
    }

    val shouldRunLoop = isForeground &&
        ((engine.isLive && !engine.isOver) || engine.particles.isNotEmpty() || engine.burstRings.isNotEmpty())

    LaunchedEffect(shouldRunLoop) {
        if (shouldRunLoop) {
            engine.resetFrameTiming()
            while (true) {
                withFrameNanos { nanos ->
                    engine.updateFrameNanos(nanos)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.Tap.bgGradient))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header: Exit Button on Left, Round Pill in Center, Pixel Hearts on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.widthIn(min = 90.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CheerUpCircleIconButton(
                        symbol = "xmark",
                        size = 36.dp,
                        iconSize = 14.dp,
                        backgroundColor = CheerUpColors.Tap.pill,
                        tint = CheerUpColors.Tap.ink.copy(alpha = 0.9f),
                        contentDesc = "Exit",
                        onClick = {
                            HapticsManager.shared.selection()
                            onExit()
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CheerUpColors.Tap.pill)
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${LanguageManager.shared.tr("Round", "Tour", "Ronda")} ${max(1, engine.round)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CheerUpColors.Tap.ink
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.widthIn(min = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 3) {
                        PixelHeartView(
                            isFilled = i < engine.lives,
                            size = 22.dp
                        )
                    }
                }
            }

            // Mascot Stage & Speech Bubble
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpMascotView(
                    mood = engine.mascotMood,
                    size = 68.dp
                )

                val bubbleShape = remember { SpeechBubbleShape() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .shadow(6.dp, bubbleShape)
                        .clip(bubbleShape)
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = engine.speechText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.deepNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Instruction Card with Live Countdown Bar
            val psychScale by animateFloatAsState(
                targetValue = if (engine.isPsychAnimation) 1.08f else 1.0f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "psychScale"
            )
            val cardShape = RoundedCornerShape(24.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding)
                    .heightIn(min = 110.dp)
                    .shadow(8.dp, cardShape)
                    .clip(cardShape)
                    .background(CheerUpColors.Tap.panel)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        scaleX = psychScale
                        scaleY = psychScale
                    },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = engine.instructionPrefix,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = CheerUpColors.Tap.ink,
                        maxLines = 1
                    )
                    if (engine.instructionWord.isNotEmpty()) {
                        Text(
                            text = engine.instructionWord,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = engine.instructionColor,
                            maxLines = 1
                        )
                    }
                }

                if (engine.instructionTag.isNotEmpty()) {
                    Text(
                        text = engine.instructionTag,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.Tap.yellow,
                        modifier = Modifier.height(16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Countdown Progress Bar
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    val barW = maxWidth * engine.progress.toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(width = barW, height = 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (engine.progress < 0.3) CheerUpColors.Tap.red else CheerUpColors.Tap.yellow
                            )
                    )
                }
            }

            // 2x2 Tactile 3D Cube Grid
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = Spacing.screenPadding),
                contentAlignment = Alignment.Center
            ) {
                val side = min((maxWidth - 28.dp) / 2, (maxHeight - 28.dp) / 2)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (engine.cubeOrder.size > 0) {
                            TapCubeTile(
                                cube = engine.cubeOrder[0],
                                size = side,
                                onBoundsMeasured = { cubeBoundsInRoot[engine.cubeOrder[0].id] = it },
                                onTap = {
                                    val c = engine.cubeOrder[0]
                                    val localBounds = cubeBoundsInRoot[c.id]?.translate(-confettiOriginInRoot.x, -confettiOriginInRoot.y)
                                    engine.selectCube(c, localBounds?.center ?: Offset.Zero, localBounds)
                                }
                            )
                        }
                        if (engine.cubeOrder.size > 1) {
                            TapCubeTile(
                                cube = engine.cubeOrder[1],
                                size = side,
                                onBoundsMeasured = { cubeBoundsInRoot[engine.cubeOrder[1].id] = it },
                                onTap = {
                                    val c = engine.cubeOrder[1]
                                    val localBounds = cubeBoundsInRoot[c.id]?.translate(-confettiOriginInRoot.x, -confettiOriginInRoot.y)
                                    engine.selectCube(c, localBounds?.center ?: Offset.Zero, localBounds)
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (engine.cubeOrder.size > 2) {
                            TapCubeTile(
                                cube = engine.cubeOrder[2],
                                size = side,
                                onBoundsMeasured = { cubeBoundsInRoot[engine.cubeOrder[2].id] = it },
                                onTap = {
                                    val c = engine.cubeOrder[2]
                                    val localBounds = cubeBoundsInRoot[c.id]?.translate(-confettiOriginInRoot.x, -confettiOriginInRoot.y)
                                    engine.selectCube(c, localBounds?.center ?: Offset.Zero, localBounds)
                                }
                            )
                        }
                        if (engine.cubeOrder.size > 3) {
                            TapCubeTile(
                                cube = engine.cubeOrder[3],
                                size = side,
                                onBoundsMeasured = { cubeBoundsInRoot[engine.cubeOrder[3].id] = it },
                                onTap = {
                                    val c = engine.cubeOrder[3]
                                    val localBounds = cubeBoundsInRoot[c.id]?.translate(-confettiOriginInRoot.x, -confettiOriginInRoot.y)
                                    engine.selectCube(c, localBounds?.center ?: Offset.Zero, localBounds)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Milestone "NEW RULE!" Banner
        val bannerText = engine.bannerText
        if (engine.showBanner && bannerText != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .graphicsLayer { rotationZ = -2f }
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = LanguageManager.shared.tr("NEW RULE!", "NOUVELLE RÈGLE !", "¡NUEVA REGLA!"),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = CheerUpColors.Tap.red
                    )
                    Text(
                        text = bannerText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CheerUpColors.deepNavy,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Confetti & Shockwave Ring Canvas Layer (aligned cleanly via confettiOriginInRoot)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    confettiOriginInRoot = coords.positionInRoot()
                }
        ) {
            val currentTick = engine.frameTick
            if (engine.particles.isNotEmpty() || engine.burstRings.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val _tick = currentTick
                    for (ring in engine.burstRings) {
                        drawCircle(
                            color = ring.color.copy(alpha = ring.opacity.coerceIn(0f, 1f)),
                            radius = ring.radius,
                            center = Offset(ring.x, ring.y),
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }

                    for (p in engine.particles) {
                        val alpha = p.opacity.coerceIn(0f, 1f)
                        val tiltScale = max(0.18f, abs(cos(p.tilt)).toFloat())
                        val pxSize = p.size

                        withTransform({
                            translate(left = p.x, top = p.y)
                            rotate(degrees = p.rotation.toFloat(), pivot = Offset.Zero)
                            scale(scaleX = 1f, scaleY = tiltScale, pivot = Offset.Zero)
                        }) {
                            when (p.shape) {
                                TapConfettiShape.RIBBON -> {
                                    val h = pxSize * p.aspect
                                    drawRoundRect(
                                        color = p.color.copy(alpha = alpha),
                                        topLeft = Offset(-pxSize * 0.5f, -h * 0.5f),
                                        size = Size(pxSize, h),
                                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                                    )
                                }
                                TapConfettiShape.SQUARE -> {
                                    val s = pxSize * 0.78f
                                    drawRoundRect(
                                        color = p.color.copy(alpha = alpha),
                                        topLeft = Offset(-s * 0.5f, -s * 0.5f),
                                        size = Size(s, s),
                                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                                    )
                                }
                                TapConfettiShape.DIAMOND -> {
                                    val s = pxSize * 0.55f
                                    val diamond = Path().apply {
                                        moveTo(0f, -s)
                                        lineTo(s, 0f)
                                        lineTo(0f, s)
                                        lineTo(-s, 0f)
                                        close()
                                    }
                                    drawPath(diamond, color = p.color.copy(alpha = alpha))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Game Over Result Overlay
        if (engine.isOver) {
            GameResultView(
                gameTitle = "TAP",
                score = max(0, engine.round - 1),
                highScore = engine.highScore,
                accentColor = Color(0xFF00A896),
                onReplay = { engine.startGame() },
                onExit = onExit,
                onWatchAdContinue = if (engine.hasUsedRewardedContinue) null else {
                    { engine.continueAfterReward() }
                }
            )
        }
    }
}

@Composable
private fun TapCubeTile(
    cube: TapCube,
    size: Dp,
    onBoundsMeasured: (Rect) -> Unit,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffsetY by animateFloatAsState(
        targetValue = if (isPressed) 6f else 0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cubePressOffset"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "cubePressScale"
    )

    val cubeShape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .size(size)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                onBoundsMeasured(
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + coords.size.width.toFloat(),
                        bottom = pos.y + coords.size.height.toFloat()
                    )
                )
            }
            .graphicsLayer {
                translationY = pressOffsetY * density
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            )
            .semantics { contentDescription = "${cube.name} cube" }
    ) {
        // Bottom 3D shadow block
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 7.dp)
                .clip(cubeShape)
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // Main vibrant cube face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cubeShape)
                .background(cube.color)
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), cubeShape)
        )
    }
}

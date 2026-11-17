package com.example.cheerup.games.wheels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.presentation.components.CheerUpCircleIconButton
import com.example.cheerup.presentation.components.CheerUpMascotView
import com.example.cheerup.presentation.components.CheerUpSymbolIcon
import com.example.cheerup.presentation.components.SpeechBubbleShape
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.Spacing

@Composable
fun WheelsScreen(
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val engine = remember { WheelsEngine(context) }

    LaunchedEffect(Unit) {
        RewardedAdManager.shared.preloadWheelsAdIfNeeded(context)
    }

    val shouldAnimate = engine.isSpinning || engine.particles.isNotEmpty() || engine.isTimerActive
    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) {
            while (true) {
                withFrameNanos { nanos ->
                    engine.updateFrameNanos(nanos)
                }
            }
        }
    }

    val flipRotation by animateFloatAsState(
        targetValue = if (engine.isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "wheelFlip"
    )
    val pointerRot by animateFloatAsState(
        targetValue = engine.pointerDeflection.toFloat(),
        animationSpec = spring(dampingRatio = 0.55f),
        label = "pointerDeflect"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header (Exit Button on Left, Title in Center)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpCircleIconButton(
                    symbol = "xmark",
                    size = 36.dp,
                    iconSize = 14.dp,
                    backgroundColor = CheerUpColors.blueGrey.copy(alpha = 0.6f),
                    onClick = {
                        HapticsManager.shared.selection()
                        onExit()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "WHEELS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.size(36.dp))
            }

            // Mascot Stage & Speech Bubble
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpMascotView(mood = engine.mascotMood, size = 64.dp)

                val bubbleShape = remember { SpeechBubbleShape() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
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

            // Spinning Wheel Stage
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { rotationZ = flipRotation },
                contentAlignment = Alignment.Center
            ) {
                val wheelSize = min(min(maxWidth - 24.dp, maxHeight - 24.dp), 340.dp)

                Box(
                    modifier = Modifier.size(wheelSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Drop Shadow
                    Box(
                        modifier = Modifier
                            .size(wheelSize)
                            .offset(y = 8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    )

                    // 12-Segment Clean Wheel
                    Canvas(
                        modifier = Modifier
                            .size(wheelSize)
                            .graphicsLayer { rotationZ = engine.rotation.toFloat() }
                    ) {
                        val segAngle = 360f / 12f
                        for (i in 0 until 12) {
                            val startDeg = i * segAngle
                            val segColor = WheelsData.segments[i].color
                            drawArc(
                                color = segColor,
                                startAngle = startDeg,
                                sweepAngle = segAngle,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = size
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.35f),
                                startAngle = startDeg,
                                sweepAngle = segAngle,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = size,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        drawCircle(
                            color = CheerUpColors.deepNavy,
                            radius = size.width * 0.5f - 3.dp.toPx(),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }

                    // Center Metallic / Dark Hub
                    val hubSize = wheelSize * 0.16f
                    Box(
                        modifier = Modifier
                            .size(hubSize)
                            .shadow(5.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CheerUpColors.blueGrey, CheerUpColors.deepNavy)
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(wheelSize * 0.07f)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2D55))
                        )
                    }

                    // Top Pointer Needle (Fixed at 12 o'clock with tick oscillation)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-8).dp)
                            .size(width = 36.dp, height = 42.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                rotationZ = pointerRot
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sx = size.width / 36f
                            val sy = size.height / 42f
                            val needlePath = Path().apply {
                                moveTo(18f * sx, 40f * sy)
                                lineTo(3f * sx, 8f * sy)
                                quadraticTo(18f * sx, -4f * sy, 33f * sx, 8f * sy)
                                close()
                            }
                            drawPath(needlePath, color = CheerUpColors.deepNavy)
                            drawPath(needlePath, color = Color.White, style = Stroke(width = 2.dp.toPx()))
                            drawCircle(
                                color = Color(0xFF00A896),
                                radius = 4.5f * sx,
                                center = Offset(18f * sx, 9f * sy)
                            )
                        }
                    }
                }
            }

            // Bottom Controls: Prominent SPIN Button
            val spinBtnScale by animateFloatAsState(
                targetValue = if (engine.isSpinning) 0.98f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f),
                label = "spinBtnScale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding)
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = spinBtnScale
                        scaleY = spinBtnScale
                        alpha = if (engine.isSpinning) 0.6f else 1.0f
                    }
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(CheerUpColors.lightGrey)
                    .clickable(enabled = !engine.isSpinning) {
                        engine.spin()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "arrow.triangle.2.circlepath",
                        tint = CheerUpColors.deepNavy,
                        size = 16.dp
                    )
                    Text(
                        text = if (engine.isSpinning) {
                            LanguageManager.shared.tr("SPINNING...", "EN COURS...", "GIRANDO...")
                        } else {
                            LanguageManager.shared.tr("SPIN", "TOURNER", "GIRAR")
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CheerUpColors.deepNavy,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Confetti Particle Explosion Layer
        val currentTick = engine.frameTick
        if (engine.particles.isNotEmpty()) {
            val density = LocalDensity.current.density
            Canvas(modifier = Modifier.fillMaxSize()) {
                val _t = currentTick
                for (p in engine.particles) {
                    val px = p.x * density
                    val py = p.y * density
                    val pw = p.size * density
                    val ph = pw * 0.65f
                    withTransform({
                        translate(left = px, top = py)
                        rotate(degrees = p.rotation.toFloat(), pivot = Offset.Zero)
                    }) {
                        drawRect(
                            color = p.color.copy(alpha = p.opacity.toFloat().coerceIn(0f, 1f)),
                            topLeft = Offset(-pw * 0.5f, -ph * 0.5f),
                            size = Size(pw, ph)
                        )
                    }
                }
            }
        }

        // Result Reveal Overlay Card
        val outcome = engine.selectedOutcome
        if (engine.showResultSheet && outcome != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        engine.showResultSheet = false
                    },
                contentAlignment = Alignment.Center
            ) {
                val sheetShape = RoundedCornerShape(32.dp)
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.screenPadding)
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .shadow(24.dp, sheetShape)
                        .clip(sheetShape)
                        .background(CheerUpColors.darkBlue)
                        .border(1.5.dp, CheerUpColors.blueGrey.copy(alpha = 0.35f), sheetShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = outcome.emoji,
                        fontSize = 64.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = outcome.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = outcome.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    if (outcome.timerSeconds != null) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheerUpSymbolIcon(
                                symbol = "timer",
                                tint = CheerUpColors.lightGrey,
                                size = 18.dp
                            )
                            Text(
                                text = if (engine.countdownRemaining > 0) {
                                    "⏱ ${engine.countdownRemaining}s"
                                } else {
                                    LanguageManager.shared.tr("Done! 🎉", "Fini ! 🎉", "¡Listo! 🎉")
                                },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = CheerUpColors.lightGrey
                            )
                        }
                    }

                    CheerUpMascotView(
                        mood = outcome.mood,
                        size = 54.dp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(CheerUpColors.blueGrey)
                            .clickable { engine.spinAgain() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheerUpSymbolIcon(
                                symbol = "arrow.triangle.2.circlepath",
                                tint = Color.White,
                                size = 16.dp
                            )
                            Text(
                                text = LanguageManager.shared.tr("SPIN AGAIN", "RETOURNER", "OTRA VEZ"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

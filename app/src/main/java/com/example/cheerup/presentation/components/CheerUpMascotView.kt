package com.example.cheerup.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cheerup.domain.model.MascotMood
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CheerUpMascotView(
    mood: MascotMood = MascotMood.HAPPY,
    size: Dp = 68.dp,
    modifier: Modifier = Modifier
) {
    val hopOffset = remember { Animatable(0f) }
    val wobbleRotation = remember { Animatable(0f) }

    LaunchedEffect(mood) {
        when (mood) {
            MascotMood.HAPPY, MascotMood.WOW, MascotMood.PARTY -> {
                launch {
                    wobbleRotation.snapTo(0f)
                    hopOffset.animateTo(
                        targetValue = -12f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                    )
                    hopOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
                    )
                }
            }
            MascotMood.SAD, MascotMood.SUS, MascotMood.DIZZY -> {
                launch {
                    hopOffset.animateTo(4f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh))
                    delay(180)
                    hopOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                }
                launch {
                    wobbleRotation.animateTo(-8f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh))
                    wobbleRotation.animateTo(8f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh))
                    wobbleRotation.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = hopOffset.value * density
                rotationZ = wobbleRotation.value
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val s = this.size.width
            val corner = s * 0.24f

            // Drop shadow
            drawRoundRect(
                color = Color(0xFF00A896).copy(alpha = 0.36f),
                topLeft = Offset(0f, s * 0.06f),
                size = Size(s, s),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Main Cube Body (#00A896 -> #028090)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00A896), Color(0xFF028090)),
                    start = Offset.Zero,
                    end = Offset(s, s)
                ),
                topLeft = Offset.Zero,
                size = Size(s, s),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Subtle white rim stroke
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset.Zero,
                size = Size(s, s),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Top-right glossy highlight (MascotGlossCorner)
            val glossW = s * 0.32f
            val glossH = s * 0.32f
            val gx = s * 0.5f + s * 0.24f - glossW * 0.5f
            val gy = s * 0.5f - s * 0.24f - glossH * 0.5f
            val glossPath = Path().apply {
                moveTo(gx, gy)
                lineTo(gx + glossW, gy)
                lineTo(gx + glossW, gy + glossH)
                lineTo(gx + glossW * 0.55f, gy + glossH)
                lineTo(gx + glossW * 0.55f, gy + glossH * 0.45f)
                lineTo(gx, gy + glossH * 0.45f)
                close()
            }
            drawPath(glossPath, color = Color.White.copy(alpha = 0.22f))

            // Eyes (centered above middle)
            val eyeCenterY = s * 0.40f
            when (mood) {
                MascotMood.HAPPY, MascotMood.PARTY, MascotMood.SAD -> {
                    val eyeW = s * 0.10f
                    val eyeH = s * 0.22f
                    val gap = s * 0.14f
                    val leftX = s * 0.5f - gap * 0.5f - eyeW
                    val rightX = s * 0.5f + gap * 0.5f
                    val eyeTop = eyeCenterY - eyeH * 0.5f
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(leftX, eyeTop),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(rightX, eyeTop),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
                MascotMood.WOW -> {
                    val r = s * 0.09f
                    val gap = s * 0.16f
                    val leftCenter = Offset(s * 0.5f - gap * 0.5f - r, eyeCenterY)
                    val rightCenter = Offset(s * 0.5f + gap * 0.5f + r, eyeCenterY)
                    drawCircle(color = Color.Black, radius = r, center = leftCenter)
                    drawCircle(color = Color.White, radius = s * 0.03f, center = leftCenter + Offset(-r * 0.25f, -r * 0.25f))
                    drawCircle(color = Color.Black, radius = r, center = rightCenter)
                    drawCircle(color = Color.White, radius = s * 0.03f, center = rightCenter + Offset(-r * 0.25f, -r * 0.25f))
                }
                MascotMood.SUS -> {
                    val eyeW = s * 0.15f
                    val eyeH = s * 0.08f
                    val gap = s * 0.16f
                    val leftX = s * 0.5f - gap * 0.5f - eyeW
                    val rightX = s * 0.5f + gap * 0.5f
                    val eyeTop = eyeCenterY - eyeH * 0.5f
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(leftX, eyeTop),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(rightX, eyeTop),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
                MascotMood.DIZZY -> {
                    val arm = s * 0.07f
                    val strokeW = s * 0.045f
                    val leftCenter = Offset(s * 0.36f, eyeCenterY)
                    val rightCenter = Offset(s * 0.64f, eyeCenterY)
                    for (c in listOf(leftCenter, rightCenter)) {
                        drawLine(
                            color = Color.Black,
                            start = Offset(c.x - arm, c.y - arm),
                            end = Offset(c.x + arm, c.y + arm),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.Black,
                            start = Offset(c.x + arm, c.y - arm),
                            end = Offset(c.x - arm, c.y + arm),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Mouth / Expression
            val mouthCenterY = s * 0.67f
            when (mood) {
                MascotMood.HAPPY, MascotMood.PARTY -> {
                    val mw = s * 0.34f
                    val mh = s * 0.12f
                    val mx = (s - mw) * 0.5f
                    val my = mouthCenterY - mh * 0.5f
                    val smilePath = Path().apply {
                        moveTo(mx, my)
                        lineTo(mx, my + mh)
                        lineTo(mx + mw, my + mh)
                        lineTo(mx + mw, my)
                    }
                    drawPath(
                        smilePath,
                        color = Color.Black,
                        style = Stroke(width = s * 0.09f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                    )
                }
                MascotMood.WOW -> {
                    val mw = s * 0.18f
                    val mh = s * 0.16f
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset((s - mw) * 0.5f, mouthCenterY - mh * 0.5f),
                        size = Size(mw, mh),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = s * 0.08f)
                    )
                }
                MascotMood.SAD -> {
                    val mw = s * 0.34f
                    val mh = s * 0.12f
                    val mx = (s - mw) * 0.5f
                    val my = mouthCenterY - mh * 0.5f
                    val frownPath = Path().apply {
                        moveTo(mx, my + mh)
                        lineTo(mx, my)
                        lineTo(mx + mw, my)
                        lineTo(mx + mw, my + mh)
                    }
                    drawPath(
                        frownPath,
                        color = Color.Black,
                        style = Stroke(width = s * 0.09f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
                    )
                }
                MascotMood.SUS -> {
                    val mw = s * 0.28f
                    val mh = s * 0.07f
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((s - mw) * 0.5f, mouthCenterY - mh * 0.5f),
                        size = Size(mw, mh)
                    )
                }
                MascotMood.DIZZY -> {
                    val mw = s * 0.32f
                    val mh = s * 0.08f
                    val mx = (s - mw) * 0.5f
                    val wavyPath = Path().apply {
                        moveTo(mx, mouthCenterY)
                        quadraticTo(mx + mw * 0.25f, mouthCenterY - mh, mx + mw * 0.5f, mouthCenterY)
                        quadraticTo(mx + mw * 0.75f, mouthCenterY + mh, mx + mw, mouthCenterY)
                    }
                    drawPath(
                        wavyPath,
                        color = Color.Black,
                        style = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
                    )
                }
            }

            // Party Hat Accessory
            if (mood == MascotMood.PARTY) {
                val hatPath = Path().apply {
                    moveTo(s * 0.5f, -s * 0.22f)
                    lineTo(s * 0.35f, 0f)
                    lineTo(s * 0.65f, 0f)
                    close()
                }
                drawPath(hatPath, color = Color(0xFF7500C8))
                drawCircle(
                    color = Color(0xFFFFC400),
                    radius = s * 0.06f,
                    center = Offset(s * 0.5f, -s * 0.22f)
                )
            }
        }
    }
}

package com.example.cheerup.presentation.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CheerUpPrimaryButton(
    title: String,
    icon: String? = null,
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "primaryBtnScale"
    )

    val shape = RoundedCornerShape(Spacing.buttonCornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isPressed) 0.88f else 1.0f
            }
            .clip(shape)
            .background(CheerUpColors.lightGrey)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action
            )
            .semantics { contentDescription = title },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                CheerUpSymbolIcon(
                    symbol = icon,
                    tint = Color.Black,
                    size = 16.dp
                )
            }
            Text(
                text = title,
                style = CheerUpFont.headline,
                color = Color.Black
            )
        }
    }
}

@Composable
fun CheerUpSecondaryButton(
    title: String,
    icon: String? = null,
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "secondaryBtnScale"
    )

    val shape = RoundedCornerShape(Spacing.buttonCornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isPressed) 0.88f else 1.0f
            }
            .clip(shape)
            .background(CheerUpColors.blueGrey.copy(alpha = 0.6f))
            .border(1.dp, CheerUpColors.slate.copy(alpha = 0.4f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action
            )
            .semantics { contentDescription = title },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                CheerUpSymbolIcon(
                    symbol = icon,
                    tint = CheerUpColors.lightGrey,
                    size = 16.dp
                )
            }
            Text(
                text = title,
                style = CheerUpFont.headline,
                color = CheerUpColors.lightGrey
            )
        }
    }
}

@Composable
fun CheerUpCircleIconButton(
    symbol: String,
    size: Dp = 36.dp,
    iconSize: Dp = 14.dp,
    backgroundColor: Color = CheerUpColors.blueGrey.copy(alpha = 0.75f),
    tint: Color = CheerUpColors.lightGrey.copy(alpha = 0.9f),
    contentDesc: String = symbol,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "circleBtnScale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        CheerUpSymbolIcon(symbol = symbol, tint = tint, size = iconSize)
    }
}

/**
 * Renders crisp vector equivalents of the SF Symbols used across CheerUp! iOS.
 */
@Composable
fun CheerUpSymbolIcon(
    symbol: String,
    tint: Color,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.13f

        when (symbol) {
            "play.fill" -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.50f)
                    lineTo(w * 0.22f, h * 0.86f)
                    close()
                }
                drawPath(p, color = tint)
            }
            "play.rectangle.fill" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.06f, h * 0.18f),
                    size = Size(w * 0.88f, h * 0.64f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = Stroke(width = stroke)
                )
                val p = Path().apply {
                    moveTo(w * 0.38f, h * 0.32f)
                    lineTo(w * 0.68f, h * 0.50f)
                    lineTo(w * 0.38f, h * 0.68f)
                    close()
                }
                drawPath(p, color = tint)
            }
            "pause.fill" -> {
                val barW = w * 0.24f
                val barH = h * 0.68f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, (h - barH) * 0.5f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.58f, (h - barH) * 0.5f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
            }
            "xmark" -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.22f, h * 0.22f),
                    end = Offset(w * 0.78f, h * 0.78f),
                    strokeWidth = stroke * 1.15f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.78f, h * 0.22f),
                    end = Offset(w * 0.22f, h * 0.78f),
                    strokeWidth = stroke * 1.15f,
                    cap = StrokeCap.Round
                )
            }
            "chevron.right" -> {
                val p = Path().apply {
                    moveTo(w * 0.34f, h * 0.18f)
                    lineTo(w * 0.68f, h * 0.50f)
                    lineTo(w * 0.34f, h * 0.82f)
                }
                drawPath(p, color = tint, style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "chevron.left" -> {
                val p = Path().apply {
                    moveTo(w * 0.66f, h * 0.18f)
                    lineTo(w * 0.32f, h * 0.50f)
                    lineTo(w * 0.66f, h * 0.82f)
                }
                drawPath(p, color = tint, style = Stroke(width = stroke * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "chevron.up" -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.66f)
                    lineTo(w * 0.50f, h * 0.32f)
                    lineTo(w * 0.82f, h * 0.66f)
                }
                drawPath(p, color = tint, style = Stroke(width = stroke * 1.25f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "chevron.down" -> {
                val p = Path().apply {
                    moveTo(w * 0.18f, h * 0.34f)
                    lineTo(w * 0.50f, h * 0.68f)
                    lineTo(w * 0.82f, h * 0.34f)
                }
                drawPath(p, color = tint, style = Stroke(width = stroke * 1.25f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "arrow.clockwise", "arrow.triangle.2.circlepath" -> {
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val arrow = Path().apply {
                    moveTo(w * 0.64f, h * 0.08f)
                    lineTo(w * 0.86f, h * 0.26f)
                    lineTo(w * 0.58f, h * 0.34f)
                    close()
                }
                drawPath(arrow, color = tint)
            }
            "square.grid.2x2.fill" -> {
                val cell = w * 0.36f
                val r = CornerRadius(w * 0.08f, w * 0.08f)
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.10f), Size(cell, cell), r)
                drawRoundRect(tint, Offset(w * 0.54f, h * 0.10f), Size(cell, cell), r)
                drawRoundRect(tint, Offset(w * 0.10f, h * 0.54f), Size(cell, cell), r)
                drawRoundRect(tint, Offset(w * 0.54f, h * 0.54f), Size(cell, cell), r)
            }
            "trophy.fill" -> {
                // Cup body
                val cup = Path().apply {
                    moveTo(w * 0.24f, h * 0.14f)
                    lineTo(w * 0.76f, h * 0.14f)
                    lineTo(w * 0.68f, h * 0.54f)
                    lineTo(w * 0.32f, h * 0.54f)
                    close()
                }
                drawPath(cup, color = tint)
                // Stem & base
                drawRect(tint, Offset(w * 0.44f, h * 0.54f), Size(w * 0.12f, h * 0.22f))
                drawRoundRect(tint, Offset(w * 0.26f, h * 0.76f), Size(w * 0.48f, h * 0.12f), CornerRadius(w * 0.04f, w * 0.04f))
            }
            "star.fill" -> {
                val p = Path()
                val cx = w * 0.5f
                val cy = h * 0.5f
                val outerR = w * 0.46f
                val innerR = w * 0.20f
                for (i in 0 until 10) {
                    val angle = (-PI / 2.0) + i * (PI / 5.0)
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = cx + (cos(angle) * r).toFloat()
                    val y = cy + (sin(angle) * r).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, color = tint)
            }
            "gearshape.fill" -> {
                val cx = w * 0.5f
                val cy = h * 0.5f
                for (i in 0 until 8) {
                    val angle = i * (PI / 4.0)
                    val x1 = cx + (cos(angle) * w * 0.22f).toFloat()
                    val y1 = cy + (sin(angle) * h * 0.22f).toFloat()
                    val x2 = cx + (cos(angle) * w * 0.43f).toFloat()
                    val y2 = cy + (sin(angle) * h * 0.43f).toFloat()
                    drawLine(tint, Offset(x1, y1), Offset(x2, y2), strokeWidth = w * 0.16f, cap = StrokeCap.Round)
                }
                drawCircle(tint, radius = w * 0.32f, center = Offset(cx, cy))
                drawCircle(CheerUpColors.deepNavy, radius = w * 0.13f, center = Offset(cx, cy))
            }
            "slider.horizontal.3" -> {
                for (i in 0..2) {
                    val y = h * (0.24f + i * 0.26f)
                    drawLine(tint, Offset(w * 0.14f, y), Offset(w * 0.86f, y), strokeWidth = stroke * 0.85f, cap = StrokeCap.Round)
                    val knobX = if (i == 1) w * 0.66f else w * 0.36f
                    drawCircle(tint, radius = w * 0.10f, center = Offset(knobX, y))
                }
            }
            "crown.fill" -> {
                val p = Path().apply {
                    moveTo(w * 0.12f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.28f)
                    lineTo(w * 0.38f, h * 0.52f)
                    lineTo(w * 0.50f, h * 0.20f)
                    lineTo(w * 0.62f, h * 0.52f)
                    lineTo(w * 0.84f, h * 0.28f)
                    lineTo(w * 0.88f, h * 0.78f)
                    close()
                }
                drawPath(p, color = tint)
            }
            "timer" -> {
                drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.54f), style = Stroke(width = stroke))
                drawLine(tint, Offset(w * 0.38f, h * 0.10f), Offset(w * 0.62f, h * 0.10f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.54f), Offset(w * 0.66f, h * 0.38f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            "infinity" -> {
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.32f, h * 0.5f), style = Stroke(width = stroke))
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.68f, h * 0.5f), style = Stroke(width = stroke))
            }
            "bolt.fill" -> {
                val p = Path().apply {
                    moveTo(w * 0.56f, h * 0.08f)
                    lineTo(w * 0.22f, h * 0.54f)
                    lineTo(w * 0.48f, h * 0.54f)
                    lineTo(w * 0.42f, h * 0.92f)
                    lineTo(w * 0.78f, h * 0.44f)
                    lineTo(w * 0.52f, h * 0.44f)
                    close()
                }
                drawPath(p, color = tint)
            }
            "questionmark" -> {
                drawArc(
                    color = tint,
                    startAngle = -180f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.16f),
                    size = Size(w * 0.44f, h * 0.34f),
                    style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round)
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.50f), Offset(w * 0.5f, h * 0.62f), strokeWidth = stroke * 1.1f, cap = StrokeCap.Round)
                drawCircle(tint, radius = stroke * 0.65f, center = Offset(w * 0.5f, h * 0.82f))
            }
            "safari.fill" -> {
                drawCircle(tint, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
                val needle = Path().apply {
                    moveTo(w * 0.5f, h * 0.18f)
                    lineTo(w * 0.62f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.82f)
                    lineTo(w * 0.38f, h * 0.5f)
                    close()
                }
                drawPath(needle, color = tint)
            }
            "checkmark.circle.fill" -> {
                drawCircle(tint, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
                val check = Path().apply {
                    moveTo(w * 0.28f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.68f)
                    lineTo(w * 0.72f, h * 0.34f)
                }
                drawPath(check, color = CheerUpColors.lightGrey, style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            "circle" -> {
                drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
            }
            "speaker.wave.2.fill" -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.38f)
                    lineTo(w * 0.32f, h * 0.38f)
                    lineTo(w * 0.54f, h * 0.18f)
                    lineTo(w * 0.54f, h * 0.82f)
                    lineTo(w * 0.32f, h * 0.62f)
                    lineTo(w * 0.14f, h * 0.62f)
                    close()
                }
                drawPath(p, color = tint)
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.42f, h * 0.26f),
                    size = Size(w * 0.44f, h * 0.48f),
                    style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round)
                )
            }
            "hand.tap.fill", "hand.point.up.left.fill" -> {
                drawCircle(tint.copy(alpha = 0.35f), radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.28f))
                drawRoundRect(tint, Offset(w * 0.40f, h * 0.24f), Size(w * 0.20f, h * 0.58f), CornerRadius(w * 0.10f, w * 0.10f))
                drawRoundRect(tint, Offset(w * 0.26f, h * 0.52f), Size(w * 0.48f, h * 0.34f), CornerRadius(w * 0.14f, w * 0.14f))
            }
            "bell.badge.fill" -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    quadraticTo(w * 0.80f, h * 0.18f, w * 0.82f, h * 0.68f)
                    lineTo(w * 0.18f, h * 0.68f)
                    quadraticTo(w * 0.20f, h * 0.18f, w * 0.5f, h * 0.14f)
                    close()
                }
                drawPath(p, color = tint)
                drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.80f))
            }
            "globe" -> {
                drawCircle(tint, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke * 0.85f))
                drawLine(tint, Offset(w * 0.10f, h * 0.5f), Offset(w * 0.90f, h * 0.5f), strokeWidth = stroke * 0.75f)
                drawLine(tint, Offset(w * 0.5f, h * 0.10f), Offset(w * 0.5f, h * 0.90f), strokeWidth = stroke * 0.75f)
            }
            else -> {
                drawCircle(tint, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}

package com.example.cheerup.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val HEART_GRID = listOf(
    "..BBB...BBB..",
    ".BSRRB.BRRRB.",
    "BSWWRRBRRRRRB",
    "BSWWRRRRRRRRB",
    "BSRRRRRRRRRSB",
    ".BSRRRRRRRSB.",
    "..BSRRRRRSB..",
    "...BSRRRSB...",
    "....BSRSB....",
    ".....BSB.....",
    "......B......"
)

@Composable
fun PixelHeartView(
    isFilled: Boolean,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "heartScale"
    )

    val borderColor = if (isFilled) Color(0xFF240711) else Color(0xFF340E1B)
    val shadowColor = if (isFilled) Color(0xFF99122C) else Color(0xFF240913).copy(alpha = 0.4f)
    val fillColor = if (isFilled) Color(0xFFFF2E55) else Color(0xFF1A060E).copy(alpha = 0.35f)
    val shineColor = if (isFilled) Color.White else Color.White.copy(alpha = 0.6f)

    Canvas(
        modifier = modifier
            .size(width = size, height = size * (11f / 13f))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = if (isFilled) "Full heart" else "Empty heart"
            }
    ) {
        val cols = 13
        val rows = 11
        val pixelW = this.size.width / cols
        val pixelH = this.size.height / rows
        val pixelSize = min(pixelW, pixelH)

        val offsetX = (this.size.width - cols * pixelSize) * 0.5f
        val offsetY = (this.size.height - rows * pixelSize) * 0.5f

        for ((rIndex, row) in HEART_GRID.withIndex()) {
            for ((cIndex, ch) in row.withIndex()) {
                val color = when (ch) {
                    'B' -> borderColor
                    'W' -> shineColor
                    'S' -> shadowColor
                    'R' -> fillColor
                    else -> continue
                }
                drawRect(
                    color = color,
                    topLeft = Offset(offsetX + cIndex * pixelSize, offsetY + rIndex * pixelSize),
                    size = Size(pixelSize + 0.5f, pixelSize + 0.5f)
                )
            }
        }
    }
}

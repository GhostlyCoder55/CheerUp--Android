package com.example.cheerup.presentation.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class SpeechBubbleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val tailW = with(density) { 4.dp.toPx() }
        val r = with(density) { 16.dp.toPx() }
        val tailHalfH = with(density) { 6.dp.toPx() }

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(tailW, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            )
            moveTo(tailW, size.height * 0.5f - tailHalfH)
            lineTo(0f, size.height * 0.5f)
            lineTo(tailW, size.height * 0.5f + tailHalfH)
            close()
        }
        return Outline.Generic(path)
    }
}

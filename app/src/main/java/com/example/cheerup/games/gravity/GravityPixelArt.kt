package com.example.cheerup.games.gravity

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

object GravityPixelArt {

    private val SERPENT_HEAD_GRID = listOf(
        "....44....",
        "....44....",
        "..111111..",
        ".11222211.",
        ".13322331.",
        ".13522531.",
        ".11222211.",
        ".11122111.",
        "..111111..",
        ".........."
    )

    private val SERPENT_BODY_GRID = listOf(
        "..........",
        "..111111..",
        ".11222211.",
        ".12233221.",
        ".12333321.",
        ".12333321.",
        ".12233221.",
        ".11222211.",
        "..111111..",
        ".........."
    )

    private val FIRE_MOTE_GRID = listOf(
        "....11....",
        "...1121...",
        "..112211..",
        ".11223211.",
        ".12233321.",
        ".12333321.",
        "..123321..",
        "...1111...",
        "..........",
        ".........."
    )

    private val WATER_MOTE_GRID = listOf(
        "....11....",
        "...1221...",
        "..122321..",
        ".12233221.",
        ".12332221.",
        ".12222221.",
        "..122221..",
        "...1111...",
        "..........",
        ".........."
    )

    private val EARTH_MOTE_GRID = listOf(
        "....33....",
        "..333333..",
        ".33444433.",
        "..112211..",
        ".11222211.",
        ".12222221.",
        "..122221..",
        "...1111...",
        "..........",
        ".........."
    )

    private val WIND_MOTE_GRID = listOf(
        "...1111...",
        "..122221..",
        ".1233311..",
        ".1231.....",
        "..123321..",
        "....1321..",
        "..112221..",
        "...1111...",
        "..........",
        ".........."
    )

    fun DrawScope.drawSerpentHead(
        center: Offset,
        spriteSize: Float,
        direction: GravityDirection,
        isWindPhase: Boolean,
        isFireBoost: Boolean,
        isWaterCalm: Boolean
    ) {
        val alpha = if (isWindPhase) 0.65f else 1.0f
        val (primary, highlight) = when {
            isWindPhase -> Color(0xFFBAE6FD) to Color.White
            isWaterCalm -> Color(0xFF0284C7) to Color(0xFF7DD3FC)
            isFireBoost -> Color(0xFFF59E0B) to Color(0xFFFDE68A)
            else -> Color(0xFF10B981) to Color(0xFF6EE7B7)
        }
        val palette = mapOf(
            '1' to primary.copy(alpha = alpha),
            '2' to highlight.copy(alpha = alpha),
            '3' to Color(0xFF141414).copy(alpha = alpha),
            '4' to Color(0xFFF43F5E).copy(alpha = alpha),
            '5' to Color.White.copy(alpha = alpha)
        )
        val rotDegrees = when (direction) {
            GravityDirection.UP -> 0f
            GravityDirection.RIGHT -> 90f
            GravityDirection.DOWN -> 180f
            GravityDirection.LEFT -> -90f
        }
        withTransform({
            rotate(degrees = rotDegrees, pivot = center)
        }) {
            drawPixelGrid(SERPENT_HEAD_GRID, palette, center, spriteSize)
        }
    }

    fun DrawScope.drawSerpentBody(
        center: Offset,
        spriteSize: Float,
        isEven: Boolean,
        isWindPhase: Boolean,
        isFireBoost: Boolean,
        isWaterCalm: Boolean
    ) {
        val alpha = if (isWindPhase) 0.65f else 1.0f
        val (primary, scalePattern, outline) = when {
            isWindPhase -> Triple(
                if (isEven) Color(0xFFBAE6FD) else Color(0xFF7DD3FC),
                Color.White,
                Color(0xFF062C1E)
            )
            isWaterCalm -> Triple(
                if (isEven) Color(0xFF0284C7) else Color(0xFF0369A1),
                Color(0xFF7DD3FC),
                Color(0xFF075985)
            )
            isFireBoost -> Triple(
                if (isEven) Color(0xFFF59E0B) else Color(0xFFD97706),
                Color(0xFFFDE68A),
                Color(0xFF062C1E)
            )
            else -> Triple(
                if (isEven) Color(0xFF10B981) else Color(0xFF059669),
                Color(0xFFA7F3D0),
                Color(0xFF062C1E)
            )
        }
        val palette = mapOf(
            '1' to outline.copy(alpha = alpha),
            '2' to primary.copy(alpha = alpha),
            '3' to scalePattern.copy(alpha = alpha)
        )
        drawPixelGrid(SERPENT_BODY_GRID, palette, center, spriteSize)
    }

    fun DrawScope.drawMoteSprite(
        type: ElementalMoteType,
        center: Offset,
        spriteSize: Float
    ) {
        val (grid, palette) = when (type) {
            ElementalMoteType.FIRE -> FIRE_MOTE_GRID to mapOf(
                '1' to Color(0xFFEA580C),
                '2' to Color(0xFFF59E0B),
                '3' to Color(0xFFFEF08A)
            )
            ElementalMoteType.WATER -> WATER_MOTE_GRID to mapOf(
                '1' to Color(0xFF075985),
                '2' to Color(0xFF0284C7),
                '3' to Color(0xFFBAE6FD)
            )
            ElementalMoteType.EARTH -> EARTH_MOTE_GRID to mapOf(
                '1' to Color(0xFF059669),
                '2' to Color(0xFF34D399),
                '3' to Color(0xFF78350F),
                '4' to Color(0xFFA16207)
            )
            ElementalMoteType.WIND -> WIND_MOTE_GRID to mapOf(
                '1' to Color(0xFF38BDF8),
                '2' to Color(0xFFBAE6FD),
                '3' to Color.White
            )
        }
        drawPixelGrid(grid, palette, center, spriteSize)
    }

    private fun DrawScope.drawPixelGrid(
        grid: List<String>,
        palette: Map<Char, Color>,
        center: Offset,
        spriteSize: Float
    ) {
        val rows = grid.size
        val cols = grid.firstOrNull()?.length ?: 10
        val pxW = spriteSize / cols
        val pxH = spriteSize / rows
        val startX = center.x - spriteSize * 0.5f
        val startY = center.y - spriteSize * 0.5f

        for ((r, row) in grid.withIndex()) {
            for ((c, ch) in row.withIndex()) {
                val color = palette[ch] ?: continue
                drawRect(
                    color = color,
                    topLeft = Offset(startX + c * pxW, startY + r * pxH),
                    size = Size(pxW + 0.4f, pxH + 0.4f)
                )
            }
        }
    }
}

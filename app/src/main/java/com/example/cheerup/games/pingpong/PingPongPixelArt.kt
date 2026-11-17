package com.example.cheerup.games.pingpong

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

object PingPongPixelArt {

    fun powerUpGrid(type: PingPongPowerUpType): List<String> {
        return when (type) {
            PingPongPowerUpType.GIANT_PADDLE -> listOf(
                "................",
                "....3......3....",
                "...34......43...",
                "..342......243..",
                ".34221111112243.",
                "3422233443322243",
                "3444444444444443",
                "1222233443322221",
                ".12221111112221.",
                "..122......221..",
                "...12......21...",
                "....1......1....",
                "................",
                "................",
                "................",
                "................"
            )
            PingPongPowerUpType.SPEED_BOOST -> listOf(
                ".......333333...",
                "......3444422...",
                ".....3444322....",
                "....3443221.....",
                "...3443221......",
                "..34444444433...",
                ".344444444432...",
                ".111113443221...",
                ".....3443221....",
                "....344321......",
                "...343221.......",
                "..34221.........",
                ".3421...........",
                ".31.............",
                "................",
                "................"
            )
            PingPongPowerUpType.SLOW_MOTION -> listOf(
                "..333333333333..",
                "..122222222221..",
                "...1344444431...",
                "...1324444231...",
                "....13244231....",
                ".....134431.....",
                "......1331......",
                "......1341......",
                ".....13..31.....",
                "....13..4.31....",
                "...13..44..31...",
                "...1324444231...",
                "...1344444431...",
                "..122222222221..",
                "..333333333333..",
                "................"
            )
            PingPongPowerUpType.STICKY_PADDLE -> listOf(
                "....4......4....",
                "..4444....4444..",
                "..4444....4444..",
                "..3333....3333..",
                "..2221....1222..",
                "..2321....1232..",
                "..2321....1232..",
                "..2321....1232..",
                "..23211..11232..",
                "..232222222232..",
                "...2332222332...",
                "....12222221....",
                ".....111111.....",
                "................",
                "................",
                "................"
            )
            PingPongPowerUpType.DOUBLE_POINTS -> listOf(
                "................",
                "................",
                ".........34443..",
                "........3444443.",
                "........342.243.",
                "43...34.11..342.",
                "243.342....3421.",
                ".24342....3421..",
                "..242....3421...",
                ".34243..3421....",
                "342.243.3444443.",
                "21...12.2222222.",
                ".1...1..1111111.",
                "................",
                "................",
                "................"
            )
            PingPongPowerUpType.SHIELD -> listOf(
                "......3443......",
                "..333324423333..",
                ".34442233224443.",
                ".34111244211143.",
                ".32122244222123.",
                ".32124444442123.",
                ".32124444442123.",
                ".32122244222123.",
                "..321124421123..",
                "..132124421231..",
                "...1322442231...",
                "....13244231....",
                ".....134431.....",
                "......1331......",
                ".......11.......",
                "................"
            )
            PingPongPowerUpType.CHAOS_BALL -> listOf(
                "................",
                ".....344443.....",
                "...3344333433...",
                "...3432112343...",
                "...3321..1233...",
                "...111...3432...",
                "........34321...",
                ".......34321....",
                "......34321.....",
                "......3322......",
                "......1221......",
                "................",
                "......3443......",
                "......3322......",
                "......1221......",
                "................"
            )
            PingPongPowerUpType.DOUBLE_BALL -> listOf(
                "................",
                "...333..........",
                "..34433.........",
                ".3443333........",
                ".3333333........",
                ".3333322........",
                "..33322.........",
                "...322..........",
                "..........333...",
                ".........34433..",
                "........3443333.",
                "........3333333.",
                "........3333322.",
                ".........33322..",
                "..........322...",
                "................"
            )
        }
    }

    fun powerUpPalette(type: PingPongPowerUpType): Map<Char, Color> {
        return when (type) {
            PingPongPowerUpType.CHAOS_BALL -> mapOf(
                '1' to Color(0xFF5B21B6),
                '2' to Color(0xFF9333EA),
                '3' to Color(0xFFA855F7),
                '4' to Color(0xFFE9D5FF)
            )
            PingPongPowerUpType.DOUBLE_BALL -> mapOf(
                '1' to Color(0xFF8FAAC7),
                '2' to Color(0xFFB8D1E6),
                '3' to Color(0xFFF5FAFF),
                '4' to Color.White
            )
            else -> {
                val (dark, light) = when (type) {
                    PingPongPowerUpType.GIANT_PADDLE -> Color(0xFF137A4B) to Color(0xFF8EF7C2)
                    PingPongPowerUpType.SPEED_BOOST -> Color(0xFFA31628) to Color(0xFFFF9E80)
                    PingPongPowerUpType.SLOW_MOTION -> Color(0xFF1B3FA8) to Color(0xFF99C2FF)
                    PingPongPowerUpType.STICKY_PADDLE -> Color(0xFFB87D0E) to Color(0xFFFFE680)
                    PingPongPowerUpType.DOUBLE_POINTS -> Color(0xFFB8256E) to Color(0xFFFFD1E8)
                    PingPongPowerUpType.SHIELD -> Color(0xFF006B8F) to Color(0xFF8CEFFF)
                    else -> Color.Black to Color.White
                }
                mapOf(
                    '1' to dark,
                    '2' to type.color,
                    '3' to light,
                    '4' to Color.White
                )
            }
        }
    }

    fun DrawScope.drawPowerUpPixelIcon(
        type: PingPongPowerUpType,
        center: Offset,
        iconSize: Float,
        alpha: Float = 1.0f
    ) {
        val grid = powerUpGrid(type)
        val palette = powerUpPalette(type)
        val rows = grid.size
        val cols = grid.firstOrNull()?.length ?: 16
        val pxW = iconSize / cols
        val pxH = iconSize / rows
        val startX = center.x - iconSize * 0.5f
        val startY = center.y - iconSize * 0.5f

        for ((r, row) in grid.withIndex()) {
            for ((c, ch) in row.withIndex()) {
                val color = palette[ch] ?: continue
                drawRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(startX + c * pxW, startY + r * pxH),
                    size = Size(pxW + 0.4f, pxH + 0.4f)
                )
            }
        }
    }
}

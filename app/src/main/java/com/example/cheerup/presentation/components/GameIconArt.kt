package com.example.cheerup.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.cheerup.R
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood

@Composable
fun GameIconArt(
    game: GameIdentifier,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (game) {
            GameIdentifier.TAP -> {
                CheerUpMascotView(mood = MascotMood.HAPPY, size = size * 0.82f)
            }
            GameIdentifier.PING_PONG -> {
                PingPongIconArtwork(size = size)
            }
            GameIdentifier.GRAVITY -> {
                GravityIconArtwork(size = size)
            }
            GameIdentifier.RUSH -> {
                RushIconArtwork(size = size)
            }
            GameIdentifier.WHEELS -> {
                WheelsIconArtwork(size = size)
            }
            GameIdentifier.CHESS -> {
                ChessIconArtwork(size = size)
            }
        }
    }
}

@Composable
private fun PingPongIconArtwork(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width
        val boxW = s * 0.80f
        val boxLeft = (s - boxW) * 0.5f
        val corner = s * 0.18f

        // Mini Table Surface (#11212D)
        drawRoundRect(
            color = Color(0xFF11212D),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Dashed Net (5 dashes)
        val dashW = s * 0.08f
        val dashH = s * 0.03f
        val gap = s * 0.04f
        val totalNetW = 5 * dashW + 4 * gap
        val startX = (s - totalNetW) * 0.5f
        val netY = (s - dashH) * 0.5f
        for (i in 0 until 5) {
            drawRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(startX + i * (dashW + gap), netY),
                size = Size(dashW, dashH)
            )
        }

        // Top CPU Paddle (Crimson #FF4B5C)
        val topW = s * 0.34f
        val padH = s * 0.09f
        drawRoundRect(
            color = Color(0xFFFF4B5C),
            topLeft = Offset(s * 0.5f + s * 0.10f - topW * 0.5f, s * 0.5f - s * 0.25f - padH * 0.5f),
            size = Size(topW, padH),
            cornerRadius = CornerRadius(padH * 0.5f, padH * 0.5f)
        )

        // Bottom Player Paddle (Teal #00A896)
        val botW = s * 0.38f
        drawRoundRect(
            color = Color(0xFF00A896),
            topLeft = Offset(s * 0.5f - s * 0.08f - botW * 0.5f, s * 0.5f + s * 0.25f - padH * 0.5f),
            size = Size(botW, padH),
            cornerRadius = CornerRadius(padH * 0.5f, padH * 0.5f)
        )

        // Pixel Ball Trail + Ball (#FFC83D)
        drawCircle(
            color = Color(0xFFFFC83D).copy(alpha = 0.35f),
            radius = s * 0.05f,
            center = Offset(s * 0.5f - s * 0.05f, s * 0.5f + s * 0.11f)
        )
        drawCircle(
            color = Color(0xFFFFC83D).copy(alpha = 0.4f),
            radius = s * 0.09f,
            center = Offset(s * 0.5f + s * 0.04f, s * 0.5f - s * 0.03f)
        )
        drawCircle(
            color = Color(0xFFFFC83D),
            radius = s * 0.07f,
            center = Offset(s * 0.5f + s * 0.04f, s * 0.5f - s * 0.03f)
        )
    }
}

@Composable
private fun GravityIconArtwork(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width
        val boxW = s * 0.80f
        val boxLeft = (s - boxW) * 0.5f
        val corner = s * 0.18f

        // Enchanted Forest Torus Frame (#0B1E15)
        drawRoundRect(
            color = Color(0xFF0B1E15),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = Color(0xFF22D3EE).copy(alpha = 0.45f),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Pixel Serpent Body Segments (L-shaped coil)
        val seg = s * 0.15f
        val cx = s * 0.5f
        val cy = s * 0.5f
        val r3 = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        drawRoundRect(
            color = Color(0xFF059669),
            topLeft = Offset(cx - seg * 1.1f - seg * 0.5f, cy + seg * 1.1f - seg * 0.5f),
            size = Size(seg, seg),
            cornerRadius = r3
        )
        drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(cx - seg * 1.1f - seg * 0.5f, cy - seg * 0.5f),
            size = Size(seg, seg),
            cornerRadius = r3
        )
        drawRoundRect(
            color = Color(0xFF059669),
            topLeft = Offset(cx - seg * 1.1f - seg * 0.5f, cy - seg * 1.1f - seg * 0.5f),
            size = Size(seg, seg),
            cornerRadius = r3
        )

        // Serpent Head
        val headS = seg * 1.12f
        drawRoundRect(
            color = Color(0xFF34D399),
            topLeft = Offset(cx - headS * 0.5f, cy - seg * 1.1f - headS * 0.5f),
            size = Size(headS, headS),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Glowing Fire & Mystical Wind Motes
        val moteR = s * 0.07f
        val fireC = Offset(cx + s * 0.19f, cy - s * 0.16f)
        drawCircle(color = Color(0xFFF59E0B).copy(alpha = 0.35f), radius = moteR * 1.4f, center = fireC)
        drawCircle(color = Color(0xFFF59E0B), radius = moteR, center = fireC)

        val purpleC = Offset(cx + s * 0.16f, cy + s * 0.17f)
        drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.35f), radius = moteR * 1.4f, center = purpleC)
        drawCircle(color = Color(0xFFA855F7), radius = moteR, center = purpleC)
    }
}

@Composable
private fun RushIconArtwork(size: Dp) {
    val shipBitmap = ImageBitmap.imageResource(id = R.drawable.rush_ship_4)
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width
        val boxW = s * 0.80f
        val boxLeft = (s - boxW) * 0.5f
        val corner = s * 0.18f

        drawRoundRect(
            color = Color(0xFF090D16),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = Color(0xFFFF2E93).copy(alpha = 0.45f),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val shipW = (s * 0.52f).toInt()
        val shipH = (s * 0.66f).toInt()
        val dstX = ((s - shipW) * 0.5f).toInt()
        val dstY = ((s - shipH) * 0.5f + s * 0.03f).toInt()

        drawImage(
            image = shipBitmap,
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(shipW, shipH),
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
private fun ChessIconArtwork(size: Dp) {
    val knightDark = ImageBitmap.imageResource(id = R.drawable.chess_knight_dark)
    val kingLight = ImageBitmap.imageResource(id = R.drawable.chess_king_light)

    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width
        val boxW = s * 0.80f
        val boxLeft = (s - boxW) * 0.5f
        val corner = s * 0.18f

        // Contained Square Box (#1E1510)
        drawRoundRect(
            color = Color(0xFF1E1510),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = Color(0xFFC9935B).copy(alpha = 0.65f),
            topLeft = Offset(boxLeft, boxLeft),
            size = Size(boxW, boxW),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Subtle warm 2x2 chessboard base inside the badge
        val cell = s * 0.33f
        val gridLeft = (s - cell * 2) * 0.5f
        drawRect(Color(0xFF4A3121), Offset(gridLeft, gridLeft), Size(cell, cell))
        drawRect(Color(0xFF2B1C14), Offset(gridLeft + cell, gridLeft), Size(cell, cell))
        drawRect(Color(0xFF2B1C14), Offset(gridLeft, gridLeft + cell), Size(cell, cell))
        drawRect(Color(0xFF4A3121), Offset(gridLeft + cell, gridLeft + cell), Size(cell, cell))

        // Brown Knight tucked slightly behind on the left
        val kw = (s * 0.34f).toInt()
        val kh = (s * 0.56f).toInt()
        val kx = (s * 0.5f - s * 0.13f - kw * 0.5f).toInt()
        val ky = (s * 0.5f + s * 0.01f - kh * 0.5f).toInt()
        drawImage(
            image = knightDark,
            dstOffset = IntOffset(kx, ky),
            dstSize = IntSize(kw, kh),
            filterQuality = FilterQuality.None
        )

        // Cream King standing proud front-and-center
        val kingW = (s * 0.38f).toInt()
        val kingH = (s * 0.62f).toInt()
        val kingX = (s * 0.5f + s * 0.10f - kingW * 0.5f).toInt()
        val kingY = (s * 0.5f + s * 0.02f - kingH * 0.5f).toInt()
        drawImage(
            image = kingLight,
            dstOffset = IntOffset(kingX, kingY),
            dstSize = IntSize(kingW, kingH),
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
private fun WheelsIconArtwork(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width
        val center = Offset(s * 0.5f, s * 0.5f)
        val r = s * 0.40f

        // Wheel outer shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = s * 0.41f,
            center = Offset(s * 0.5f, s * 0.55f)
        )

        // Outer Wheel with Sweep Gradient
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFF4470B), Color(0xFFF7BD18),
                    Color(0xFF00919B), Color(0xFF7500C8),
                    Color(0xFFDF2955), Color(0xFF3B82F6),
                    Color(0xFF22C55E), Color(0xFFFF7AB6),
                    Color(0xFFF4470B)
                ),
                center = center
            ),
            radius = r,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = r,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Inner dark hub
        drawCircle(
            color = Color(0xFF242033),
            radius = s * 0.14f,
            center = center
        )
        drawCircle(
            color = Color(0xFFFF470B),
            radius = s * 0.06f,
            center = center
        )

        // Top Indicator Needle / Pointer
        val needleY = s * 0.5f - s * 0.38f
        val needleW = s * 0.18f
        val needleH = s * 0.16f
        val needlePath = Path().apply {
            moveTo(s * 0.5f - needleW * 0.5f, needleY - needleH * 0.4f)
            lineTo(s * 0.5f + needleW * 0.5f, needleY - needleH * 0.4f)
            lineTo(s * 0.5f, needleY + needleH * 0.6f)
            close()
        }
        drawPath(needlePath, color = Color.White)
    }
}

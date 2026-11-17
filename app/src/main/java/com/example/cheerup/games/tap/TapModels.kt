package com.example.cheerup.games.tap

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class TapCube(
    val id: Int, // 0: Red, 1: Blue, 2: Green, 3: Yellow
    val name: String,
    val color: Color,
    val hex: Long
) {
    override fun equals(other: Any?): Boolean =
        other is TapCube && this.id == other.id

    override fun hashCode(): Int = id
}

enum class TapRuleType(val rawValue: String) {
    TAP("tap"),
    NOT("not"),
    INK("ink"),
    PREV("prev"),
    LIE("lie")
}

enum class TapConfettiShape {
    RIBBON,
    SQUARE,
    DIAMOND
}

data class TapPixelParticle(
    val id: UUID = UUID.randomUUID(),
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Double,
    var vr: Double,
    var tilt: Double = 0.0,
    var tiltSpeed: Double = 9.0,
    var swayPhase: Double = 0.0,
    var swaySpeed: Double = 7.5,
    var swayAmplitude: Float = 26f,
    var drag: Double = 0.06,
    var gravity: Float = 580f,
    var size: Float,
    var aspect: Float = 0.52f,
    var shape: TapConfettiShape = TapConfettiShape.RIBBON,
    var color: Color,
    var age: Double = 0.0,
    var lifetime: Double = 1.35,
    var opacity: Float = 1.0f
)

data class TapBurstRing(
    val id: UUID = UUID.randomUUID(),
    val x: Float,
    val y: Float,
    val color: Color,
    var radius: Float = 22f,
    val startRadius: Float = 22f,
    var maxRadius: Float = 135f,
    var age: Double = 0.0,
    var duration: Double = 0.42,
    var opacity: Float = 0.85f
)

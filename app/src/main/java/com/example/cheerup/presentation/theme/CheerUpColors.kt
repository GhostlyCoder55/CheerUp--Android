package com.example.cheerup.presentation.theme

import androidx.compose.ui.graphics.Color

object CheerUpColors {
    // MARK: Main App Palette (Dark Slate Gradient)
    val deepNavy    = Color(0xFF06141B) // #06141B
    val darkBlue    = Color(0xFF11212D) // #11212D
    val blueGrey    = Color(0xFF253745) // #253745
    val slate       = Color(0xFF4A5C6A) // #4A5C6A
    val mutedSilver = Color(0xFF9BA8AB) // #9BA8AB
    val lightGrey   = Color(0xFFCCD0CF) // #CCD0CF

    // Official Unified Background Gradient for all Games & Exhibits
    val backgroundGradient: List<Color> = listOf(
        Color(0xFF253745),
        Color(0xFF11212D),
        Color(0xFF06141B)
    )

    // MARK: - Tap Palette
    object Tap {
        val red    = Color(0xFFFF4B5C) // #ff4b5c
        val blue   = Color(0xFF3D7BFF) // #3d7bff
        val green  = Color(0xFF2EDB8A) // #2edb8a
        val yellow = Color(0xFFFFC83D) // #ffc83d

        val bgGradient: List<Color> = backgroundGradient
        val bg    = Color(0xFF06141B)
        val panel = Color(0xFF11212D)
        val pill  = Color(0xFF253745)
        val ink   = Color(0xFFCCD0CF)
    }

    // MARK: - Rush Palette (Hyper Light Drifter / Celeste Indie Pixel Palette)
    object Rush {
        val magenta = Color(0xFFFF2E93)
        val cyan    = Color(0xFF22D3EE)
        val gold    = Color(0xFFFFC83D)
        val deep    = Color(0xFF090D16)
    }

    // MARK: - Wheels Palette
    object Wheels {
        val orange  = Color(0xFFF4470B)
        val amber   = Color(0xFFF7BD18)
        val teal    = Color(0xFF00919B)
        val purple  = Color(0xFF7500C8)
        val crimson = Color(0xFFDF2955)
        val blue    = Color(0xFF3B82F6)
        val green   = Color(0xFF22C55E)
        val pink    = Color(0xFFFF7AB6)
        val gold    = Color(0xFFFFC400)
    }

    // MARK: - Game Gradients (Unified Luminance & Depth across all Arcade Cards)
    object GameGradient {
        val tap: List<Color> = listOf(
            Color(0xFF73A9AB), Color(0xFF9E6B6A)
        )
        val pingPong: List<Color> = listOf(
            Color(0xFF00A896), Color(0xFF253745)
        )
        val gravity: List<Color> = listOf(
            Color(0xFF0EA371), Color(0xFF163B2C)
        )
        val rush: List<Color> = listOf(
            Color(0xFFFF3396), Color(0xFF3D2356)
        )
        val chess: List<Color> = listOf(
            Color(0xFFC8A57B), Color(0xFF8C5835), Color(0xFF42281D)
        )
        val wheels: List<Color> = listOf(
            Color(0xFFF75C22), Color(0xFF432357)
        )
    }

    fun fromHex(hex: Long, alpha: Float = 1.0f): Color {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        return Color(red = r, green = g, blue = b, alpha = alpha)
    }
}

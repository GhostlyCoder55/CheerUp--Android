package com.example.cheerup.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CheerUpFont {
    private val roundedFamily = FontFamily.SansSerif

    // MARK: Headings
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = roundedFamily)
    val title      = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = roundedFamily)
    val title2     = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = roundedFamily)
    val title3     = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = roundedFamily)

    // MARK: Body
    val headline   = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = roundedFamily)
    val body       = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = roundedFamily)
    val callout    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = roundedFamily)
    val caption    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = roundedFamily)
    val caption2   = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = roundedFamily)

    // MARK: Game-specific
    val scoreDisplay    = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Black, fontFamily = roundedFamily)
    val gameInstruction = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = roundedFamily)
    val gameStat        = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = roundedFamily)
    val gameLabel       = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = roundedFamily)
}

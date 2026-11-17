package com.example.cheerup.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.presentation.components.CheerUpCircleIconButton
import com.example.cheerup.presentation.components.GameCardView
import com.example.cheerup.presentation.settings.SettingsSheet
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing
import kotlinx.coroutines.delay

private val HOME_GAMES: List<Pair<GameIdentifier, List<Color>>> = listOf(
    GameIdentifier.TAP to CheerUpColors.GameGradient.tap,
    GameIdentifier.PING_PONG to CheerUpColors.GameGradient.pingPong,
    GameIdentifier.GRAVITY to CheerUpColors.GameGradient.gravity,
    GameIdentifier.RUSH to CheerUpColors.GameGradient.rush,
    GameIdentifier.WHEELS to CheerUpColors.GameGradient.wheels,
    GameIdentifier.CHESS to CheerUpColors.GameGradient.chess
)

@Composable
fun ArcadeHomeScreen(
    onSelectGame: (GameIdentifier) -> Unit
) {
    val highScores by ScoreManager.shared.scoresFlow.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val currentLang = LanguageManager.shared.currentLanguage // Observe language for recomposition

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
            .systemBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenPadding, vertical = Spacing.xs)
        ) {
            val totalHeight = maxHeight
            val cardSpacing = when {
                totalHeight < 700.dp -> 7.dp
                totalHeight < 800.dp -> 8.dp
                else -> 10.dp
            }
            val headerBottomSpacing = when {
                totalHeight < 700.dp -> 6.dp
                totalHeight < 800.dp -> 10.dp
                else -> 12.dp
            }
            val cardCount = HOME_GAMES.size
            val reserved = 64.dp + headerBottomSpacing + (cardSpacing * (cardCount - 1).coerceAtLeast(0)) + 12.dp
            val computedCardHeight = ((totalHeight - reserved) / cardCount.coerceAtLeast(1))
                .coerceIn(68.dp, 112.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = headerBottomSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CheerUp!",
                            style = CheerUpFont.largeTitle,
                            color = CheerUpColors.lightGrey
                        )
                        Text(
                            text = LanguageManager.shared.tr(
                                "Your little arcade corner.",
                                "Votre coin d’arcade.",
                                "Tu rincón arcade."
                            ),
                            style = CheerUpFont.body,
                            color = CheerUpColors.mutedSilver
                        )
                    }

                    CheerUpCircleIconButton(
                        symbol = "gearshape.fill",
                        size = 40.dp,
                        iconSize = 17.dp,
                        backgroundColor = CheerUpColors.blueGrey.copy(alpha = 0.4f),
                        tint = CheerUpColors.mutedSilver,
                        contentDesc = LanguageManager.shared.tr("Settings", "Réglages", "Ajustes"),
                        onClick = { showSettings = true }
                    )
                }

                // Vertical Presentation of Mini Games & Visualizer (100% Static - No scrolling)
                Column(
                    verticalArrangement = Arrangement.spacedBy(cardSpacing)
                ) {
                    HOME_GAMES.forEachIndexed { index, (game, gradient) ->
                        val cardAlpha = remember { Animatable(0f) }
                        val cardOffsetY = remember { Animatable(25f) }

                        LaunchedEffect(Unit) {
                            delay(80L + index * 70L)
                            cardAlpha.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                        LaunchedEffect(Unit) {
                            delay(80L + index * 70L)
                            cardOffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                        }

                        // Reference currentLang so cards recompose on language switch
                        val _lang = currentLang
                        GameCardView(
                            game = game,
                            highScore = highScores[game] ?: ScoreManager.shared.highScore(game),
                            gradientColors = gradient,
                            cardHeight = computedCardHeight,
                            modifier = Modifier.graphicsLayer {
                                alpha = cardAlpha.value
                                translationY = cardOffsetY.value * density
                            },
                            onSelect = { onSelectGame(game) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    if (showSettings) {
        SettingsSheet(onDismiss = { showSettings = false })
    }
}

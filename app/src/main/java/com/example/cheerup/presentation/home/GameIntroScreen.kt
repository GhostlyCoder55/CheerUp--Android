package com.example.cheerup.presentation.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.core.score.ScoreManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.leaderboard.PlayGamesManager
import com.example.cheerup.presentation.components.CheerUpPrimaryButton
import com.example.cheerup.presentation.components.CheerUpSecondaryButton
import com.example.cheerup.presentation.components.CheerUpSymbolIcon
import com.example.cheerup.presentation.components.GameIconArt
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing

@Composable
fun GameIntroScreen(
    game: GameIdentifier,
    gradientColors: List<Color>,
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val badgeScale = remember { Animatable(0.6f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffset = remember { Animatable(24f) }

    LaunchedEffect(game) {
        badgeScale.animateTo(1.0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(game) {
        contentAlpha.animateTo(1.0f, spring(stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(game) {
        contentOffset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
            .systemBarsPadding()
            .padding(horizontal = Spacing.screenPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxxl)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Custom Game Art Icon Badge (120 x 120)
            val badgeShape = RoundedCornerShape(32.dp)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                        alpha = contentAlpha.value
                    }
                    .shadow(
                        elevation = 20.dp,
                        shape = badgeShape,
                        ambientColor = gradientColors.firstOrNull() ?: Color.Black,
                        spotColor = gradientColors.firstOrNull() ?: Color.Black
                    )
                    .clip(badgeShape)
                    .background(
                        Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset.Zero,
                            end = Offset(300f, 300f)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.2f), badgeShape),
                contentAlignment = Alignment.Center
            ) {
                GameIconArt(game = game, size = 84.dp)
            }

            // Text & High Score
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = contentOffset.value * density
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = game.title,
                    style = CheerUpFont.largeTitle,
                    color = CheerUpColors.lightGrey
                )

                Text(
                    text = game.shortDescription,
                    style = CheerUpFont.body,
                    color = CheerUpColors.mutedSilver,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp)
                )

                if (game.isGame) {
                    val hs = ScoreManager.shared.highScore(game)
                    if (hs > 0) {
                        Row(
                            modifier = Modifier
                                .padding(top = Spacing.sm)
                                .clip(CircleShape)
                                .clickable {
                                    (context as? Activity)?.let { act ->
                                        PlayGamesManager.shared.showLeaderboard(act, game)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheerUpSymbolIcon(
                                symbol = "trophy.fill",
                                tint = Color(0xFFFFD60A).copy(alpha = 0.9f),
                                size = 14.dp
                            )
                            Text(
                                text = "${LanguageManager.shared.tr("Best", "Record", "Récord")}: $hs",
                                style = CheerUpFont.callout,
                                color = Color(0xFFFFD60A).copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffset.value * density
                    },
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                CheerUpPrimaryButton(
                    title = if (!game.isGame) {
                        LanguageManager.shared.tr("Open Exhibit", "Ouvrir", "Abrir")
                    } else {
                        LanguageManager.shared.tr("Play", "Jouer", "Jugar")
                    },
                    icon = if (!game.isGame) "slider.horizontal.3" else "play.fill",
                    action = onPlay
                )

                CheerUpSecondaryButton(
                    title = LanguageManager.shared.tr("Back", "Retour", "Atrás"),
                    icon = "chevron.left",
                    action = onBack
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

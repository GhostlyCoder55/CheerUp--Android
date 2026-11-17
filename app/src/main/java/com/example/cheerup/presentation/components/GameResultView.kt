package com.example.cheerup.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cheerup.ads.RewardedAdManager
import com.example.cheerup.ads.RewardedAdState
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@Composable
fun GameResultView(
    gameTitle: String,
    score: Int,
    highScore: Int,
    accentColor: Color,
    onReplay: () -> Unit,
    onExit: () -> Unit,
    onWatchAdContinue: (() -> Unit)? = null
) {
    var displayedScore by remember { mutableIntStateOf(0) }
    val isNewHighScore = remember(score, highScore) { score > highScore && score > 0 }
    var adStatusMessage by remember { mutableStateOf<String?>(null) }
    val adManager = RewardedAdManager.shared
    val popScale = remember { Animatable(0.65f) }

    LaunchedEffect(score) {
        popScale.animateTo(1.0f, spring(dampingRatio = 0.65f))
    }

    LaunchedEffect(score, onWatchAdContinue != null) {
        if (onWatchAdContinue != null) {
            adManager.preloadIfNeeded()
        }
        val steps = min(score, 30)
        if (steps <= 0) {
            displayedScore = score
            if (isNewHighScore) {
                HapticsManager.shared.success()
            }
            return@LaunchedEffect
        }
        for (i in 0..steps) {
            displayedScore = ((score.toDouble() * i.toDouble()) / steps.toDouble()).toInt()
            if (i == steps) {
                displayedScore = score
                if (isNewHighScore) {
                    HapticsManager.shared.success()
                }
            }
            delay(20)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.screenPadding)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                }
                .clip(cardShape)
                .background(CheerUpColors.darkBlue)
                .border(1.dp, CheerUpColors.blueGrey.copy(alpha = 0.5f), cardShape)
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Title
            Text(
                text = gameTitle,
                style = CheerUpFont.title2,
                color = CheerUpColors.lightGrey
            )

            // Score Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = LanguageManager.shared.tr("FINAL SCORE", "SCORE FINAL", "PUNTUACIÓN"),
                    style = CheerUpFont.caption.copy(letterSpacing = 2.sp),
                    color = CheerUpColors.mutedSilver
                )

                Text(
                    text = "$displayedScore",
                    style = CheerUpFont.scoreDisplay,
                    color = accentColor
                )

                if (isNewHighScore) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Yellow.copy(alpha = 0.15f))
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheerUpSymbolIcon(
                            symbol = "star.fill",
                            tint = Color.Yellow,
                            size = 12.dp
                        )
                        Text(
                            text = LanguageManager.shared.tr("NEW BEST!", "NOUVEAU RECORD !", "¡NUEVO RÉCORD!"),
                            style = CheerUpFont.caption,
                            color = Color.Yellow
                        )
                    }
                }
            }

            // Best Score
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheerUpSymbolIcon(
                    symbol = "trophy.fill",
                    tint = CheerUpColors.mutedSilver,
                    size = 14.dp
                )
                Text(
                    text = "${LanguageManager.shared.tr("Best", "Record", "Récord")}: ${max(score, highScore)}",
                    style = CheerUpFont.callout,
                    color = CheerUpColors.mutedSilver
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onWatchAdContinue != null) {
                    val btnShape = RoundedCornerShape(Spacing.buttonCornerRadius)
                    val isBusy = adManager.state.isBusy

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(btnShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.85f))
                                )
                            )
                            .clickable(enabled = !isBusy) {
                                HapticsManager.shared.selection()
                                adStatusMessage = null
                                adManager.presentRewardedAd(
                                    onRewardEarned = {
                                        onWatchAdContinue()
                                    },
                                    onDismissedWithoutReward = {
                                        adStatusMessage = if (adManager.state is RewardedAdState.Failed) {
                                            LanguageManager.shared.tr(
                                                "Video unavailable right now. You can still play again!",
                                                "Vidéo indisponible pour le moment. Tu peux rejouer !",
                                                "Video no disponible ahora. ¡Aún puedes volver a jugar!"
                                            )
                                        } else {
                                            LanguageManager.shared.tr(
                                                "Watch the full video to earn an extra try.",
                                                "Regarde la vidéo en entier pour gagner un essai.",
                                                "Mira el video completo para ganar otro intento."
                                            )
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CheerUpColors.deepNavy,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                CheerUpSymbolIcon(
                                    symbol = "play.rectangle.fill",
                                    tint = CheerUpColors.deepNavy,
                                    size = 16.dp
                                )
                            }
                            Text(
                                text = LanguageManager.shared.tr(
                                    "Watch Video – Continue",
                                    "Voir une vidéo – Continuer",
                                    "Ver vídeo – Continuar"
                                ),
                                style = CheerUpFont.headline,
                                color = CheerUpColors.deepNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    adStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = CheerUpFont.caption,
                            color = CheerUpColors.mutedSilver,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                CheerUpPrimaryButton(
                    title = LanguageManager.shared.tr("Play Again", "Rejouer", "Jugar de nuevo"),
                    icon = "arrow.clockwise",
                    action = onReplay
                )

                CheerUpSecondaryButton(
                    title = LanguageManager.shared.tr("Arcade", "Arcade", "Arcade"),
                    icon = "square.grid.2x2.fill",
                    action = onExit
                )
            }
        }
    }
}

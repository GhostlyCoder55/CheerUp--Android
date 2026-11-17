package com.example.cheerup.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing

@Composable
fun GameCardView(
    game: GameIdentifier,
    highScore: Int,
    gradientColors: List<Color>,
    cardHeight: Dp = 114.dp,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "gameCardScale"
    )

    val cardShape = RoundedCornerShape(Spacing.cardCornerRadius)
    val isCompact = cardHeight < 118.dp
    val iconBoxSize = if (isCompact) 74.dp else 84.dp
    val iconArtSize = if (isCompact) 60.dp else 70.dp
    val iconCorner = if (isCompact) 18.dp else 22.dp
    val iconBoxShape = RoundedCornerShape(iconCorner)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isPressed) 0.88f else 1.0f
            }
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                ambientColor = gradientColors.firstOrNull() ?: Color.Black,
                spotColor = gradientColors.firstOrNull() ?: Color.Black
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset.Zero,
                    end = Offset(1000f, 400f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    HapticsManager.shared.selection()
                    onSelect()
                }
            )
            .semantics {
                contentDescription = "${game.title}. ${game.subtitle}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) Spacing.md else Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Game Icon Artwork Badge
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(iconBoxShape)
                    .background(Color.Black.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), iconBoxShape),
                contentAlignment = Alignment.Center
            ) {
                GameIconArt(game = game, size = iconArtSize)
            }

            // Text Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = game.title,
                    style = if (cardHeight < 108.dp) CheerUpFont.headline else CheerUpFont.title2,
                    color = Color.White
                )

                Text(
                    text = game.subtitle,
                    style = CheerUpFont.caption,
                    color = Color.White.copy(alpha = 0.82f)
                )

                Spacer(modifier = Modifier.weight(1f, fill = false))

                if (!game.isGame) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheerUpSymbolIcon(
                            symbol = game.symbol,
                            tint = Color.White.copy(alpha = 0.9f),
                            size = 10.dp
                        )
                        Text(
                            text = LanguageManager.shared.tr("Interactive Exhibit", "Expo Interactive", "Expo Interactiva"),
                            style = CheerUpFont.caption2,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                } else if (highScore > 0) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheerUpSymbolIcon(
                            symbol = "trophy.fill",
                            tint = Color.White.copy(alpha = 0.9f),
                            size = 10.dp
                        )
                        Text(
                            text = "${LanguageManager.shared.tr("Best", "Record", "Récord")}: $highScore",
                            style = CheerUpFont.caption2,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            CheerUpSymbolIcon(
                symbol = "chevron.right",
                tint = Color.White.copy(alpha = 0.55f),
                size = 15.dp,
                modifier = Modifier.padding(end = Spacing.xs)
            )
        }
    }
}

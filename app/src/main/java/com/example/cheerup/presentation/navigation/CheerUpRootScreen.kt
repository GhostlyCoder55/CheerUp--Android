package com.example.cheerup.presentation.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.AppLanguage
import com.example.cheerup.domain.model.GameIdentifier
import com.example.cheerup.domain.model.MascotMood
import com.example.cheerup.games.chess.ChessScreen
import com.example.cheerup.games.gravity.GravityScreen
import com.example.cheerup.games.pingpong.PingPongScreen
import com.example.cheerup.games.rush.RushScreen
import com.example.cheerup.games.tap.TapScreen
import com.example.cheerup.games.wheels.WheelsScreen
import com.example.cheerup.notifications.NotificationManager
import com.example.cheerup.presentation.components.CheerUpMascotView
import com.example.cheerup.presentation.components.CheerUpSymbolIcon
import com.example.cheerup.presentation.home.ArcadeHomeScreen
import com.example.cheerup.presentation.home.GameIntroScreen
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing

@Composable
fun CheerUpRootScreen() {
    val languageManager = LanguageManager.shared
    val hasCompletedOnboarding = languageManager.hasCompletedLanguageOnboarding
    val appState = remember { AppState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, hasCompletedOnboarding) {
        val observer = LifecycleEventObserver { _, event ->
            if (hasCompletedOnboarding &&
                (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_STOP)
            ) {
                NotificationManager.shared.refreshScheduleIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
    ) {
        if (!hasCompletedOnboarding) {
            LanguageOnboardingView(
                onSelectLanguage = { chosenLanguage ->
                    languageManager.selectInitialLanguage(chosenLanguage)
                }
            )
        } else {
            ArcadeRootView(appState = appState)
        }
    }
}

@Composable
fun LanguageOnboardingView(
    onSelectLanguage: (AppLanguage) -> Unit
) {
    var mascotMood by remember { mutableStateOf(MascotMood.HAPPY) }
    var appeared by remember { mutableStateOf(false) }

    val options = remember {
        listOf(
            AppLanguage.ENGLISH to "English",
            AppLanguage.FRENCH to "Français",
            AppLanguage.SPANISH to "Español"
        )
    }

    LaunchedEffect(Unit) {
        appeared = true
    }

    val mascotScale by animateFloatAsState(
        targetValue = if (appeared) 1.0f else 0.6f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "onboardingMascotScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(450),
        label = "onboardingAlpha"
    )
    val bubbleOffsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 15f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "bubbleOffsetY"
    )
    val buttonsOffsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 25f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "buttonsOffsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
            .padding(horizontal = Spacing.screenPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // MiniCheerUp! Mascot & Speech Bubble
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .scale(mascotScale)
                        .alpha(contentAlpha)
                ) {
                    CheerUpMascotView(mood = mascotMood, size = 96.dp)
                }

                Box(
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .offset(y = bubbleOffsetY.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(CheerUpColors.lightGrey)
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Hi! I'm MiniCheerUp!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = CheerUpColors.deepNavy
                    )
                }
            }

            // Language Selection Buttons (English, Français, Español)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenPadding)
                    .alpha(contentAlpha)
                    .offset(y = buttonsOffsetY.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                options.forEach { (lang, title) ->
                    LanguageOptionButton(
                        title = title,
                        onClick = {
                            HapticsManager.shared.selection()
                            AudioManager.shared.play(AudioManager.SoundEffect.POP)
                            mascotMood = MascotMood.PARTY
                            onSelectLanguage(lang)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LanguageOptionButton(
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "langOptionScale"
    )
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isPressed) 0.88f else 1.0f
            }
            .clip(shape)
            .background(CheerUpColors.darkBlue.copy(alpha = 0.9f))
            .border(
                width = 1.5.dp,
                color = CheerUpColors.blueGrey.copy(alpha = 0.6f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = CheerUpFont.title3,
            color = CheerUpColors.lightGrey
        )
        Spacer(modifier = Modifier.weight(1f))
        CheerUpSymbolIcon(
            symbol = "chevron.right",
            tint = CheerUpColors.mutedSilver,
            size = 15.dp
        )
    }
}

private data class RootNavState(
    val selectedGame: GameIdentifier?,
    val showingIntro: Boolean,
    val isPlayingGame: Boolean
)

@Composable
fun ArcadeRootView(appState: AppState) {
    val context = LocalContext.current
    val selectedGame = appState.selectedGame
    val showingIntro = appState.showingIntro
    val isPlayingGame = appState.isPlayingGame

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        NotificationManager.shared.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted && !NotificationManager.shared.hasRequestedPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationManager.shared.refreshScheduleIfNeeded()
            }
        } else {
            NotificationManager.shared.refreshScheduleIfNeeded()
        }
    }

    BackHandler(enabled = isPlayingGame || showingIntro) {
        appState.exitToArcade()
    }

    val gradients = remember {
        mapOf(
            GameIdentifier.TAP to CheerUpColors.GameGradient.tap,
            GameIdentifier.PING_PONG to CheerUpColors.GameGradient.pingPong,
            GameIdentifier.GRAVITY to CheerUpColors.GameGradient.gravity,
            GameIdentifier.RUSH to CheerUpColors.GameGradient.rush,
            GameIdentifier.CHESS to CheerUpColors.GameGradient.chess,
            GameIdentifier.WHEELS to CheerUpColors.GameGradient.wheels
        )
    }

    val navState = RootNavState(selectedGame, showingIntro, isPlayingGame)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(CheerUpColors.backgroundGradient))
    ) {
        AnimatedContent(
            targetState = navState,
            transitionSpec = {
                fadeIn(tween(350)) togetherWith fadeOut(tween(350))
            },
            label = "arcadeRootNavigation"
        ) { state ->
            val game = state.selectedGame
            when {
                state.isPlayingGame && game != null -> {
                    when (game) {
                        GameIdentifier.TAP -> TapScreen(onExit = { appState.exitToArcade() })
                        GameIdentifier.PING_PONG -> PingPongScreen(onExit = { appState.exitToArcade() })
                        GameIdentifier.GRAVITY -> GravityScreen(onExit = { appState.exitToArcade() })
                        GameIdentifier.RUSH -> RushScreen(onExit = { appState.exitToArcade() })
                        GameIdentifier.CHESS -> ChessScreen(onExit = { appState.exitToArcade() })
                        GameIdentifier.WHEELS -> WheelsScreen(onExit = { appState.exitToArcade() })
                    }
                }
                state.showingIntro && game != null -> {
                    GameIntroScreen(
                        game = game,
                        gradientColors = gradients[game] ?: emptyList(),
                        onPlay = { appState.startGame() },
                        onBack = { appState.exitToArcade() }
                    )
                }
                else -> {
                    ArcadeHomeScreen(
                        onSelectGame = { chosen ->
                            appState.selectGame(chosen)
                        }
                    )
                }
            }
        }
    }
}

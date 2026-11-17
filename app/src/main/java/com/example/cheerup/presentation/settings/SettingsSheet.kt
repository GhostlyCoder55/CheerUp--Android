package com.example.cheerup.presentation.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cheerup.BuildConfig
import com.example.cheerup.core.audio.AudioManager
import com.example.cheerup.core.haptics.HapticsManager
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.AppLanguage
import com.example.cheerup.leaderboard.PlayGamesManager
import com.example.cheerup.notifications.NotificationManager
import com.example.cheerup.presentation.components.CheerUpSymbolIcon
import com.example.cheerup.presentation.theme.CheerUpColors
import com.example.cheerup.presentation.theme.CheerUpFont
import com.example.cheerup.presentation.theme.Spacing
import com.example.cheerup.privacy.ConsentManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val languageManager = LanguageManager.shared

    var soundEnabled by remember { mutableStateOf(AudioManager.shared.isEnabled) }
    var hapticsEnabled by remember { mutableStateOf(HapticsManager.shared.isEnabled) }
    var notificationsEnabled by remember { mutableStateOf(NotificationManager.shared.isEnabled) }
    var showNotificationDeniedAlert by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        NotificationManager.shared.onPermissionResult(granted)
        notificationsEnabled = NotificationManager.shared.isEnabled
        if (!granted) {
            showNotificationDeniedAlert = true
        }
    }

    LaunchedEffect(Unit) {
        NotificationManager.shared.refreshAuthorizationStatus()
        notificationsEnabled = if (NotificationManager.shared.authorizationStatus == NotificationManager.AuthorizationStatus.DENIED) {
            false
        } else {
            NotificationManager.shared.isEnabled
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CheerUpColors.darkBlue,
        contentColor = CheerUpColors.lightGrey
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CheerUpColors.darkBlue, CheerUpColors.deepNavy)
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Text(
                text = languageManager.tr("Settings", "Réglages", "Ajustes"),
                style = CheerUpFont.title3,
                color = CheerUpColors.lightGrey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // PREFERENCES Section
            SettingsSection(
                title = languageManager.tr("PREFERENCES", "PRÉFÉRENCES", "PREFERENCIAS")
            ) {
                SettingsToggleRow(
                    title = languageManager.tr("Sound Effects", "Sons", "Sonidos"),
                    symbol = "speaker.wave.2.fill",
                    isOn = soundEnabled,
                    onCheckedChange = { newValue ->
                        soundEnabled = newValue
                        AudioManager.shared.setEnabled(newValue)
                    }
                )

                RowDivider()

                SettingsToggleRow(
                    title = languageManager.tr("Haptic Feedback", "Vibrations", "Vibración"),
                    symbol = "hand.tap.fill",
                    isOn = hapticsEnabled,
                    onCheckedChange = { newValue ->
                        hapticsEnabled = newValue
                        HapticsManager.shared.setEnabled(newValue)
                    }
                )

                RowDivider()

                SettingsToggleRow(
                    title = languageManager.tr("Play Reminders", "Rappels de jeu", "Recordatorios"),
                    symbol = "bell.badge.fill",
                    isOn = notificationsEnabled,
                    onCheckedChange = { newValue ->
                        if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            NotificationManager.shared.authorizationStatus == NotificationManager.AuthorizationStatus.NOT_DETERMINED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val ok = NotificationManager.shared.setEnabled(newValue)
                            if (!ok && newValue) {
                                notificationsEnabled = false
                                showNotificationDeniedAlert = true
                            } else {
                                notificationsEnabled = newValue
                            }
                        }
                    }
                )
            }

            // LANGUAGE Section
            SettingsSection(
                title = languageManager.tr("LANGUAGE", "LANGUE", "IDIOMA")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageMenu = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "globe",
                        tint = CheerUpColors.mutedSilver,
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = languageManager.tr("Language", "Langue", "Idioma"),
                        style = CheerUpFont.body,
                        color = CheerUpColors.lightGrey
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = languageManager.currentLanguage.displayName,
                                style = CheerUpFont.body,
                                color = CheerUpColors.mutedSilver
                            )
                            CheerUpSymbolIcon(
                                symbol = "chevron.down",
                                tint = CheerUpColors.mutedSilver,
                                size = 12.dp
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.background(CheerUpColors.darkBlue)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = lang.displayName,
                                            style = CheerUpFont.body,
                                            color = if (lang == languageManager.currentLanguage) Color.White else CheerUpColors.mutedSilver
                                        )
                                    },
                                    onClick = {
                                        showLanguageMenu = false
                                        HapticsManager.shared.selection()
                                        languageManager.updateLanguage(lang)
                                        NotificationManager.shared.refreshScheduleIfNeeded()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // LEADERBOARDS & PRIVACY Section (when applicable)
            SettingsSection(
                title = languageManager.tr("ABOUT", "À PROPOS", "ACERCA DE")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val act = context as? Activity ?: return@clickable
                            HapticsManager.shared.selection()
                            if (PlayGamesManager.shared.isAuthenticated) {
                                PlayGamesManager.shared.showAllLeaderboards(act)
                            } else {
                                PlayGamesManager.shared.signInInteractively(act) { signedIn ->
                                    if (signedIn) {
                                        PlayGamesManager.shared.showAllLeaderboards(act)
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheerUpSymbolIcon(
                        symbol = "trophy.fill",
                        tint = CheerUpColors.mutedSilver,
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = languageManager.tr("Leaderboards", "Classements", "Clasificaciones"),
                        style = CheerUpFont.body,
                        color = CheerUpColors.lightGrey
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    CheerUpSymbolIcon(
                        symbol = "chevron.right",
                        tint = CheerUpColors.mutedSilver,
                        size = 14.dp
                    )
                }

                if (ConsentManager.shared.isPrivacyOptionsRequired) {
                    RowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                (context as? Activity)?.let { act ->
                                    ConsentManager.shared.showPrivacyOptionsForm(act)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = languageManager.tr("Privacy Choices", "Choix de confidentialité", "Opciones de privacidad"),
                            style = CheerUpFont.body,
                            color = CheerUpColors.lightGrey
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        CheerUpSymbolIcon(
                            symbol = "chevron.right",
                            tint = CheerUpColors.mutedSilver,
                            size = 14.dp
                        )
                    }
                }

                RowDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = languageManager.tr("Version", "Version", "Versión"),
                        style = CheerUpFont.body,
                        color = CheerUpColors.lightGrey
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = CheerUpFont.body,
                        color = CheerUpColors.mutedSilver
                    )
                }
            }
        }
    }

    if (showNotificationDeniedAlert) {
        AlertDialog(
            onDismissRequest = { showNotificationDeniedAlert = false },
            containerColor = CheerUpColors.darkBlue,
            titleContentColor = CheerUpColors.lightGrey,
            textContentColor = CheerUpColors.mutedSilver,
            title = {
                Text(
                    text = languageManager.tr(
                        "Notifications Disabled",
                        "Notifications désactivées",
                        "Notificaciones desactivadas"
                    ),
                    style = CheerUpFont.title3
                )
            },
            text = {
                Text(
                    text = languageManager.tr(
                        "Enable notifications in Android Settings to get a reminder every two days.",
                        "Active les notifications dans les réglages Android pour recevoir un rappel tous les deux jours.",
                        "Activa las notificaciones en los ajustes de Android para recibir un recordatorio cada dos días."
                    ),
                    style = CheerUpFont.body
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationDeniedAlert = false
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(fallback)
                        }
                    }
                ) {
                    Text(
                        text = languageManager.tr("Open Settings", "Ouvrir les réglages", "Abrir Ajustes"),
                        color = CheerUpColors.lightGrey
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDeniedAlert = false }) {
                    Text(
                        text = languageManager.tr("Cancel", "Annuler", "Cancelar"),
                        color = CheerUpColors.mutedSilver
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = CheerUpFont.caption.copy(letterSpacing = 1.1.sp),
            color = CheerUpColors.mutedSilver.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CheerUpColors.blueGrey.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), shape),
            content = content
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    symbol: String,
    isOn: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheerUpSymbolIcon(
            symbol = symbol,
            tint = CheerUpColors.mutedSilver,
            size = 16.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = CheerUpFont.body,
            color = CheerUpColors.lightGrey
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isOn,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CheerUpColors.deepNavy,
                checkedTrackColor = Color(0xFF00A896),
                uncheckedThumbColor = CheerUpColors.mutedSilver,
                uncheckedTrackColor = CheerUpColors.deepNavy.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 16.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

package com.example.cheerup.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as SystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cheerup.MainActivity
import com.example.cheerup.R
import com.example.cheerup.core.localization.LanguageManager
import com.example.cheerup.domain.model.AppLanguage
import java.util.concurrent.TimeUnit

class NotificationManager private constructor() {

    enum class AuthorizationStatus {
        NOT_DETERMINED,
        AUTHORIZED,
        DENIED
    }

    data class ReminderMessage(
        val en: String,
        val fr: String,
        val es: String
    ) {
        fun body(language: AppLanguage): String = when (language) {
            AppLanguage.ENGLISH -> en
            AppLanguage.FRENCH -> fr
            AppLanguage.SPANISH -> es
        }
    }

    data class ReminderContent(
        val title: String,
        val body: String,
        val hasDefaultSound: Boolean = true
    )

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    var isEnabled: Boolean by mutableStateOf(true)
        private set

    var authorizationStatus: AuthorizationStatus by mutableStateOf(AuthorizationStatus.NOT_DETERMINED)
        private set

    val isAuthorized: Boolean
        get() = authorizationStatus == AuthorizationStatus.AUTHORIZED

    var hasRequestedPermission: Boolean
        get() = prefs?.getBoolean(REQUESTED_KEY, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(REQUESTED_KEY, value)?.apply()
        }

    fun initialize(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        val sp = ctx.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        if (!sp.contains(ENABLED_KEY)) {
            sp.edit().putBoolean(ENABLED_KEY, true).apply()
        }
        isEnabled = sp.getBoolean(ENABLED_KEY, true)
        createNotificationChannelIfNeeded(ctx)
        refreshAuthorizationStatus()
    }

    fun refreshAuthorizationStatus() {
        val ctx = appContext ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            authorizationStatus = if (granted) {
                AuthorizationStatus.AUTHORIZED
            } else if (hasRequestedPermission) {
                AuthorizationStatus.DENIED
            } else {
                AuthorizationStatus.NOT_DETERMINED
            }
        } else {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? SystemNotificationManager
            authorizationStatus = if (nm?.areNotificationsEnabled() != false) {
                AuthorizationStatus.AUTHORIZED
            } else {
                AuthorizationStatus.DENIED
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        hasRequestedPermission = true
        authorizationStatus = if (granted) AuthorizationStatus.AUTHORIZED else AuthorizationStatus.DENIED
        if (granted && isEnabled) {
            scheduleTwoDayReminder()
        } else if (!granted) {
            isEnabled = false
            prefs?.edit()?.putBoolean(ENABLED_KEY, false)?.apply()
            cancelPendingReminders()
        }
    }

    fun setEnabled(enabled: Boolean): Boolean {
        isEnabled = enabled
        prefs?.edit()?.putBoolean(ENABLED_KEY, enabled)?.apply()

        if (!enabled) {
            cancelPendingReminders()
            return true
        }

        refreshAuthorizationStatus()
        if (isAuthorized) {
            scheduleTwoDayReminder()
            return true
        }

        isEnabled = false
        prefs?.edit()?.putBoolean(ENABLED_KEY, false)?.apply()
        return false
    }

    /** Resets the 48-hour inactivity timer when the app becomes active or moves to the background. */
    fun refreshScheduleIfNeeded() {
        if (!isEnabled) {
            cancelPendingReminders()
            return
        }
        refreshAuthorizationStatus()
        if (!isAuthorized) return
        scheduleTwoDayReminder()
    }

    /** Schedules a repeating local notification every 2 days (48 hours) via WorkManager. */
    fun scheduleTwoDayReminder() {
        val ctx = appContext ?: return
        try {
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(2, TimeUnit.DAYS)
                .setInitialDelay(2, TimeUnit.DAYS)
                .addTag(REMINDER_IDENTIFIER)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                REMINDER_IDENTIFIER,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } catch (_: Throwable) {
            // Safe no-op in unit tests where WorkManager is not initialized
        }
    }

    fun cancelPendingReminders() {
        val ctx = appContext ?: return
        try {
            WorkManager.getInstance(ctx).cancelUniqueWork(REMINDER_IDENTIFIER)
        } catch (_: Throwable) {
            // Safe no-op in unit tests
        }
    }

    fun makeReminderContent(language: AppLanguage, index: Int? = null): ReminderContent {
        val message = if (index != null && index in reminderMessages.indices) {
            reminderMessages[index]
        } else {
            reminderMessages.random()
        }
        return ReminderContent(
            title = "CheerUp!",
            body = message.body(language),
            hasDefaultSound = true
        )
    }

    internal fun createNotificationChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? SystemNotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Play Reminders",
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle 2-day reminders to jump back into CheerUp! Arcade"
            }
            nm.createNotificationChannel(channel)
        }
    }

    internal fun showReminderNotification(context: Context) {
        createNotificationChannelIfNeeded(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val sp = context.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        if (!sp.getBoolean(ENABLED_KEY, true)) return

        val langCode = sp.getString("cheerup.language", "en")
        val lang = AppLanguage.fromCode(langCode) ?: AppLanguage.ENGLISH
        val content = makeReminderContent(lang)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? SystemNotificationManager
        nm?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val REMINDER_IDENTIFIER = "cheerup.reminder.twoDays"
        const val TWO_DAYS_INTERVAL: Double = 2.0 * 24.0 * 60.0 * 60.0 // 172,800 seconds (48 hours)
        const val CHANNEL_ID = "cheerup_play_reminders"
        private const val NOTIFICATION_ID = 2002
        private const val ENABLED_KEY = "cheerup.notifications.enabled"
        private const val REQUESTED_KEY = "cheerup.notifications.requested"

        val reminderMessages: List<ReminderMessage> = listOf(
            ReminderMessage(
                en = "Ready for a quick round? Jump back in!",
                fr = "Prêt pour une partie rapide ? Reviens jouer !",
                es = "¿Listo para una partida rápida? ¡Vuelve a jugar!"
            ),
            ReminderMessage(
                en = "Time for a quick CheerUp! Can you beat your high score?",
                fr = "L'heure d'un petit CheerUp! Tu bats ton record ?",
                es = "¡Hora de un CheerUp! ¿Superas tu récord?"
            ),
            ReminderMessage(
                en = "MiniCheerUp! misses you — jump back into the arcade!",
                fr = "MiniCheerUp! t'attend — reviens dans l'arcade !",
                es = "¡MiniCheerUp! te espera — ¡vuelve al arcade!"
            ),
            ReminderMessage(
                en = "Got 2 minutes? Tap, bounce, and rush back in!",
                fr = "2 minutes de libre ? Viens faire une partie !",
                es = "¿Tienes 2 minutos? ¡Vuelve a jugar un rato!"
            )
        )

        val shared: NotificationManager by lazy { NotificationManager() }
    }
}

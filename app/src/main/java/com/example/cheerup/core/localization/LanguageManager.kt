package com.example.cheerup.core.localization

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cheerup.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanguageManager private constructor() {

    private var prefs: SharedPreferences? = null

    private val _languageFlow = MutableStateFlow(AppLanguage.ENGLISH)
    val languageFlow: StateFlow<AppLanguage> = _languageFlow.asStateFlow()

    var currentLanguage: AppLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    var hasCompletedLanguageOnboarding: Boolean by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        hasCompletedLanguageOnboarding = sp.getBoolean(ONBOARDING_COMPLETED_KEY, false)
        val savedCode = sp.getString(LANGUAGE_KEY, null)
        val lang = AppLanguage.fromCode(savedCode) ?: AppLanguage.ENGLISH
        currentLanguage = lang
        _languageFlow.value = lang
    }

    fun updateLanguage(language: AppLanguage) {
        currentLanguage = language
        _languageFlow.value = language
        prefs?.edit()?.putString(LANGUAGE_KEY, language.code)?.apply()
    }

    fun selectInitialLanguage(language: AppLanguage) {
        currentLanguage = language
        _languageFlow.value = language
        hasCompletedLanguageOnboarding = true
        prefs?.edit()
            ?.putString(LANGUAGE_KEY, language.code)
            ?.putBoolean(ONBOARDING_COMPLETED_KEY, true)
            ?.apply()
    }

    val isFrench: Boolean
        get() = currentLanguage == AppLanguage.FRENCH

    val isSpanish: Boolean
        get() = currentLanguage == AppLanguage.SPANISH

    fun tr(en: String, fr: String, es: String? = null): String {
        return when (currentLanguage) {
            AppLanguage.FRENCH -> fr
            AppLanguage.SPANISH -> es ?: en
            AppLanguage.ENGLISH -> en
        }
    }

    companion object {
        const val PREFS_NAME = "cheerup_prefs"
        private const val LANGUAGE_KEY = "cheerup.language"
        private const val ONBOARDING_COMPLETED_KEY = "cheerup.language.onboarded"

        val shared: LanguageManager by lazy { LanguageManager() }
    }
}

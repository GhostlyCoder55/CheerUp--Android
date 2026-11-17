package com.example.cheerup.domain.model

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    FRENCH("fr"),
    SPANISH("es");

    val id: String get() = code

    val displayName: String
        get() = when (this) {
            ENGLISH -> "English"
            FRENCH -> "Français"
            SPANISH -> "Español"
        }

    companion object {
        fun fromCode(code: String?): AppLanguage? =
            entries.firstOrNull { it.code == code }
    }
}

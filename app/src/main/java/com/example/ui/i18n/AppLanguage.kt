package com.example.ui.i18n

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
) {
    SYSTEM("system", "System Default", "System Default", "🌐"),
    ENGLISH("en", "English", "English", "🇺🇸"),
    SPANISH("es", "Spanish", "Español", "🇪🇸"),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵"),
    GERMAN("de", "German", "Deutsch", "🇩🇪"),
    FRENCH("fr", "French", "Français", "🇫🇷"),
    CHINESE("zh", "Chinese (Simplified)", "简体中文", "🇨🇳"),
    PORTUGUESE("pt", "Portuguese", "Português", "🇧🇷"),
    RUSSIAN("ru", "Russian", "Русский", "🇷🇺"),
    KOREAN("ko", "Korean", "한국어", "🇰🇷");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

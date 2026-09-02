package com.inscopelabs.abx.binbox.ui.i18n

import java.util.Locale

object Translations {
    val English = AppStrings()
    val Spanish = SpanishTranslations
    val Japanese = JapaneseTranslations
    val German = GermanTranslations
    val French = FrenchTranslations
    val Chinese = ChineseTranslations
    val Portuguese = PortugueseTranslations
    val Russian = RussianTranslations
    val Korean = KoreanTranslations

    fun getStringsFor(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.ENGLISH -> English
            AppLanguage.SPANISH -> Spanish
            AppLanguage.JAPANESE -> Japanese
            AppLanguage.GERMAN -> German
            AppLanguage.FRENCH -> French
            AppLanguage.CHINESE -> Chinese
            AppLanguage.PORTUGUESE -> Portuguese
            AppLanguage.RUSSIAN -> Russian
            AppLanguage.KOREAN -> Korean
            AppLanguage.SYSTEM -> {
                val locale = Locale.getDefault().language
                when {
                    locale.startsWith("es") -> Spanish
                    locale.startsWith("ja") -> Japanese
                    locale.startsWith("de") -> German
                    locale.startsWith("fr") -> French
                    locale.startsWith("zh") -> Chinese
                    locale.startsWith("pt") -> Portuguese
                    locale.startsWith("ru") -> Russian
                    locale.startsWith("ko") -> Korean
                    else -> English
                }
            }
        }
    }
}

package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import android.app.Application
import android.content.Context
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.ui.i18n.AppLanguage
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.i18n.Translations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TerminalPreferencesManager(
    application: Application,
    coroutineScope: CoroutineScope
) {
    private val prefs = application.getSharedPreferences("binbox_prefs", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("pref_language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code)
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val strings: StateFlow<AppStrings> = _appLanguage.map { lang ->
        Translations.getStringsFor(lang)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, Translations.getStringsFor(_appLanguage.value))

    private val _currentTheme = MutableStateFlow(TerminalThemes.MonokaiPro)
    val currentTheme: StateFlow<TerminalThemePreset> = _currentTheme.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(13)
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _cursorStyle = MutableStateFlow(CursorStyle.BLOCK)
    val cursorStyle: StateFlow<CursorStyle> = _cursorStyle.asStateFlow()

    private val _fontFamilyName = MutableStateFlow("Monospace")
    val fontFamilyName: StateFlow<String> = _fontFamilyName.asStateFlow()

    private val _bellMode = MutableStateFlow("Vibrate")
    val bellMode: StateFlow<String> = _bellMode.asStateFlow()

    private val _bufferLineLimit = MutableStateFlow(2000)
    val bufferLineLimit: StateFlow<Int> = _bufferLineLimit.asStateFlow()

    private val _wordWrapEnabled = MutableStateFlow(true)
    val wordWrapEnabled: StateFlow<Boolean> = _wordWrapEnabled.asStateFlow()

    private val _hapticFeedbackEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        prefs.edit().putString("pref_language", lang.code).apply()
    }

    fun setTheme(theme: TerminalThemePreset) {
        _currentTheme.value = theme
    }

    fun setFontSize(sizeSp: Int) {
        _fontSizeSp.value = sizeSp.coerceIn(9, 24)
    }

    fun setFontFamily(name: String) {
        _fontFamilyName.value = name
    }

    fun setBellMode(mode: String) {
        _bellMode.value = mode
    }

    fun setBufferLineLimit(limit: Int) {
        _bufferLineLimit.value = limit
    }

    fun setWordWrapEnabled(enabled: Boolean) {
        _wordWrapEnabled.value = enabled
    }

    fun setCursorStyle(style: CursorStyle) {
        _cursorStyle.value = style
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        _hapticFeedbackEnabled.value = enabled
    }

    fun resetPreferences() {
        setLanguage(AppLanguage.SYSTEM)
        setTheme(TerminalThemes.MonokaiPro)
        setFontSize(13)
        setFontFamily("Monospace")
        setCursorStyle(CursorStyle.BLOCK)
        setBellMode("Vibrate")
        setBufferLineLimit(2000)
        setWordWrapEnabled(true)
        toggleHapticFeedback(true)
    }
}

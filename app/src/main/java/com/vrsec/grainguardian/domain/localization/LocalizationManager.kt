package com.vrsec.grainguardian.domain.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage {
    ENGLISH,
    TELUGU
}

object LocalizationManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage = _currentLanguage.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
    }

    fun isTeluguRaw(): Boolean = _currentLanguage.value == AppLanguage.TELUGU

    @Composable
    fun isTelugu(): Boolean {
        val lang by currentLanguage.collectAsState()
        return lang == AppLanguage.TELUGU
    }

    fun getString(en: String, te: String): String {
        return if (isTeluguRaw()) te else en
    }
}

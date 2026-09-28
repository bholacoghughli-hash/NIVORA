package com.nivora.browser.data.repository

import com.nivora.browser.data.local.AccentColor
import com.nivora.browser.data.local.AppThemeMode
import com.nivora.browser.data.local.BrowserSettings
import com.nivora.browser.data.local.NivoraPreferences
import com.nivora.browser.data.local.SearchProvider
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val preferences: NivoraPreferences) {

    val settings: Flow<BrowserSettings> = preferences.settingsFlow

    suspend fun setThemeMode(mode: AppThemeMode) = preferences.setThemeMode(mode)
    suspend fun setAccentColor(accent: AccentColor) = preferences.setAccentColor(accent)
    suspend fun setSearchProvider(provider: SearchProvider) = preferences.setSearchProvider(provider)
    suspend fun setDesktopModeDefault(enabled: Boolean) = preferences.setDesktopSiteDefault(enabled)
    suspend fun setDoNotTrack(enabled: Boolean) = preferences.setDoNotTrack(enabled)
    suspend fun setJavaScriptEnabled(enabled: Boolean) = preferences.setJavaScriptEnabled(enabled)
    suspend fun setCookiesEnabled(enabled: Boolean) = preferences.setCookiesEnabled(enabled)
    suspend fun setSafeSearch(enabled: Boolean) = preferences.setSafeSearch(enabled)
    suspend fun setTrackingProtection(enabled: Boolean) = preferences.setTrackingProtection(enabled)
    suspend fun setClearOnExit(enabled: Boolean) = preferences.setClearOnExit(enabled)
}

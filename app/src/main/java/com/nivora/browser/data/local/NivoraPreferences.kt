package com.nivora.browser.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nivora_preferences")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

enum class SearchProvider(val displayName: String, val searchUrl: String, val homeUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com"),
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s", "https://www.ecosia.org")
}

enum class AccentColor(val displayName: String, val primaryHex: Long) {
    CYAN("Nivora Cyan", 0xFF00C9FF),
    ELECTRIC_BLUE("Electric Blue", 0xFF0284C7),
    EMERALD("Privacy Emerald", 0xFF10B981),
    PURPLE("Cyber Purple", 0xFF8B5CF6),
    AMBER("Amber Gold", 0xFFF59E0B)
}

data class BrowserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.CYAN,
    val searchProvider: SearchProvider = SearchProvider.DUCKDUCKGO,
    val isDesktopSiteDefault: Boolean = false,
    val isDoNotTrackEnabled: Boolean = true,
    val isJavaScriptEnabled: Boolean = true,
    val isCookiesEnabled: Boolean = true,
    val isSafeSearchEnabled: Boolean = true,
    val isTrackingProtectionActive: Boolean = true,
    val clearOnExit: Boolean = false,
    val homepageUrl: String = "about:blank"
)

class NivoraPreferences(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_ACCENT = stringPreferencesKey("accent_color")
        private val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
        private val KEY_DESKTOP_MODE = booleanPreferencesKey("desktop_mode_default")
        private val KEY_DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
        private val KEY_JAVASCRIPT = booleanPreferencesKey("javascript_enabled")
        private val KEY_COOKIES = booleanPreferencesKey("cookies_enabled")
        private val KEY_SAFE_SEARCH = booleanPreferencesKey("safe_search")
        private val KEY_TRACKING_PROTECTION = booleanPreferencesKey("tracking_protection")
        private val KEY_CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        private val KEY_HOMEPAGE = stringPreferencesKey("homepage_url")
    }

    val settingsFlow: Flow<BrowserSettings> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[KEY_THEME] ?: AppThemeMode.SYSTEM.name
        val accentStr = prefs[KEY_ACCENT] ?: AccentColor.CYAN.name
        val searchStr = prefs[KEY_SEARCH_ENGINE] ?: SearchProvider.DUCKDUCKGO.name

        BrowserSettings(
            themeMode = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.SYSTEM),
            accentColor = runCatching { AccentColor.valueOf(accentStr) }.getOrDefault(AccentColor.CYAN),
            searchProvider = runCatching { SearchProvider.valueOf(searchStr) }.getOrDefault(SearchProvider.DUCKDUCKGO),
            isDesktopSiteDefault = prefs[KEY_DESKTOP_MODE] ?: false,
            isDoNotTrackEnabled = prefs[KEY_DO_NOT_TRACK] ?: true,
            isJavaScriptEnabled = prefs[KEY_JAVASCRIPT] ?: true,
            isCookiesEnabled = prefs[KEY_COOKIES] ?: true,
            isSafeSearchEnabled = prefs[KEY_SAFE_SEARCH] ?: true,
            isTrackingProtectionActive = prefs[KEY_TRACKING_PROTECTION] ?: true,
            clearOnExit = prefs[KEY_CLEAR_ON_EXIT] ?: false,
            homepageUrl = prefs[KEY_HOMEPAGE] ?: "about:blank"
        )
    }

    suspend fun setThemeMode(theme: AppThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setAccentColor(accent: AccentColor) {
        context.dataStore.edit { it[KEY_ACCENT] = accent.name }
    }

    suspend fun setSearchProvider(provider: SearchProvider) {
        context.dataStore.edit { it[KEY_SEARCH_ENGINE] = provider.name }
    }

    suspend fun setDesktopSiteDefault(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DESKTOP_MODE] = enabled }
    }

    suspend fun setDoNotTrack(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DO_NOT_TRACK] = enabled }
    }

    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_JAVASCRIPT] = enabled }
    }

    suspend fun setCookiesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_COOKIES] = enabled }
    }

    suspend fun setSafeSearch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SAFE_SEARCH] = enabled }
    }

    suspend fun setTrackingProtection(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TRACKING_PROTECTION] = enabled }
    }

    suspend fun setClearOnExit(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CLEAR_ON_EXIT] = enabled }
    }

    suspend fun setHomepageUrl(url: String) {
        context.dataStore.edit { it[KEY_HOMEPAGE] = url }
    }
}

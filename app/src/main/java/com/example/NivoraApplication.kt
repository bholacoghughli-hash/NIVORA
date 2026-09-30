package com.example

import android.app.Application
import com.nivora.browser.browser.tabs.TabManager
import com.nivora.browser.data.downloads.NivoraDownloadManager
import com.nivora.browser.data.local.NivoraPreferences
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.data.repository.SettingsRepository
import com.nivora.browser.database.NivoraDatabase
import com.nivora.browser.privacy.BiometricLockManager
import com.nivora.browser.privacy.CookieSiteDataManager
import com.nivora.browser.privacy.DefaultSafeBrowsingProvider
import com.nivora.browser.privacy.SafeBrowsingProvider
import com.nivora.browser.privacy.TrackerBlockerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NivoraApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    val database by lazy { NivoraDatabase.getDatabase(this) }
    val browserRepository by lazy { BrowserRepository(database) }
    val preferences by lazy { NivoraPreferences(this) }
    val settingsRepository by lazy { SettingsRepository(preferences) }
    val downloadManager by lazy { NivoraDownloadManager(this, browserRepository, applicationScope) }
    val trackerBlockerEngine by lazy { TrackerBlockerEngine() }
    val cookieSiteDataManager by lazy { CookieSiteDataManager(this, browserRepository) }
    val safeBrowsingProvider: SafeBrowsingProvider by lazy { DefaultSafeBrowsingProvider() }
    val biometricLockManager by lazy { BiometricLockManager(this) }
    val aiProvider by lazy { com.nivora.browser.ai.LocalHeuristicAIProvider() }

    // TabManager को context के साथ सुरक्षित तरीके से लोड करना
    lateinit var tabManager: TabManager

    override fun onCreate() {
        super.onCreate()
        try {
            tabManager = TabManager(this) { url, title ->
                applicationScope.launch {
                    browserRepository.recordVisit(url, title)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

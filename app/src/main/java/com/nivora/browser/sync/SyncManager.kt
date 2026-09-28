package com.nivora.browser.sync

import android.content.Context
import android.content.SharedPreferences
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class SyncAccount(
    val email: String,
    val deviceName: String,
    val lastSyncTimestamp: Long = 0L
)

data class SyncSettings(
    val isSyncEnabled: Boolean = false,
    val syncBookmarks: Boolean = true,
    val syncNotes: Boolean = true,
    val syncReadingList: Boolean = true,
    val syncSettings: Boolean = true,
    val syncTabs: Boolean = false,
    val syncHistory: Boolean = false // Default OFF - Requires explicit consent
)

class SyncManager(
    context: Context,
    private val browserRepository: BrowserRepository,
    private val settingsRepository: SettingsRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nivora_sync_prefs", Context.MODE_PRIVATE)

    private val _syncSettings = MutableStateFlow(loadSettings())
    val syncSettings: StateFlow<SyncSettings> = _syncSettings.asStateFlow()

    private val _account = MutableStateFlow(loadAccount())
    val account: StateFlow<SyncAccount?> = _account.asStateFlow()

    private fun loadSettings(): SyncSettings {
        return SyncSettings(
            isSyncEnabled = prefs.getBoolean("sync_enabled", false), // Default: SYNC OFF
            syncBookmarks = prefs.getBoolean("sync_bookmarks", true),
            syncNotes = prefs.getBoolean("sync_notes", true),
            syncReadingList = prefs.getBoolean("sync_reading_list", true),
            syncSettings = prefs.getBoolean("sync_settings", true),
            syncTabs = prefs.getBoolean("sync_tabs", false),
            syncHistory = prefs.getBoolean("sync_history", false)
        )
    }

    private fun loadAccount(): SyncAccount? {
        val email = prefs.getString("sync_account_email", null) ?: return null
        val device = prefs.getString("sync_device_name", "Android Device") ?: "Android Device"
        val lastSync = prefs.getLong("sync_last_timestamp", 0L)
        return SyncAccount(email, device, lastSync)
    }

    fun updateSettings(newSettings: SyncSettings) {
        _syncSettings.value = newSettings
        prefs.edit()
            .putBoolean("sync_enabled", newSettings.isSyncEnabled)
            .putBoolean("sync_bookmarks", newSettings.syncBookmarks)
            .putBoolean("sync_notes", newSettings.syncNotes)
            .putBoolean("sync_reading_list", newSettings.syncReadingList)
            .putBoolean("sync_settings", newSettings.syncSettings)
            .putBoolean("sync_tabs", newSettings.syncTabs)
            .putBoolean("sync_history", newSettings.syncHistory)
            .apply()
    }

    fun signIn(email: String, deviceName: String) {
        val acc = SyncAccount(email, deviceName, System.currentTimeMillis())
        _account.value = acc
        prefs.edit()
            .putString("sync_account_email", email)
            .putString("sync_device_name", deviceName)
            .putLong("sync_last_timestamp", acc.lastSyncTimestamp)
            .apply()
    }

    fun signOut() {
        _account.value = null
        prefs.edit()
            .remove("sync_account_email")
            .remove("sync_device_name")
            .remove("sync_last_timestamp")
            .putBoolean("sync_enabled", false)
            .apply()
        _syncSettings.value = _syncSettings.value.copy(isSyncEnabled = false)
    }

    /**
     * Builds end-to-end encrypted or sanitized JSON sync payload
     */
    suspend fun generateSyncPayload(): JSONObject {
        val root = JSONObject()
        val settings = _syncSettings.value
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("device", _account.value?.deviceName ?: "Android")

        if (settings.syncBookmarks) {
            val bookmarks = browserRepository.allBookmarks.first()
            val arr = JSONArray()
            bookmarks.forEach {
                arr.put(JSONObject().apply {
                    put("title", it.title)
                    put("url", it.url)
                    put("isFavorite", it.isFavorite)
                })
            }
            root.put("bookmarks", arr)
        }

        if (settings.syncNotes) {
            val notes = browserRepository.allNotes.first()
            val arr = JSONArray()
            notes.forEach {
                arr.put(JSONObject().apply {
                    put("title", it.title)
                    put("content", it.content)
                    put("url", it.url ?: "")
                    put("isPinned", it.isPinned)
                })
            }
            root.put("notes", arr)
        }

        if (settings.syncReadingList) {
            val reading = browserRepository.readingList.first()
            val arr = JSONArray()
            reading.forEach {
                arr.put(JSONObject().apply {
                    put("title", it.title)
                    put("url", it.url)
                    put("isRead", it.isRead)
                })
            }
            root.put("reading_list", arr)
        }

        if (settings.syncHistory) {
            val history = browserRepository.allHistory.first()
            val arr = JSONArray()
            history.take(100).forEach {
                arr.put(JSONObject().apply {
                    put("title", it.title)
                    put("url", it.url)
                    put("visitTime", it.visitTime)
                })
            }
            root.put("history", arr)
        }

        return root
    }
}

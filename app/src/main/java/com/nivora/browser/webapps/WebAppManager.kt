package com.nivora.browser.webapps

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class WebAppItem(
    val id: String,
    val title: String,
    val startUrl: String,
    val iconUrl: String? = null,
    val isStandalone: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

class WebAppManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nivora_webapps", Context.MODE_PRIVATE)

    private val _webApps = MutableStateFlow<List<WebAppItem>>(emptyList())
    val webApps: StateFlow<List<WebAppItem>> = _webApps.asStateFlow()

    init {
        loadWebApps()
    }

    private fun loadWebApps() {
        val json = prefs.getString("installed_webapps", null) ?: return
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<WebAppItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WebAppItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        startUrl = obj.getString("startUrl"),
                        iconUrl = obj.optString("iconUrl", null),
                        isStandalone = obj.optBoolean("isStandalone", true),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
            _webApps.value = list
        } catch (_: Exception) {}
    }

    private fun saveWebApps(list: List<WebAppItem>) {
        _webApps.value = list
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("startUrl", item.startUrl)
                put("iconUrl", item.iconUrl ?: "")
                put("isStandalone", item.isStandalone)
                put("addedAt", item.addedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString("installed_webapps", arr.toString()).apply()
    }

    fun addWebApp(title: String, url: String, iconUrl: String? = null): WebAppItem {
        val cleanTitle = if (title.isBlank()) url else title
        val newItem = WebAppItem(
            id = java.util.UUID.randomUUID().toString(),
            title = cleanTitle,
            startUrl = url,
            iconUrl = iconUrl
        )
        val current = _webApps.value.toMutableList()
        current.removeAll { it.startUrl == url }
        current.add(0, newItem)
        saveWebApps(current)
        return newItem
    }

    fun removeWebApp(id: String) {
        val current = _webApps.value.filterNot { it.id == id }
        saveWebApps(current)
    }

    fun hasWebApp(url: String): Boolean {
        return _webApps.value.any { it.startUrl == url }
    }
}

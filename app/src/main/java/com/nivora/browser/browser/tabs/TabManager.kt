package com.nivora.browser.browser.tabs

import android.content.Context
import com.nivora.browser.browser.engine.BrowserEngine
import com.nivora.browser.browser.webview.AndroidWebViewEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#0284C7"
)

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Tab",
    var url: String = "",
    val engine: BrowserEngine,
    val isIncognito: Boolean = false,
    val groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ClosedTabRecord(
    val url: String,
    val title: String,
    val isIncognito: Boolean,
    val groupId: String?
)

class TabManager(
    private val context: Context,
    private val onPageLoaded: (url: String, title: String) -> Unit
) {
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _tabGroups = MutableStateFlow<List<TabGroup>>(
        listOf(
            TabGroup(id = "group_personal", name = "Personal", colorHex = "#0284C7"),
            TabGroup(id = "group_study", name = "Study", colorHex = "#10B981"),
            TabGroup(id = "group_research", name = "Research", colorHex = "#8B5CF6"),
            TabGroup(id = "group_work", name = "Work", colorHex = "#F59E0B")
        )
    )
    val tabGroups: StateFlow<List<TabGroup>> = _tabGroups.asStateFlow()

    private val _selectedGroupFilter = MutableStateFlow<String?>(null)
    val selectedGroupFilter: StateFlow<String?> = _selectedGroupFilter.asStateFlow()

    // Recently closed tabs stack (LIFO)
    private val closedTabsStack = ArrayDeque<ClosedTabRecord>()
    private val _canReopenClosedTab = MutableStateFlow(false)
    val canReopenClosedTab: StateFlow<Boolean> = _canReopenClosedTab.asStateFlow()

    init {
    try {
        createTab(url = "", isIncognito = false)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    }

    fun createTab(
        url: String = "",
        isIncognito: Boolean = false,
        groupId: String? = null
    ): BrowserTab {
        val tabId = UUID.randomUUID().toString()
        val app = context.applicationContext as? com.example.NivoraApplication
        val engine = AndroidWebViewEngine(context, isIncognito = isIncognito) { loadedUrl, title ->
            _tabs.update { currentTabs ->
                currentTabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(url = loadedUrl, title = title)
                    } else tab
                }
            }
            if (!isIncognito) {
                onPageLoaded(loadedUrl, title)
            }
        }.apply {
            trackerBlockerEngine = app?.trackerBlockerEngine
            safeBrowsingProvider = app?.safeBrowsingProvider
        }

        val newTab = BrowserTab(
            id = tabId,
            title = if (url.isBlank()) "New Tab" else url,
            url = url,
            engine = engine,
            isIncognito = isIncognito,
            groupId = groupId ?: _selectedGroupFilter.value
        )

        if (url.isNotBlank()) {
            engine.loadUrl(url)
        }

        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        return newTab
    }

    fun duplicateTab(id: String): BrowserTab? {
        val original = _tabs.value.find { it.id == id } ?: return null
        return createTab(
            url = original.url,
            isIncognito = original.isIncognito,
            groupId = original.groupId
        )
    }

    fun selectTab(id: String) {
        if (_tabs.value.any { it.id == id }) {
            _activeTabId.value = id
        }
    }

    fun closeTab(id: String) {
        val currentList = _tabs.value
        val tabToClose = currentList.find { it.id == id } ?: return

        // Push to recently closed stack if non-blank
        if (tabToClose.url.isNotBlank() && !tabToClose.url.startsWith("about:")) {
            closedTabsStack.addLast(
                ClosedTabRecord(
                    url = tabToClose.url,
                    title = tabToClose.title,
                    isIncognito = tabToClose.isIncognito,
                    groupId = tabToClose.groupId
                )
            )
            _canReopenClosedTab.value = true
        }

        tabToClose.engine.destroy()

        val updated = currentList.filterNot { it.id == id }
        _tabs.value = updated

        if (_activeTabId.value == id) {
            _activeTabId.value = updated.lastOrNull()?.id
        }

        if (updated.isEmpty()) {
            createTab()
        }
    }

    fun reopenClosedTab(): BrowserTab? {
        if (closedTabsStack.isEmpty()) return null
        val record = closedTabsStack.removeLast()
        _canReopenClosedTab.value = closedTabsStack.isNotEmpty()
        return createTab(
            url = record.url,
            isIncognito = record.isIncognito,
            groupId = record.groupId
        )
    }

    fun closeAllTabs() {
        val currentList = _tabs.value
        currentList.forEach { tab ->
            if (tab.url.isNotBlank() && !tab.url.startsWith("about:")) {
                closedTabsStack.addLast(
                    ClosedTabRecord(
                        url = tab.url,
                        title = tab.title,
                        isIncognito = tab.isIncognito,
                        groupId = tab.groupId
                    )
                )
            }
            tab.engine.destroy()
        }
        _canReopenClosedTab.value = closedTabsStack.isNotEmpty()
        _tabs.value = emptyList()
        createTab()
    }

    fun getActiveTab(): BrowserTab? {
        val currentId = _activeTabId.value ?: return _tabs.value.firstOrNull()
        return _tabs.value.find { it.id == currentId } ?: _tabs.value.firstOrNull()
    }

    // Tab Groups
    fun setGroupFilter(groupId: String?) {
        _selectedGroupFilter.value = groupId
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(groupId = groupId) else it }
        }
    }

    fun createGroup(name: String, colorHex: String = "#0284C7"): TabGroup {
        val newGroup = TabGroup(name = name, colorHex = colorHex)
        _tabGroups.update { it + newGroup }
        return newGroup
    }

    fun renameGroup(groupId: String, newName: String) {
        _tabGroups.update { list ->
            list.map { if (it.id == groupId) it.copy(name = newName) else it }
        }
    }

    fun deleteGroup(groupId: String) {
        _tabGroups.update { list -> list.filterNot { it.id == groupId } }
        // Ungroup tabs that were part of this group
        _tabs.update { list ->
            list.map { if (it.groupId == groupId) it.copy(groupId = null) else it }
        }
        if (_selectedGroupFilter.value == groupId) {
            _selectedGroupFilter.value = null
        }
    }
}

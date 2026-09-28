package com.nivora.browser.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nivora.browser.browser.engine.EngineState
import com.nivora.browser.browser.tabs.BrowserTab
import com.nivora.browser.browser.tabs.TabGroup
import com.nivora.browser.browser.tabs.TabManager
import com.nivora.browser.data.local.AccentColor
import com.nivora.browser.data.local.AppThemeMode
import com.nivora.browser.data.local.BrowserSettings
import com.nivora.browser.data.local.SearchProvider
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.data.repository.SettingsRepository
import com.nivora.browser.database.entities.Bookmark
import com.nivora.browser.database.entities.BookmarkFolder
import com.nivora.browser.database.entities.DownloadItem
import com.nivora.browser.database.entities.HistoryItem
import com.nivora.browser.database.entities.ReadingListItem
import com.nivora.browser.search.SearchResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class HistoryTimeRange(val label: String, val durationMillis: Long) {
    LAST_HOUR("Last hour", TimeUnit.HOURS.toMillis(1)),
    LAST_24_HOURS("Last 24 hours", TimeUnit.DAYS.toMillis(1)),
    LAST_7_DAYS("Last 7 days", TimeUnit.DAYS.toMillis(7)),
    LAST_4_WEEKS("Last 4 weeks", TimeUnit.DAYS.toMillis(28)),
    ALL_TIME("All time", -1L)
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(
    val tabManager: TabManager,
    private val browserRepository: BrowserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tabs: StateFlow<List<BrowserTab>> = tabManager.tabs
    val activeTabId: StateFlow<String?> = tabManager.activeTabId
    val canReopenClosedTab: StateFlow<Boolean> = tabManager.canReopenClosedTab
    val tabGroups: StateFlow<List<TabGroup>> = tabManager.tabGroups
    val selectedGroupFilter: StateFlow<String?> = tabManager.selectedGroupFilter

    val settings: StateFlow<BrowserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserSettings())

    val bookmarks: StateFlow<List<Bookmark>> = browserRepository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteBookmarks: StateFlow<List<Bookmark>> = browserRepository.favoriteBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkFolders: StateFlow<List<BookmarkFolder>> = browserRepository.bookmarkFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = browserRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<HistoryItem>> = browserRepository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = browserRepository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingList: StateFlow<List<ReadingListItem>> = browserRepository.readingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab Engine State Flow
    val activeEngineState: StateFlow<EngineState> = activeTabId.flatMapLatest { id ->
        val tab = tabs.value.find { it.id == id } ?: tabs.value.firstOrNull()
        tab?.engine?.state ?: flowOf(EngineState())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EngineState())

    // Desktop mode toggle for current tab
    private val _isDesktopActive = MutableStateFlow(false)
    val isDesktopActive: StateFlow<Boolean> = _isDesktopActive.asStateFlow()

    // Find in page state
    private val _findInPageQuery = MutableStateFlow("")
    val findInPageQuery: StateFlow<String> = _findInPageQuery.asStateFlow()

    private val _isFindInPageVisible = MutableStateFlow(false)
    val isFindInPageVisible: StateFlow<Boolean> = _isFindInPageVisible.asStateFlow()

    fun getActiveTab(): BrowserTab? = tabManager.getActiveTab()

    fun loadUrlOrSearch(input: String) {
        val active = getActiveTab() ?: return
        val currentProvider = settings.value.searchProvider
        val resolvedUrl = SearchResolver.resolveInput(input, currentProvider)
        viewModelScope.launch {
            if (!SearchResolver.isUrl(input.trim()) && !active.isIncognito) {
                browserRepository.recordSearch(input.trim())
            }
            // Check if site has per-site desktop mode stored
            val origin = SearchResolver.extractDomain(resolvedUrl)
            if (origin.isNotBlank()) {
                val siteDesktop = browserRepository.isDesktopForOrigin(origin)
                if (siteDesktop || settings.value.isDesktopSiteDefault) {
                    _isDesktopActive.value = true
                    active.engine.setDesktopMode(true)
                }
            }
        }
        active.url = resolvedUrl
        active.engine.loadUrl(resolvedUrl)
    }

    fun retryCurrentPage() {
        val active = getActiveTab() ?: return
        val url = activeEngineState.value.failingUrl ?: active.url
        if (url.isNotBlank()) {
            active.engine.loadUrl(url)
        } else {
            active.engine.reload()
        }
    }

    fun handleBackPress(): Boolean {
        if (_isFindInPageVisible.value) {
            closeFindInPage()
            return true
        }
        val active = getActiveTab()
        return if (active != null && active.engine.canGoBack()) {
            active.engine.goBack()
        } else {
            false
        }
    }

    fun goForward() {
        getActiveTab()?.engine?.goForward()
    }

    fun reload() {
        getActiveTab()?.engine?.reload()
    }

    fun stop() {
        getActiveTab()?.engine?.stopLoading()
    }

    // Tabs Operations
    fun openNewTab(url: String = "", isIncognito: Boolean = false, groupId: String? = null): BrowserTab {
        return tabManager.createTab(url = url, isIncognito = isIncognito, groupId = groupId)
    }

    fun duplicateTab(id: String) {
        tabManager.duplicateTab(id)
    }

    fun selectTab(id: String) {
        tabManager.selectTab(id)
    }

    fun closeTab(id: String) {
        tabManager.closeTab(id)
    }

    fun reopenClosedTab() {
        tabManager.reopenClosedTab()
    }

    fun closeAllTabs() {
        tabManager.closeAllTabs()
    }

    fun setGroupFilter(groupId: String?) {
        tabManager.setGroupFilter(groupId)
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        tabManager.assignTabToGroup(tabId, groupId)
    }

    fun createGroup(name: String, colorHex: String = "#0284C7") {
        tabManager.createGroup(name, colorHex)
    }

    fun renameGroup(groupId: String, newName: String) {
        tabManager.renameGroup(groupId, newName)
    }

    fun deleteGroup(groupId: String) {
        tabManager.deleteGroup(groupId)
    }

    // Bookmarks Operations
    fun bookmarkCurrentPage(folderId: Long? = null, isFavorite: Boolean = false) {
        val state = activeEngineState.value
        val url = state.url
        val title = if (state.title.isNotBlank()) state.title else url
        if (url.isNotBlank() && !url.startsWith("about:")) {
            viewModelScope.launch {
                browserRepository.addBookmark(
                    title = title,
                    url = url,
                    folderId = folderId,
                    isFavorite = isFavorite
                )
            }
        }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            browserRepository.updateBookmark(bookmark)
        }
    }

    fun toggleFavorite(bookmark: Bookmark) {
        viewModelScope.launch {
            browserRepository.toggleFavorite(bookmark)
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            browserRepository.removeBookmarkById(id)
        }
    }

    fun createBookmarkFolder(name: String) {
        viewModelScope.launch {
            browserRepository.createFolder(name)
        }
    }

    fun deleteBookmarkFolder(id: Long) {
        viewModelScope.launch {
            browserRepository.deleteFolderById(id)
        }
    }

    // Reading List Operations
    fun addToReadingList(customTitle: String? = null, customUrl: String? = null) {
        val state = activeEngineState.value
        val url = customUrl ?: state.url
        val title = customTitle ?: state.title
        if (url.isNotBlank() && !url.startsWith("about:")) {
            viewModelScope.launch {
                browserRepository.addToReadingList(title, url)
            }
        }
    }

    fun toggleReadingItemStatus(id: Long, currentIsRead: Boolean) {
        viewModelScope.launch {
            browserRepository.setReadingItemStatus(id, !currentIsRead)
        }
    }

    fun deleteReadingItem(id: Long) {
        viewModelScope.launch {
            browserRepository.deleteReadingItem(id)
        }
    }

    // Desktop Mode & Per-Site Mode
    fun toggleDesktopMode(rememberForSite: Boolean = false) {
        val current = _isDesktopActive.value
        val newState = !current
        _isDesktopActive.value = newState
        getActiveTab()?.engine?.setDesktopMode(newState)

        if (rememberForSite) {
            val url = activeEngineState.value.url
            val origin = SearchResolver.extractDomain(url)
            if (origin.isNotBlank()) {
                viewModelScope.launch {
                    browserRepository.setDesktopForOrigin(origin, newState)
                }
            }
        }
    }

    // Find In Page
    fun openFindInPage() {
        _isFindInPageVisible.value = true
    }

    fun closeFindInPage() {
        _isFindInPageVisible.value = false
        _findInPageQuery.value = ""
        getActiveTab()?.engine?.clearMatches()
    }

    fun setFindQuery(query: String) {
        _findInPageQuery.value = query
        getActiveTab()?.engine?.findInPage(query)
    }

    fun findNext(forward: Boolean) {
        getActiveTab()?.engine?.findNext(forward)
    }

    // History Operations
    fun clearHistory() {
        viewModelScope.launch {
            browserRepository.clearHistory()
        }
    }

    fun deleteHistoryRange(range: HistoryTimeRange) {
        viewModelScope.launch {
            if (range == HistoryTimeRange.ALL_TIME) {
                browserRepository.clearHistory()
            } else {
                val cutoff = System.currentTimeMillis() - range.durationMillis
                browserRepository.deleteHistorySince(cutoff)
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            browserRepository.deleteHistoryById(id)
        }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            browserRepository.clearDownloads()
        }
    }

    // Settings
    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccentColor(accent: AccentColor) {
        viewModelScope.launch { settingsRepository.setAccentColor(accent) }
    }

    fun setSearchProvider(provider: SearchProvider) {
        viewModelScope.launch { settingsRepository.setSearchProvider(provider) }
    }

    fun setDesktopDefault(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDesktopModeDefault(enabled) }
    }

    fun setDoNotTrack(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDoNotTrack(enabled) }
    }

    fun setJavaScript(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setJavaScriptEnabled(enabled) }
    }

    fun setCookies(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCookiesEnabled(enabled) }
    }

    fun setSafeSearch(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSafeSearch(enabled) }
    }

    fun setTrackingProtection(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrackingProtection(enabled) }
    }

    fun setClearOnExit(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setClearOnExit(enabled) }
    }
}

class BrowserViewModelFactory(
    private val tabManager: TabManager,
    private val browserRepository: BrowserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            return BrowserViewModel(tabManager, browserRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

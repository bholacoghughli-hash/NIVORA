package com.nivora.browser.data.repository

import com.nivora.browser.database.NivoraDatabase
import com.nivora.browser.database.entities.Bookmark
import com.nivora.browser.database.entities.BookmarkFolder
import com.nivora.browser.database.entities.DownloadItem
import com.nivora.browser.database.entities.HistoryItem
import com.nivora.browser.database.entities.ReadingListItem
import com.nivora.browser.database.entities.SearchHistoryEntity
import com.nivora.browser.database.entities.SitePermission
import com.nivora.browser.database.entities.TabEntity
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: NivoraDatabase) {

    private val historyDao = database.historyDao()
    private val bookmarkDao = database.bookmarkDao()
    private val downloadDao = database.downloadDao()
    private val readingListDao = database.readingListDao()
    private val tabDao = database.tabDao()
    private val searchHistoryDao = database.searchHistoryDao()
    private val sitePermissionsDao = database.sitePermissionsDao()

    // History
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()
    val recentHistory: Flow<List<HistoryItem>> = historyDao.getRecentHistory(10)

    fun searchHistory(query: String): Flow<List<HistoryItem>> = historyDao.searchHistory(query)

    suspend fun recordVisit(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return
        val existing = historyDao.findByUrl(url)
        val cleanTitle = if (title.isBlank()) url else title
        if (existing != null) {
            historyDao.updateVisit(existing.id, cleanTitle, System.currentTimeMillis())
        } else {
            historyDao.insertHistory(
                HistoryItem(
                    url = url,
                    title = cleanTitle,
                    visitTime = System.currentTimeMillis(),
                    visitCount = 1
                )
            )
        }
    }

    suspend fun deleteHistoryById(id: Long) = historyDao.deleteHistoryById(id)
    suspend fun deleteHistorySince(startTime: Long) = historyDao.deleteHistorySince(startTime)
    suspend fun clearHistory() = historyDao.clearHistory()

    // Bookmarks
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val favoriteBookmarks: Flow<List<Bookmark>> = bookmarkDao.getFavoriteBookmarks()
    val bookmarkFolders: Flow<List<BookmarkFolder>> = bookmarkDao.getAllFolders()

    fun searchBookmarks(query: String): Flow<List<Bookmark>> = bookmarkDao.searchBookmarks(query)
    fun getBookmarksInFolder(folderId: Long): Flow<List<Bookmark>> = bookmarkDao.getBookmarksInFolder(folderId)

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.getBookmarkByUrl(url) != null
    }

    suspend fun addBookmark(
        title: String,
        url: String,
        faviconUrl: String? = null,
        folderId: Long? = null,
        isFavorite: Boolean = false
    ): Long {
        val cleanTitle = if (title.isBlank()) url else title
        return bookmarkDao.insertBookmark(
            Bookmark(
                title = cleanTitle,
                url = url,
                faviconUrl = faviconUrl,
                folderId = folderId,
                isFavorite = isFavorite
            )
        )
    }

    suspend fun updateBookmark(bookmark: Bookmark) = bookmarkDao.updateBookmark(bookmark)

    suspend fun toggleFavorite(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark.copy(isFavorite = !bookmark.isFavorite))
    }

    suspend fun removeBookmarkById(id: Long) = bookmarkDao.deleteBookmarkById(id)

    suspend fun createFolder(name: String, parentId: Long? = null): Long {
        return bookmarkDao.insertFolder(BookmarkFolder(name = name, parentId = parentId))
    }

    suspend fun deleteFolderById(id: Long) = bookmarkDao.deleteFolderById(id)

    // Downloads
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    suspend fun recordDownload(fileName: String, url: String, filePath: String, totalBytes: Long) {
        downloadDao.insertDownload(
            DownloadItem(
                fileName = fileName,
                url = url,
                filePath = filePath,
                totalBytes = totalBytes,
                downloadedBytes = totalBytes,
                status = "COMPLETED"
            )
        )
    }
    suspend fun deleteDownloadById(id: Long) = downloadDao.deleteDownloadById(id)
    suspend fun clearDownloads() = downloadDao.clearDownloads()

    // Reading List
    val readingList: Flow<List<ReadingListItem>> = readingListDao.getAllReadingList()
    fun searchReadingList(query: String): Flow<List<ReadingListItem>> = readingListDao.searchReadingList(query)
    suspend fun addToReadingList(title: String, url: String) {
        readingListDao.insertReadingItem(
            ReadingListItem(title = if (title.isBlank()) url else title, url = url)
        )
    }
    suspend fun setReadingItemStatus(id: Long, isRead: Boolean) = readingListDao.setReadStatus(id, isRead)
    suspend fun deleteReadingItem(id: Long) = readingListDao.deleteReadingItem(id)

    // Tabs
    val allTabs: Flow<List<TabEntity>> = tabDao.getAllTabs()
    suspend fun saveTab(tab: TabEntity) = tabDao.insertTab(tab)
    suspend fun removeTab(tabId: String) = tabDao.deleteTabById(tabId)
    suspend fun clearTabs() = tabDao.clearAllTabs()

    // Search History
    val recentSearches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()
    suspend fun recordSearch(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }
    suspend fun clearSearchHistory() = searchHistoryDao.clearSearchHistory()

    // Notes
    private val notesDao = database.notesDao()
    val allNotes: Flow<List<com.nivora.browser.database.entities.Note>> = notesDao.getAllNotes()
    fun searchNotes(query: String): Flow<List<com.nivora.browser.database.entities.Note>> = notesDao.searchNotes(query)

    suspend fun addNote(title: String, content: String, url: String? = null, isPinned: Boolean = false): Long {
        return notesDao.insertNote(
            com.nivora.browser.database.entities.Note(
                title = title.ifBlank { "Untitled Note" },
                content = content,
                url = url,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateNote(note: com.nivora.browser.database.entities.Note) {
        notesDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePinNote(note: com.nivora.browser.database.entities.Note) {
        notesDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNoteById(id: Long) = notesDao.deleteNoteById(id)

    // Site Permissions & Per-Site Desktop Mode
    suspend fun isDesktopForOrigin(origin: String): Boolean {
        val perm = sitePermissionsDao.getPermissionForOrigin(origin)
        return perm?.isDesktopMode ?: false
    }

    suspend fun setDesktopForOrigin(origin: String, isDesktop: Boolean) {
        val existing = sitePermissionsDao.getPermissionForOrigin(origin)
        if (existing != null) {
            sitePermissionsDao.setPermission(existing.copy(isDesktopMode = isDesktop))
        } else {
            sitePermissionsDao.setPermission(SitePermission(origin = origin, isDesktopMode = isDesktop))
        }
    }
}

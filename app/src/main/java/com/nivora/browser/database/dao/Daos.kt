package com.nivora.browser.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nivora.browser.database.entities.Bookmark
import com.nivora.browser.database.entities.BookmarkFolder
import com.nivora.browser.database.entities.CustomSearchEngine
import com.nivora.browser.database.entities.DownloadItem
import com.nivora.browser.database.entities.HistoryItem
import com.nivora.browser.database.entities.Note
import com.nivora.browser.database.entities.ReadingListItem
import com.nivora.browser.database.entities.SearchHistoryEntity
import com.nivora.browser.database.entities.SitePermission
import com.nivora.browser.database.entities.TabEntity
import com.nivora.browser.database.entities.TabGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getBookmarksInFolder(folderId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchBookmarks(query: String): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<BookmarkFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolder): Long

    @Delete
    suspend fun deleteFolder(folder: BookmarkFolder)

    @Query("DELETE FROM bookmark_folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitTime DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history ORDER BY visitTime DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitTime DESC")
    fun searchHistory(query: String): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryItem?

    @Query("UPDATE history SET visitTime = :time, visitCount = visitCount + 1, title = :title WHERE id = :id")
    suspend fun updateVisit(id: Long, title: String, time: Long)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history WHERE visitTime >= :startTime")
    suspend fun deleteHistorySince(startTime: Long)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem): Long

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearDownloads()
}

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY addedAt DESC")
    fun getAllReadingList(): Flow<List<ReadingListItem>>

    @Query("SELECT * FROM reading_list WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY addedAt DESC")
    fun searchReadingList(query: String): Flow<List<ReadingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingItem(item: ReadingListItem): Long

    @Query("UPDATE reading_list SET isRead = :isRead WHERE id = :id")
    suspend fun setReadStatus(id: Long, isRead: Boolean)

    @Query("DELETE FROM reading_list WHERE id = :id")
    suspend fun deleteReadingItem(id: Long)
}

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY position ASC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()

    @Query("SELECT * FROM tab_groups ORDER BY createdAt ASC")
    fun getAllGroups(): Flow<List<TabGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: TabGroupEntity)

    @Update
    suspend fun updateGroup(group: TabGroupEntity)

    @Query("DELETE FROM tab_groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 15")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(item: SearchHistoryEntity): Long

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Dao
interface NotesDao {
    @Query("SELECT * FROM browser_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM browser_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM browser_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface SitePermissionsDao {
    @Query("SELECT * FROM site_permissions WHERE origin = :origin")
    suspend fun getPermissionForOrigin(origin: String): SitePermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPermission(permission: SitePermission)
}

@Dao
interface CustomSearchEnginesDao {
    @Query("SELECT * FROM custom_search_engines")
    fun getAllEngines(): Flow<List<CustomSearchEngine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngine(engine: CustomSearchEngine): Long
}

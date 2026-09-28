package com.nivora.browser.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val folderId: Long? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmark_folders")
data class BookmarkFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val visitTime: Long = System.currentTimeMillis(),
    val visitCount: Int = 1
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val url: String,
    val filePath: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = "COMPLETED", // COMPLETED, DOWNLOADING, FAILED, PAUSED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_list")
data class ReadingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val contentSnippet: String? = null,
    val isRead: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String? = null,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val isIncognito: Boolean = false,
    val position: Int = 0,
    val groupId: String? = null,
    val lastActiveTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "tab_groups")
data class TabGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "site_permissions")
data class SitePermission(
    @PrimaryKey
    val origin: String,
    val allowCookies: Boolean = true,
    val allowJs: Boolean = true,
    val allowPopups: Boolean = false,
    val allowLocation: Boolean = false,
    val isDesktopMode: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_search_engines")
data class CustomSearchEngine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val searchUrl: String,
    val iconUrl: String? = null,
    val isDefault: Boolean = false
)

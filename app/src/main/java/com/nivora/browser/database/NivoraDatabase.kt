package com.nivora.browser.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nivora.browser.database.dao.BookmarkDao
import com.nivora.browser.database.dao.CustomSearchEnginesDao
import com.nivora.browser.database.dao.DownloadDao
import com.nivora.browser.database.dao.HistoryDao
import com.nivora.browser.database.dao.NotesDao
import com.nivora.browser.database.dao.ReadingListDao
import com.nivora.browser.database.dao.SearchHistoryDao
import com.nivora.browser.database.dao.SitePermissionsDao
import com.nivora.browser.database.dao.TabDao
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

@Database(
    entities = [
        Bookmark::class,
        BookmarkFolder::class,
        HistoryItem::class,
        DownloadItem::class,
        ReadingListItem::class,
        Note::class,
        TabEntity::class,
        TabGroupEntity::class,
        SearchHistoryEntity::class,
        SitePermission::class,
        CustomSearchEngine::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NivoraDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun tabDao(): TabDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun notesDao(): NotesDao
    abstract fun sitePermissionsDao(): SitePermissionsDao
    abstract fun customSearchEnginesDao(): CustomSearchEnginesDao

    companion object {
        @Volatile
        private var INSTANCE: NivoraDatabase? = null

        fun getDatabase(context: Context): NivoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NivoraDatabase::class.java,
                    "nivora_browser.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

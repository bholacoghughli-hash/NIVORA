package com.nivora.browser.data.backup

import android.content.Context
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object DataTransferManager {

    /**
     * Export all data to NIVORA JSON format
     */
    suspend fun exportToJson(
        context: Context,
        browserRepository: BrowserRepository,
        includeHistory: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "NIVORA")
        root.put("exportDate", System.currentTimeMillis())

        // Bookmarks
        val bookmarks = browserRepository.allBookmarks.first()
        val bmArr = JSONArray()
        bookmarks.forEach {
            bmArr.put(JSONObject().apply {
                put("title", it.title)
                put("url", it.url)
                put("isFavorite", it.isFavorite)
            })
        }
        root.put("bookmarks", bmArr)

        // Notes
        val notes = browserRepository.allNotes.first()
        val notesArr = JSONArray()
        notes.forEach {
            notesArr.put(JSONObject().apply {
                put("title", it.title)
                put("content", it.content)
                put("url", it.url ?: "")
                put("isPinned", it.isPinned)
            })
        }
        root.put("notes", notesArr)

        // Reading list
        val reading = browserRepository.readingList.first()
        val rlArr = JSONArray()
        reading.forEach {
            rlArr.put(JSONObject().apply {
                put("title", it.title)
                put("url", it.url)
                put("isRead", it.isRead)
            })
        }
        root.put("readingList", rlArr)

        if (includeHistory) {
            val history = browserRepository.allHistory.first()
            val histArr = JSONArray()
            history.forEach {
                histArr.put(JSONObject().apply {
                    put("title", it.title)
                    put("url", it.url)
                    put("visitTime", it.visitTime)
                })
            }
            root.put("history", histArr)
        }

        val file = File(context.cacheDir, "nivora_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        file
    }

    /**
     * Export Bookmarks to Standard Netscape HTML Bookmark format
     */
    suspend fun exportBookmarksToHtml(
        context: Context,
        browserRepository: BrowserRepository
    ): File = withContext(Dispatchers.IO) {
        val bookmarks = browserRepository.allBookmarks.first()
        val sb = StringBuilder()
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<!-- This is an automatically generated file. It will be read and overwritten. Do Not Edit! -->\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>Bookmarks</TITLE>\n")
        sb.append("<H1>Bookmarks</H1>\n")
        sb.append("<DL><p>\n")

        bookmarks.forEach { bm ->
            // Sanitize against HTML injection
            val cleanTitle = bm.title.replace("<", "&lt;").replace(">", "&gt;")
            val cleanUrl = bm.url.replace("\"", "%22")
            sb.append("    <DT><A HREF=\"$cleanUrl\" ADD_DATE=\"${bm.createdAt / 1000}\">$cleanTitle</A>\n")
        }

        sb.append("</DL><p>\n")

        val file = File(context.cacheDir, "nivora_bookmarks_${System.currentTimeMillis()}.html")
        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        file
    }

    /**
     * Import and validate JSON backup data (strictly sanitizing inputs, no script execution)
     */
    suspend fun importJsonData(
        jsonString: String,
        browserRepository: BrowserRepository
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val root = JSONObject(jsonString)

            val bmArr = root.optJSONArray("bookmarks")
            if (bmArr != null) {
                for (i in 0 until bmArr.length()) {
                    val obj = bmArr.getJSONObject(i)
                    val url = obj.optString("url")
                    val title = obj.optString("title")
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        browserRepository.addBookmark(
                            title = title.take(200),
                            url = url,
                            isFavorite = obj.optBoolean("isFavorite", false)
                        )
                        count++
                    }
                }
            }

            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    val title = obj.optString("title")
                    val content = obj.optString("content")
                    val url = obj.optString("url").takeIf { it.isNotBlank() }
                    browserRepository.addNote(
                        title = title.take(200),
                        content = content,
                        url = url,
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                    count++
                }
            }

            val rlArr = root.optJSONArray("readingList")
            if (rlArr != null) {
                for (i in 0 until rlArr.length()) {
                    val obj = rlArr.getJSONObject(i)
                    val url = obj.optString("url")
                    val title = obj.optString("title")
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        browserRepository.addToReadingList(title.take(200), url)
                        count++
                    }
                }
            }
        } catch (_: Exception) {}
        count
    }

    /**
     * Import Netscape Bookmark HTML
     */
    suspend fun importBookmarkHtml(
        htmlString: String,
        browserRepository: BrowserRepository
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val regex = Regex("""<A\s+HREF="([^"]+)"[^>]*>([^<]+)</A>""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(htmlString)

        for (match in matches) {
            val url = match.groupValues[1].trim()
            val title = match.groupValues[2].trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                browserRepository.addBookmark(title = title.take(200), url = url)
                count++
            }
        }
        count
    }
}

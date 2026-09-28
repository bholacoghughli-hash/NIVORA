package com.nivora.browser.data.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nivora.browser.data.repository.BrowserRepository
import com.nivora.browser.database.entities.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ActiveDownloadProgress(
    val downloadId: Long,
    val fileName: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progressPercent: Int,
    val status: Int
)

class NivoraDownloadManager(
    private val context: Context,
    private val browserRepository: BrowserRepository,
    private val scope: CoroutineScope
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _activeDownloads = MutableStateFlow<Map<Long, ActiveDownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<Long, ActiveDownloadProgress>> = _activeDownloads.asStateFlow()

    fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null
    ): Long {
        return try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val uri = Uri.parse(url)

            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("Downloading with NIVORA")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                if (userAgent != null) {
                    addRequestHeader("User-Agent", userAgent)
                }
                addRequestHeader("DNT", "1")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                try {
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                } catch (_: Exception) {
                    // Fallback to internal app files directory
                }
            }

            val id = downloadManager.enqueue(request)
            Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()

            // Record initial entry in database
            val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            scope.launch {
                browserRepository.recordDownload(
                    fileName = fileName,
                    url = url,
                    filePath = targetFile.absolutePath,
                    totalBytes = 0L
                )
            }
            id
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed to start: ${e.message}", Toast.LENGTH_SHORT).show()
            -1L
        }
    }

    fun openDownloadedFile(item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File does not exist: ${item.fileName}", Toast.LENGTH_SHORT).show()
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDownloadedFile(item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) {
                // If local file is missing, share URL
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, item.url)
                    putExtra(Intent.EXTRA_TITLE, item.fileName)
                }
                context.startActivity(Intent.createChooser(intent, "Share file link"))
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${item.fileName}"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun retryDownload(item: DownloadItem) {
        startDownload(item.url)
    }

    fun deleteDownload(item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            scope.launch {
                browserRepository.deleteDownloadById(item.id)
            }
            Toast.makeText(context, "Deleted ${item.fileName}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

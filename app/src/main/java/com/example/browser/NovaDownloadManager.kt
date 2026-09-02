package com.example.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.DownloadDao
import com.example.data.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class NovaDownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val scope: CoroutineScope
) {
    private val systemDownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    fun startDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        try {
            val guessedFileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading $guessedFileName with NOVA Browser")
                setTitle(guessedFileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, guessedFileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = systemDownloadManager?.enqueue(request) ?: 0L

            val downloadFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                guessedFileName
            )

            scope.launch(Dispatchers.IO) {
                downloadDao.insertDownload(
                    DownloadItem(
                        downloadId = downloadId,
                        fileName = guessedFileName,
                        url = url,
                        filePath = downloadFile.absolutePath,
                        fileSize = contentLength,
                        mimeType = mimeType,
                        timestamp = System.currentTimeMillis(),
                        status = "DOWNLOADING"
                    )
                )
            }

            Toast.makeText(context, "Download started: $guessedFileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start download: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openDownloadedFile(item: DownloadItem) {
        try {
            val file = item.filePath?.let { File(it) }
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, item.mimeType ?: getMimeType(file.extension))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // Try viewing download Uri
                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDownloadedFile(item: DownloadItem) {
        try {
            val file = item.filePath?.let { File(it) }
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = item.mimeType ?: getMimeType(file.extension)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share file via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownload(item: DownloadItem) {
        scope.launch(Dispatchers.IO) {
            downloadDao.deleteDownloadById(item.id)
            try {
                item.filePath?.let {
                    val file = File(it)
                    if (file.exists()) file.delete()
                }
            } catch (e: Exception) {
                // Ignore file removal errors
            }
        }
    }

    private fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
    }
}

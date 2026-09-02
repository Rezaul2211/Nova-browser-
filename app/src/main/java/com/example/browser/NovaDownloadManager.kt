package com.example.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.DownloadDao
import com.example.data.DownloadItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        // 1. Handle base64 Data URLs (e.g. data:image/png;base64,...)
        if (cleanUrl.startsWith("data:", ignoreCase = true)) {
            downloadDataUrl(cleanUrl, mimeType)
            return
        }

        // 2. Handle HTTP/HTTPS URLs
        try {
            val guessedFileName = URLUtil.guessFileName(cleanUrl, contentDisposition, mimeType)
                .let { if (it.isBlank() || it == "downloadfile") "nova_download_${System.currentTimeMillis()}" else it }
            val resolvedMimeType = if (mimeType.isNotBlank() && mimeType != "application/octet-stream") {
                mimeType
            } else {
                getMimeType(File(guessedFileName).extension)
            }

            val request = DownloadManager.Request(Uri.parse(cleanUrl)).apply {
                setMimeType(resolvedMimeType)
                if (userAgent.isNotBlank()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                setDescription("Downloading $guessedFileName with AUREN Browser")
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
                        url = cleanUrl,
                        filePath = downloadFile.absolutePath,
                        fileSize = contentLength.coerceAtLeast(0),
                        mimeType = resolvedMimeType,
                        timestamp = System.currentTimeMillis(),
                        status = "DOWNLOADING"
                    )
                )
            }

            Toast.makeText(context, "Download started: $guessedFileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fallback direct stream download in background
            downloadDirectStream(cleanUrl, mimeType)
        }
    }

    fun downloadImageOrMedia(mediaUrl: String, customName: String? = null) {
        val clean = mediaUrl.trim()
        if (clean.isBlank()) return

        if (clean.startsWith("data:", ignoreCase = true)) {
            downloadDataUrl(clean, "image/jpeg")
            return
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(clean).ifBlank { "jpg" }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = customName ?: "auren_media_${timeStamp}.$extension"

        startDownload(
            url = clean,
            userAgent = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36",
            contentDisposition = "attachment; filename=\"$fileName\"",
            mimeType = getMimeType(extension),
            contentLength = 0L
        )
    }

    private fun downloadDataUrl(dataUrl: String, fallbackMime: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val parts = dataUrl.split(",", limit = 2)
                if (parts.size < 2) return@launch
                val header = parts[0]
                val base64Data = parts[1]

                val mime = if (header.contains(":") && header.contains(";")) {
                    header.substringAfter(":").substringBefore(";")
                } else fallbackMime

                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "png"
                val fileName = "auren_image_${System.currentTimeMillis()}.$ext"
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val targetFile = File(downloadDir, fileName)
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                FileOutputStream(targetFile).use { it.write(bytes) }

                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mime), null)

                downloadDao.insertDownload(
                    DownloadItem(
                        downloadId = System.currentTimeMillis(),
                        fileName = fileName,
                        url = "data:$mime",
                        filePath = targetFile.absolutePath,
                        fileSize = bytes.size.toLong(),
                        mimeType = mime,
                        timestamp = System.currentTimeMillis(),
                        status = "COMPLETED"
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved image to Downloads: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save data image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadDirectStream(url: String, mimeType: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val u = URL(url)
                val conn = u.openConnection()
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.connect()

                val fileName = URLUtil.guessFileName(url, null, mimeType).ifBlank { "download_${System.currentTimeMillis()}" }
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val targetFile = File(downloadDir, fileName)
                conn.getInputStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)

                downloadDao.insertDownload(
                    DownloadItem(
                        downloadId = System.currentTimeMillis(),
                        fileName = fileName,
                        url = url,
                        filePath = targetFile.absolutePath,
                        fileSize = targetFile.length(),
                        mimeType = mimeType.ifBlank { getMimeType(targetFile.extension) },
                        timestamp = System.currentTimeMillis(),
                        status = "COMPLETED"
                    )
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloaded $fileName successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
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

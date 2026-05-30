package ir.amirroid.chatguard.core.download.implementation

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import ir.amirroid.chatguard.core.download.abstraction.Downloader

class DownloadManagerDownloaderImpl(
    private val context: Context
) : Downloader {
    override fun download(
        url: String,
        fileName: String,
        mimeType: String?
    ): Long {
        val request = DownloadManager.Request(url.toUri())
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        mimeType?.let(request::setMimeType)

        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )

        val manager = context.getSystemService(DownloadManager::class.java)
        return manager.enqueue(request)
    }
}
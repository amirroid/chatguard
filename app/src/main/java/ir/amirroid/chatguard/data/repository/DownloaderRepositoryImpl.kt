package ir.amirroid.chatguard.data.repository

import ir.amirroid.chatguard.core.download.abstraction.Downloader
import ir.amirroid.chatguard.domain.repository.DownloaderRepository

class DownloaderRepositoryImpl(private val downloader: Downloader) : DownloaderRepository {
    override fun download(
        url: String,
        fileName: String,
        mimeType: String?
    ): Long {
        return downloader.download(url, fileName, mimeType)
    }
}
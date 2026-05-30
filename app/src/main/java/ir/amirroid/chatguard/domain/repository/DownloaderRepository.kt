package ir.amirroid.chatguard.domain.repository

interface DownloaderRepository {
    fun download(
        url: String,
        fileName: String,
        mimeType: String? = null
    ): Long
}
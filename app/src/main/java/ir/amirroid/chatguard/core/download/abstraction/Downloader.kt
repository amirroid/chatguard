package ir.amirroid.chatguard.core.download.abstraction

interface Downloader {
    fun download(
        url: String,
        fileName: String,
        mimeType: String? = null
    ): Long
}
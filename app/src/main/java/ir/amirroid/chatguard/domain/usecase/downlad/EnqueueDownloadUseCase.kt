package ir.amirroid.chatguard.domain.usecase.downlad

import ir.amirroid.chatguard.domain.repository.DownloaderRepository

class EnqueueDownloadUseCase(
    private val repository: DownloaderRepository
) {
    operator fun invoke(
        url: String, filename: String, mimeType: String?
    ) = repository.download(url, filename, mimeType)
}
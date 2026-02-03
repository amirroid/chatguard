package ir.amirroid.chatguard.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun readFile(uri: Uri): ByteArray?
    suspend fun writeFile(uri: Uri, content: ByteArray)
}
package ir.sysfail.chatguard.data.repository

import android.content.Context
import android.net.Uri
import ir.sysfail.chatguard.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepositoryImpl(
    private val context: Context,
) : StorageRepository {

    override suspend fun readFile(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        }.getOrNull()
    }

    override suspend fun writeFile(uri: Uri, content: ByteArray) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(content)
            output.flush()
        } ?: error("Unable to open output stream for uri: $uri")
    }
}

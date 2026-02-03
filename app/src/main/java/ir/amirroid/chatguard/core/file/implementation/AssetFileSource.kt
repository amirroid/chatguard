package ir.amirroid.chatguard.core.file.implementation

import android.content.Context
import ir.amirroid.chatguard.core.file.abstraction.FileSource
import java.io.InputStream

open class AssetFileSource(
    private val context: Context,
    private val assetPath: String
) : FileSource {
    override fun open(): InputStream = context.assets.open(assetPath)
}

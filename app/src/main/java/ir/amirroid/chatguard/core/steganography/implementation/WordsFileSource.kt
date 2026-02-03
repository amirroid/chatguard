package ir.amirroid.chatguard.core.steganography.implementation

import android.content.Context
import ir.amirroid.chatguard.core.file.implementation.AssetFileSource

class WordsFileSource(context: Context) : AssetFileSource(
    context = context,
    assetPath = WORDS_FILE
) {
    companion object {
        private const val WORDS_FILE = "words.txt"
    }
}
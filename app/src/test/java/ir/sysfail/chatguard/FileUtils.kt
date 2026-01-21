package ir.sysfail.chatguard

object FileUtils {
    fun getWordsBytes() = javaClass.getResourceAsStream("/words.txt")
        ?.use { it.readBytes() }
        ?: throw IllegalStateException("words.txt not found")
}
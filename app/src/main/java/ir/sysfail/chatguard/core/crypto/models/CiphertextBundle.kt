package ir.sysfail.chatguard.core.crypto.models

data class CiphertextBundle(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val authTag: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CiphertextBundle

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!authTag.contentEquals(other.authTag)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + (authTag?.contentHashCode() ?: 0)
        return result
    }
}
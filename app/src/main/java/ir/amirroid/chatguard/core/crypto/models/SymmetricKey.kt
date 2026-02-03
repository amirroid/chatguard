package ir.amirroid.chatguard.core.crypto.models

data class SymmetricKey(
    val keyBytes: ByteArray,
    val algorithm: String = "AES"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SymmetricKey

        if (!keyBytes.contentEquals(other.keyBytes)) return false
        if (algorithm != other.algorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyBytes.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}
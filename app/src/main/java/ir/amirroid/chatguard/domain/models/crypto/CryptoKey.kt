package ir.amirroid.chatguard.domain.models.crypto

data class CryptoKey(
    val encoded: ByteArray,
    val algorithm: String = "EC",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CryptoKey

        if (!encoded.contentEquals(other.encoded)) return false
        if (algorithm != other.algorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encoded.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}
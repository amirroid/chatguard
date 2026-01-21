package ir.sysfail.chatguard.core.crypto.models

data class SignedPublicKey(
    val publicKey: ByteArray,
    val signature: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedPublicKey

        if (timestamp != other.timestamp) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
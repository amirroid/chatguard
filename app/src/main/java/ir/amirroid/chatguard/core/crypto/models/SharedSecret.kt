package ir.amirroid.chatguard.core.crypto.models

data class SharedSecret(
    val secretBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SharedSecret

        if (!secretBytes.contentEquals(other.secretBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return secretBytes.contentHashCode()
    }
}
package ir.amirroid.chatguard.core.crypto.models

data class DerivedKeyMaterial(
    val encryptionKey: SymmetricKey,
    val nonce: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DerivedKeyMaterial

        if (encryptionKey != other.encryptionKey) return false
        if (!nonce.contentEquals(other.nonce)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionKey.hashCode()
        result = 31 * result + nonce.contentHashCode()
        return result
    }
}
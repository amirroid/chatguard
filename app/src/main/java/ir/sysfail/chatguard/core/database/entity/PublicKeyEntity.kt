package ir.sysfail.chatguard.core.database.entity

import androidx.room.Entity

@Entity(primaryKeys = ["appPackageName", "username"])
data class PublicKeyEntity(
    val username: String,
    val appPackageName: String,
    val publicKey: ByteArray,
    val algorithm: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKeyEntity

        if (username != other.username) return false
        if (appPackageName != other.appPackageName) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (algorithm != other.algorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + appPackageName.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}
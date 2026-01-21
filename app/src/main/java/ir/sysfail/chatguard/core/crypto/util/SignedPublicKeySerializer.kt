package ir.sysfail.chatguard.core.crypto.util

import ir.sysfail.chatguard.core.crypto.models.SignedPublicKey

object SignedPublicKeySerializer {

    fun serialize(signedKey: SignedPublicKey): ByteArray {
        fun Int.toBytes() = byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )

        fun Long.toBytes() = byteArrayOf(
            (this shr 56).toByte(),
            (this shr 48).toByte(),
            (this shr 40).toByte(),
            (this shr 32).toByte(),
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )

        return buildList {
            add(signedKey.publicKey.size.toBytes())
            add(signedKey.publicKey)
            add(signedKey.signature.size.toBytes())
            add(signedKey.signature)
            add(signedKey.timestamp.toBytes())
        }.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    }

    fun deserialize(data: ByteArray): SignedPublicKey {
        var offset = 0

        fun readInt(): Int {
            if (offset + 4 > data.size) throw IllegalArgumentException("Data too short for reading Int")
            val value = ((data[offset].toInt() and 0xFF) shl 24) or
                    ((data[offset + 1].toInt() and 0xFF) shl 16) or
                    ((data[offset + 2].toInt() and 0xFF) shl 8) or
                    (data[offset + 3].toInt() and 0xFF)
            offset += 4
            return value
        }

        fun readLong(): Long {
            if (offset + 8 > data.size) throw IllegalArgumentException("Data too short for reading Long")
            val value = ((data[offset].toLong() and 0xFF) shl 56) or
                    ((data[offset + 1].toLong() and 0xFF) shl 48) or
                    ((data[offset + 2].toLong() and 0xFF) shl 40) or
                    ((data[offset + 3].toLong() and 0xFF) shl 32) or
                    ((data[offset + 4].toLong() and 0xFF) shl 24) or
                    ((data[offset + 5].toLong() and 0xFF) shl 16) or
                    ((data[offset + 6].toLong() and 0xFF) shl 8) or
                    (data[offset + 7].toLong() and 0xFF)
            offset += 8
            return value
        }

        fun readBytes(): ByteArray {
            val size = readInt()
            if (offset + size > data.size) throw IllegalArgumentException("Data too short for reading bytes of size $size")
            val bytes = data.copyOfRange(offset, offset + size)
            offset += size
            return bytes
        }

        val publicKey = readBytes()
        val signature = readBytes()
        val timestamp = readLong()

        return SignedPublicKey(
            publicKey = publicKey,
            signature = signature,
            timestamp = timestamp
        )
    }
}
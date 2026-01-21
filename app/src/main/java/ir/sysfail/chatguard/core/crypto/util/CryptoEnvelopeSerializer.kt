package ir.sysfail.chatguard.core.crypto.util

import ir.sysfail.chatguard.core.crypto.models.CryptoEnvelope

object CryptoEnvelopeSerializer {

    fun serialize(envelope: CryptoEnvelope): ByteArray {
        fun Int.toBytes() = byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )

        return buildList {
            // Receiver envelope fields
            add(envelope.receiverEphemeralPublicKey.size.toBytes())
            add(envelope.receiverEphemeralPublicKey)

            add(envelope.receiverSignature.size.toBytes())
            add(envelope.receiverSignature)

            add(envelope.ciphertext.size.toBytes())
            add(envelope.ciphertext)

            add(envelope.nonce.size.toBytes())
            add(envelope.nonce)

            val authSize = envelope.authTag?.size ?: 0
            add(authSize.toBytes())
            envelope.authTag?.let { add(it) }

            // Sender envelope fields
            add(envelope.senderEphemeralPublicKey.size.toBytes())
            add(envelope.senderEphemeralPublicKey)

            add(envelope.senderSignature.size.toBytes())
            add(envelope.senderSignature)

            add(envelope.senderWrappedKey.size.toBytes())
            add(envelope.senderWrappedKey)

            add(envelope.senderWrappedKeyNonce.size.toBytes())
            add(envelope.senderWrappedKeyNonce)

            val senderAuthSize = envelope.senderWrappedKeyAuthTag?.size ?: 0
            add(senderAuthSize.toBytes())
            envelope.senderWrappedKeyAuthTag?.let { add(it) }

        }.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    }

    fun deserialize(data: ByteArray): CryptoEnvelope {
        var offset = 0

        fun readInt(): Int {
            if (offset + 4 > data.size) {
                throw IllegalArgumentException("Data too short for reading Int at offset $offset")
            }
            val value = ((data[offset].toInt() and 0xFF) shl 24) or
                    ((data[offset + 1].toInt() and 0xFF) shl 16) or
                    ((data[offset + 2].toInt() and 0xFF) shl 8) or
                    (data[offset + 3].toInt() and 0xFF)
            offset += 4
            return value
        }

        fun readBytes(): ByteArray {
            val size = readInt()
            if (size < 0) {
                throw IllegalArgumentException("Invalid negative size: $size at offset ${offset - 4}")
            }
            if (offset + size > data.size) {
                throw IllegalArgumentException("Data too short for reading bytes of size $size at offset $offset")
            }
            val bytes = data.copyOfRange(offset, offset + size)
            offset += size
            return bytes
        }

        // Receiver envelope fields
        val receiverEphemeralPublicKey = readBytes()
        val receiverSignature = readBytes()
        val ciphertext = readBytes()
        val nonce = readBytes()

        val authTagSize = readInt()
        val authTag = if (authTagSize > 0) {
            if (offset + authTagSize > data.size) {
                throw IllegalArgumentException("Data too short for reading authTag of size $authTagSize at offset $offset")
            }
            val tag = data.copyOfRange(offset, offset + authTagSize)
            offset += authTagSize
            tag
        } else null

        // Sender envelope fields
        val senderEphemeralPublicKey = readBytes()
        val senderSignature = readBytes()
        val senderWrappedKey = readBytes()
        val senderWrappedKeyNonce = readBytes()

        val senderWrappedKeyAuthTagSize = readInt()
        val senderWrappedKeyAuthTag = if (senderWrappedKeyAuthTagSize > 0) {
            if (offset + senderWrappedKeyAuthTagSize > data.size) {
                throw IllegalArgumentException("Data too short for reading senderWrappedKeyAuthTag of size $senderWrappedKeyAuthTagSize at offset $offset")
            }
            val tag = data.copyOfRange(offset, offset + senderWrappedKeyAuthTagSize)
            offset += senderWrappedKeyAuthTagSize
            tag
        } else null

        return CryptoEnvelope(
            receiverEphemeralPublicKey = receiverEphemeralPublicKey,
            receiverSignature = receiverSignature,
            ciphertext = ciphertext,
            nonce = nonce,
            authTag = authTag,
            senderEphemeralPublicKey = senderEphemeralPublicKey,
            senderSignature = senderSignature,
            senderWrappedKey = senderWrappedKey,
            senderWrappedKeyNonce = senderWrappedKeyNonce,
            senderWrappedKeyAuthTag = senderWrappedKeyAuthTag
        )
    }
}
package ir.amirroid.chatguard.core.crypto.util

import ir.amirroid.chatguard.core.crypto.models.CryptoEnvelope

/**
 * Binary serializer for [CryptoEnvelope] using a fixed-layout v2 format to minimize size.
 */
object CryptoEnvelopeSerializer {

    private const val FORMAT_VERSION: Byte = 2
    private const val NONCE_LENGTH = 12
    private const val AUTH_TAG_LENGTH = 16

    fun serialize(envelope: CryptoEnvelope): ByteArray {
        require(envelope.nonce.size == NONCE_LENGTH) {
            "Nonce must be $NONCE_LENGTH bytes, got ${envelope.nonce.size}"
        }
        val authTag = envelope.authTag
            ?: throw IllegalArgumentException("Auth tag is required for v2 envelopes")
        require(authTag.size == AUTH_TAG_LENGTH) {
            "Auth tag must be $AUTH_TAG_LENGTH bytes, got ${authTag.size}"
        }

        val ciphertext = envelope.ciphertext
        val lengthField = ciphertext.size

        return ByteArray(1 + NONCE_LENGTH + 4 + lengthField + AUTH_TAG_LENGTH).also { out ->
            var offset = 0
            out[offset++] = FORMAT_VERSION
            envelope.nonce.copyInto(out, offset)
            offset += NONCE_LENGTH
            out[offset++] = (lengthField shr 24).toByte()
            out[offset++] = (lengthField shr 16).toByte()
            out[offset++] = (lengthField shr 8).toByte()
            out[offset++] = lengthField.toByte()
            ciphertext.copyInto(out, offset)
            offset += lengthField
            authTag.copyInto(out, offset)
        }
    }

    fun deserialize(data: ByteArray): CryptoEnvelope {
        val minSize = 1 + NONCE_LENGTH + 4 + AUTH_TAG_LENGTH
        require(data.size >= minSize) { "Envelope data too short (${data.size} bytes)" }

        var offset = 0
        val version = data[offset++]
        require(version == FORMAT_VERSION) {
            "Unsupported envelope version: $version (expected $FORMAT_VERSION)"
        }

        val nonce = data.copyOfRange(offset, offset + NONCE_LENGTH)
        offset += NONCE_LENGTH

        val ciphertextLength = readIntBE(data, offset)
        offset += 4
        require(ciphertextLength >= 0) { "Invalid ciphertext length: $ciphertextLength" }

        val expectedEnd = offset + ciphertextLength + AUTH_TAG_LENGTH
        require(data.size >= expectedEnd) {
            "Envelope too short: need at least $expectedEnd bytes, got ${data.size}"
        }
        // Poetic decoding may append a padding byte; ignore trailing bytes after the tag.

        val ciphertext = data.copyOfRange(offset, offset + ciphertextLength)
        offset += ciphertextLength
        val authTag = data.copyOfRange(offset, offset + AUTH_TAG_LENGTH)

        return CryptoEnvelope(
            nonce = nonce,
            ciphertext = ciphertext,
            authTag = authTag,
        )
    }

    private fun readIntBE(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }
}

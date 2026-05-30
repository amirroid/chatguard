package ir.amirroid.chatguard.crypto

import ir.amirroid.chatguard.core.crypto.models.CryptoEnvelope
import ir.amirroid.chatguard.core.crypto.util.CryptoEnvelopeSerializer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CryptoEnvelopeSerializerTest {

    @Test
    fun `serialize and deserialize round-trip`() {
        val envelope = CryptoEnvelope(
            nonce = ByteArray(12) { it.toByte() },
            ciphertext = "hello".toByteArray(),
            authTag = ByteArray(16) { (it + 1).toByte() },
        )

        val bytes = CryptoEnvelopeSerializer.serialize(envelope)
        val restored = CryptoEnvelopeSerializer.deserialize(bytes)

        assertArrayEquals(envelope.nonce, restored.nonce)
        assertArrayEquals(envelope.ciphertext, restored.ciphertext)
        assertArrayEquals(envelope.authTag, restored.authTag)
    }

    @Test
    fun `deserialize tolerates trailing padding byte from poetic decode`() {
        val envelope = CryptoEnvelope(
            nonce = ByteArray(12) { it.toByte() },
            ciphertext = "secret".toByteArray(),
            authTag = ByteArray(16) { (it + 2).toByte() },
        )
        val bytes = CryptoEnvelopeSerializer.serialize(envelope) + byteArrayOf(0)

        val restored = CryptoEnvelopeSerializer.deserialize(bytes)

        assertArrayEquals(envelope.nonce, restored.nonce)
        assertArrayEquals(envelope.ciphertext, restored.ciphertext)
        assertArrayEquals(envelope.authTag, restored.authTag)
    }

    @Test
    fun `deserialize rejects wrong version`() {
        val envelope = CryptoEnvelope(
            nonce = ByteArray(12),
            ciphertext = byteArrayOf(1),
            authTag = ByteArray(16),
        )
        val bytes = CryptoEnvelopeSerializer.serialize(envelope).copyOf()
        bytes[0] = 1

        assertThrows(IllegalArgumentException::class.java) {
            CryptoEnvelopeSerializer.deserialize(bytes)
        }
    }
}

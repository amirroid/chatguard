package ir.amirroid.chatguard.core.crypto.implementation

import ir.amirroid.chatguard.core.crypto.abstraction.CipherEngine
import ir.amirroid.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.abstraction.SharedSecretDeriver
import ir.amirroid.chatguard.core.crypto.models.CiphertextBundle
import ir.amirroid.chatguard.core.crypto.models.CryptoEnvelope
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey

/**
 * End-to-end message encryption using a single long-term ECDH shared secret per peer pair.
 *
 * Flow:
 * 1. After a one-time public-key exchange, both parties share the same ECDH secret.
 * 2. HKDF derives one AES key per peer pair from the shared secret.
 * 3. Each message uses a fresh random GCM nonce (stored in the envelope).
 * 4. The envelope stores only nonce, ciphertext, and authentication tag.
 *
 * Sender and recipient use the same decrypt path, so senders can re-read their own messages
 * without extra key-wrapping or a second ephemeral keypair.
 */
class DefaultCryptoOrchestrator(
    private val cipherEngine: CipherEngine,
    private val secretDeriver: SharedSecretDeriver,
) : CryptoOrchestrator {

    override suspend fun encryptMessage(
        plaintext: ByteArray,
        myIdentityPrivateKey: PrivateKey,
        myIdentityPublicKey: PublicKey,
        theirIdentityPublicKey: PublicKey,
    ): Result<CryptoEnvelope> = runCatching {
        val sharedSecret = secretDeriver.deriveSharedSecret(
            myIdentityPrivateKey,
            theirIdentityPublicKey,
        ).getOrThrow()

        val keyMaterial = secretDeriver.deriveEncryptionKey(
            sharedSecret,
            info = MESSAGE_KEY_INFO,
            salt = CONVERSATION_SALT,
        ).getOrThrow()

        val bundle = cipherEngine.encrypt(
            keyMaterial.encryptionKey,
            plaintext,
            associatedData = byteArrayOf(),
        ).getOrThrow()

        CryptoEnvelope(
            nonce = bundle.nonce,
            ciphertext = bundle.ciphertext,
            authTag = bundle.authTag,
        )
    }

    override suspend fun decryptMessage(
        envelope: CryptoEnvelope,
        myIdentityPrivateKey: PrivateKey,
        myIdentityPublicKey: PublicKey,
        theirIdentityPublicKey: PublicKey,
        iAmSender: Boolean,
    ): Result<ByteArray> = runCatching {
        // Same ECDH inputs for sender and receiver; iAmSender kept for API compatibility.
        val sharedSecret = secretDeriver.deriveSharedSecret(
            myIdentityPrivateKey,
            theirIdentityPublicKey,
        ).getOrThrow()

        val keyMaterial = secretDeriver.deriveEncryptionKey(
            sharedSecret,
            info = MESSAGE_KEY_INFO,
            salt = CONVERSATION_SALT,
        ).getOrThrow()

        val bundle = CiphertextBundle(
            ciphertext = envelope.ciphertext,
            nonce = envelope.nonce,
            authTag = envelope.authTag,
        )

        cipherEngine.decrypt(
            keyMaterial.encryptionKey,
            bundle,
            associatedData = byteArrayOf(),
        ).getOrThrow()
    }

    companion object {
        private val MESSAGE_KEY_INFO = "ChatGuard-Message-v2".toByteArray(Charsets.UTF_8)
        private val CONVERSATION_SALT = "ChatGuard-Conversation-v2".toByteArray(Charsets.UTF_8)
    }
}

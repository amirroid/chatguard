package ir.amirroid.chatguard.integration

import ir.amirroid.chatguard.FileUtils
import ir.amirroid.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.abstraction.KeyManager
import ir.amirroid.chatguard.core.crypto.abstraction.SignatureValidator
import ir.amirroid.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.amirroid.chatguard.core.crypto.implementation.DefaultCryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.amirroid.chatguard.core.crypto.implementation.EcdsaSignatureValidator
import ir.amirroid.chatguard.core.crypto.implementation.HkdfSecretDeriver
import ir.amirroid.chatguard.core.crypto.models.SignedPublicKey
import ir.amirroid.chatguard.core.crypto.util.CryptoEnvelopeSerializer
import ir.amirroid.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.amirroid.chatguard.core.file.implementation.ByteArrayFileSource
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.amirroid.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FullFlowCryptoSteganographyTest {

    private lateinit var keyManager: KeyManager
    private lateinit var signatureValidator: SignatureValidator
    private lateinit var cryptoOrchestrator: CryptoOrchestrator
    private lateinit var corpusProvider: CorpusProvider
    private lateinit var poeticEncoder: PoeticEncoder
    private lateinit var poeticDecoder: PoeticDecoder

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        signatureValidator = EcdsaSignatureValidator()
        cryptoOrchestrator = DefaultCryptoOrchestrator(
            cipherEngine = AesGcmCipherEngine(),
            secretDeriver = HkdfSecretDeriver(),
        )

        val wordsBytes = FileUtils.getWordsBytes()
        corpusProvider = CachedCorpusProvider(ByteArrayFileSource(wordsBytes))
        poeticEncoder = WordBasedEncoder(corpusProvider)
        poeticDecoder = WordBasedDecoder(corpusProvider)
    }

    @Test
    fun `full flow encrypt decrypt using signed poetic public key`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "سلام! این یک پیام محرمانه است."

        val bobSignedKey = SignedPublicKey(
            publicKey = bob.publicKey.encoded,
            signature = signatureValidator.sign(bob.privateKey, bob.publicKey.encoded).getOrThrow(),
        )
        val decodedBobKey = SignedPublicKeySerializer.deserialize(
            poeticDecoder.decode(poeticEncoder.encode(SignedPublicKeySerializer.serialize(bobSignedKey)).getOrThrow()).getOrThrow(),
        )
        val bobPublicKey = keyManager.reconstructPublicKey(decodedBobKey.publicKey).getOrThrow()
        assertTrue(
            signatureValidator.verify(bobPublicKey, decodedBobKey.publicKey, decodedBobKey.signature).getOrThrow(),
        )

        val envelope = cryptoOrchestrator.encryptMessage(
            message.toByteArray(),
            alice.privateKey,
            alice.publicKey,
            bobPublicKey,
        ).getOrThrow()

        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(
            poeticDecoder.decode(
                poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope)).getOrThrow(),
            ).getOrThrow(),
        )

        assertEquals(
            message,
            String(
                cryptoOrchestrator.decryptMessage(
                    decodedEnvelope,
                    bob.privateKey,
                    bob.publicKey,
                    alice.publicKey,
                    iAmSender = false,
                ).getOrThrow(),
            ),
        )
    }

    @Test
    fun `full flow long message and sender re-read`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = buildString {
            repeat(2000) {
                append("This is a long test message number $it. ")
                if (it % 10 == 0) append("\n")
            }
        }

        val bobSignedKey = SignedPublicKey(
            publicKey = bob.publicKey.encoded,
            signature = signatureValidator.sign(bob.privateKey, bob.publicKey.encoded).getOrThrow(),
        )
        val bobPublicKey = keyManager.reconstructPublicKey(
            SignedPublicKeySerializer.deserialize(
                poeticDecoder.decode(poeticEncoder.encode(SignedPublicKeySerializer.serialize(bobSignedKey)).getOrThrow()).getOrThrow(),
            ).publicKey,
        ).getOrThrow()

        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(
            poeticDecoder.decode(
                poeticEncoder.encode(
                    CryptoEnvelopeSerializer.serialize(
                        cryptoOrchestrator.encryptMessage(
                            message.toByteArray(),
                            alice.privateKey,
                            alice.publicKey,
                            bobPublicKey,
                        ).getOrThrow(),
                    ),
                ).getOrThrow(),
            ).getOrThrow(),
        )

        assertEquals(
            message,
            String(
                cryptoOrchestrator.decryptMessage(
                    decodedEnvelope,
                    bob.privateKey,
                    bob.publicKey,
                    alice.publicKey,
                    iAmSender = false,
                ).getOrThrow(),
            ),
        )
        assertEquals(
            message,
            String(
                cryptoOrchestrator.decryptMessage(
                    decodedEnvelope,
                    alice.privateKey,
                    alice.publicKey,
                    bob.publicKey,
                    iAmSender = true,
                ).getOrThrow(),
            ),
        )
    }
}

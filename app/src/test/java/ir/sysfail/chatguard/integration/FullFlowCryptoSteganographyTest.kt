package ir.sysfail.chatguard.integration

import ir.sysfail.chatguard.FileUtils
import ir.sysfail.chatguard.core.crypto.abstraction.CipherEngine
import ir.sysfail.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.sysfail.chatguard.core.crypto.abstraction.KeyManager
import ir.sysfail.chatguard.core.crypto.abstraction.SharedSecretDeriver
import ir.sysfail.chatguard.core.crypto.abstraction.SignatureValidator
import ir.sysfail.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.sysfail.chatguard.core.crypto.implementation.DefaultCryptoOrchestrator
import ir.sysfail.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.sysfail.chatguard.core.crypto.implementation.EcdsaSignatureValidator
import ir.sysfail.chatguard.core.crypto.implementation.HkdfSecretDeriver
import ir.sysfail.chatguard.core.crypto.models.SignedPublicKey
import ir.sysfail.chatguard.core.crypto.util.CryptoEnvelopeSerializer
import ir.sysfail.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.sysfail.chatguard.core.file.implementation.ByteArrayFileSource
import ir.sysfail.chatguard.core.steganography.abstraction.CorpusProvider
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.sysfail.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.sysfail.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.sysfail.chatguard.core.steganography.implementation.WordBasedEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FullFlowCryptoSteganographyTest {
    private lateinit var keyManager: KeyManager
    private lateinit var cipherEngine: CipherEngine
    private lateinit var signatureValidator: SignatureValidator
    private lateinit var secretDeriver: SharedSecretDeriver
    private lateinit var cryptoOrchestrator: CryptoOrchestrator

    private lateinit var corpusProvider: CorpusProvider
    private lateinit var poeticEncoder: PoeticEncoder
    private lateinit var poeticDecoder: PoeticDecoder

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        cipherEngine = AesGcmCipherEngine()
        signatureValidator = EcdsaSignatureValidator()
        secretDeriver = HkdfSecretDeriver()
        cryptoOrchestrator = DefaultCryptoOrchestrator(
            keyManager, cipherEngine, signatureValidator, secretDeriver
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

        val signature = signatureValidator.sign(
            bob.privateKey,
            bob.publicKey.encoded
        ).getOrThrow()

        val signedPublicKey = SignedPublicKey(
            publicKey = bob.publicKey.encoded,
            signature = signature
        )

        val signedKeyBytes = SignedPublicKeySerializer.serialize(signedPublicKey)
        val poeticText = poeticEncoder.encode(signedKeyBytes).getOrThrow()

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedSignedKey = SignedPublicKeySerializer.deserialize(decodedBytes)

        val reconstructedBobPublicKey = keyManager
            .reconstructPublicKey(decodedSignedKey.publicKey)
            .getOrThrow()

        val isValid = signatureValidator.verify(
            reconstructedBobPublicKey,
            decodedSignedKey.publicKey,
            decodedSignedKey.signature
        ).getOrThrow()

        assertTrue(isValid)

        val envelope = cryptoOrchestrator.encryptMessage(
            message.toByteArray(),
            alice.privateKey,
            alice.publicKey,
            reconstructedBobPublicKey,
        ).getOrThrow()

        val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
        val poeticEnvelope = poeticEncoder.encode(envelopeBytes).getOrThrow()

        val decodedEnvelopeBytes = poeticDecoder.decode(poeticEnvelope).getOrThrow()
        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(decodedEnvelopeBytes)

        val decrypted = cryptoOrchestrator.decryptMessage(
            decodedEnvelope,
            bob.privateKey,
            bob.publicKey,
            alice.publicKey,
            false
        ).getOrThrow()

        assertEquals(message, String(decrypted))
    }

    @Test
    fun `full flow encrypt decrypt long message using signed poetic public key`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()

        val message = buildString {
            repeat(2000) {
                append("This is a long test message number $it. ")
                if (it % 10 == 0) append("\n")
            }
        }

        val signature = signatureValidator.sign(
            bob.privateKey,
            bob.publicKey.encoded
        ).getOrThrow()

        val signedPublicKey = SignedPublicKey(
            publicKey = bob.publicKey.encoded,
            signature = signature
        )

        val signedKeyBytes = SignedPublicKeySerializer.serialize(signedPublicKey)
        val poeticText = poeticEncoder.encode(signedKeyBytes).getOrThrow()

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedSignedKey = SignedPublicKeySerializer.deserialize(decodedBytes)

        val reconstructedBobPublicKey = keyManager
            .reconstructPublicKey(decodedSignedKey.publicKey)
            .getOrThrow()

        val isValid = signatureValidator.verify(
            reconstructedBobPublicKey,
            decodedSignedKey.publicKey,
            decodedSignedKey.signature
        ).getOrThrow()

        assertTrue(isValid)

        val envelope = cryptoOrchestrator.encryptMessage(
            message.toByteArray(),
            alice.privateKey,
            alice.publicKey,
            reconstructedBobPublicKey,
        ).getOrThrow()

        val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
        val poeticEnvelope = poeticEncoder.encode(envelopeBytes).getOrThrow()

        val decodedEnvelopeBytes = poeticDecoder.decode(poeticEnvelope).getOrThrow()
        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(decodedEnvelopeBytes)

        val decrypted = cryptoOrchestrator.decryptMessage(
            decodedEnvelope,
            bob.privateKey,
            bob.publicKey,
            alice.publicKey,
            false
        ).getOrThrow()

        val myDecrypted = cryptoOrchestrator.decryptMessage(
            decodedEnvelope,
            alice.privateKey,
            alice.publicKey,
            bob.publicKey,
            true
        ).getOrThrow()

        assertEquals(message, String(decrypted))
        assertEquals(message, String(myDecrypted))
    }
}
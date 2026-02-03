package ir.amirroid.chatguard.integration

import ir.amirroid.chatguard.FileUtils
import ir.amirroid.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.amirroid.chatguard.core.crypto.implementation.EcdsaSignatureValidator
import ir.amirroid.chatguard.core.crypto.models.SignedPublicKey
import ir.amirroid.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.amirroid.chatguard.core.file.implementation.ByteArrayFileSource
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.amirroid.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SignedPublicKeySteganographyTest {

    private lateinit var keyManager: EcdhKeyManager
    private lateinit var signatureValidator: EcdsaSignatureValidator
    private lateinit var corpusProvider: CorpusProvider
    private lateinit var poeticEncoder: PoeticEncoder
    private lateinit var poeticDecoder: PoeticDecoder

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        signatureValidator = EcdsaSignatureValidator()

        val wordsBytes = FileUtils.getWordsBytes()

        corpusProvider = CachedCorpusProvider(ByteArrayFileSource(wordsBytes))
        poeticEncoder = WordBasedEncoder(corpusProvider)
        poeticDecoder = WordBasedDecoder(corpusProvider)
    }

    @Test
    fun `encode and decode signed public key as Persian text`() = runTest {
        corpusProvider.load().getOrThrow()

        val keyPair = keyManager.generateIdentityKeyPair().getOrThrow()
        val signature = signatureValidator.sign(
            keyPair.privateKey,
            keyPair.publicKey.encoded
        ).getOrThrow()

        val signedKey = SignedPublicKey(
            publicKey = keyPair.publicKey.encoded,
            signature = signature
        )

        val serialized = SignedPublicKeySerializer.serialize(signedKey)
        val poeticText = poeticEncoder.encode(serialized).getOrThrow()

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedKey = SignedPublicKeySerializer.deserialize(decodedBytes)

        val reconstructedPublicKey = keyManager.reconstructPublicKey(
            decodedKey.publicKey
        ).getOrThrow()

        val isValid = signatureValidator.verify(
            reconstructedPublicKey,
            decodedKey.publicKey,
            decodedKey.signature
        ).getOrThrow()

        assertTrue(isValid)
        assertArrayEquals(signedKey.publicKey, decodedKey.publicKey)
        assertArrayEquals(signedKey.signature, decodedKey.signature)
    }

    @Test
    fun `different keys produce different Persian texts`() = runTest {
        corpusProvider.load().getOrThrow()

        val keyPair1 = keyManager.generateIdentityKeyPair().getOrThrow()
        val signature1 = signatureValidator.sign(
            keyPair1.privateKey,
            keyPair1.publicKey.encoded
        ).getOrThrow()

        val keyPair2 = keyManager.generateIdentityKeyPair().getOrThrow()
        val signature2 = signatureValidator.sign(
            keyPair2.privateKey,
            keyPair2.publicKey.encoded
        ).getOrThrow()

        val signedKey1 = SignedPublicKey(keyPair1.publicKey.encoded, signature1)
        val signedKey2 = SignedPublicKey(keyPair2.publicKey.encoded, signature2)

        val poetic1 = poeticEncoder.encode(
            SignedPublicKeySerializer.serialize(signedKey1)
        ).getOrThrow()

        val poetic2 = poeticEncoder.encode(
            SignedPublicKeySerializer.serialize(signedKey2)
        ).getOrThrow()

        assertNotEquals(poetic1, poetic2)
    }

    @Test
    fun `verify fingerprint matches after decode`() = runTest {
        corpusProvider.load().getOrThrow()

        val keyPair = keyManager.generateIdentityKeyPair().getOrThrow()
        val signature = signatureValidator.sign(
            keyPair.privateKey,
            keyPair.publicKey.encoded
        ).getOrThrow()

        val originalFingerprint = keyManager.calculateFingerprint(keyPair.publicKey).getOrThrow()

        val signedKey = SignedPublicKey(
            publicKey = keyPair.publicKey.encoded,
            signature = signature
        )

        val serialized = SignedPublicKeySerializer.serialize(signedKey)
        val poeticText = poeticEncoder.encode(serialized).getOrThrow()

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedKey = SignedPublicKeySerializer.deserialize(decodedBytes)

        val reconstructedPublicKey = keyManager.reconstructPublicKey(
            decodedKey.publicKey
        ).getOrThrow()

        val decodedFingerprint =
            keyManager.calculateFingerprint(reconstructedPublicKey).getOrThrow()

        assertEquals(originalFingerprint, decodedFingerprint)
    }
}
package ir.amirroid.chatguard.domain.models.crypto

data class IdentityCryptoKeyPair(
    val privateKey: CryptoKey,
    val publicKey: CryptoKey,
)
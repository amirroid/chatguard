package ir.sysfail.chatguard.core.crypto.models

data class EphemeralKeyPair(
    val privateKey: PrivateKey,
    val publicKey: PublicKey
)
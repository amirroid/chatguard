package ir.sysfail.chatguard.core.crypto.models

data class IdentityKeyPair(
    val privateKey: PrivateKey,
    val publicKey: PublicKey
)

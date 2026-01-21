package ir.sysfail.chatguard.data.mappers.crypto

import ir.sysfail.chatguard.core.crypto.models.IdentityKeyPair
import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair

fun IdentityCryptoKeyPair.toIdentityKeyPair() = IdentityKeyPair(
    privateKey = privateKey.toPrivateKey(),
    publicKey = publicKey.toPublicKey(),
)

fun IdentityKeyPair.toIdentityCryptoKeyPair() = IdentityCryptoKeyPair(
    privateKey = privateKey.toCryptoKey(),
    publicKey = publicKey.toCryptoKey(),
)
package ir.amirroid.chatguard.data.mappers.crypto

import ir.amirroid.chatguard.core.crypto.models.IdentityKeyPair
import ir.amirroid.chatguard.domain.models.crypto.IdentityCryptoKeyPair

fun IdentityCryptoKeyPair.toIdentityKeyPair() = IdentityKeyPair(
    privateKey = privateKey.toPrivateKey(),
    publicKey = publicKey.toPublicKey(),
)

fun IdentityKeyPair.toIdentityCryptoKeyPair() = IdentityCryptoKeyPair(
    privateKey = privateKey.toCryptoKey(),
    publicKey = publicKey.toCryptoKey(),
)
package ir.amirroid.chatguard.data.mappers.crypto

import ir.amirroid.chatguard.core.database.entity.PublicKeyEntity
import ir.amirroid.chatguard.domain.models.crypto.CryptoKey

fun PublicKeyEntity.toCryptoKey() = CryptoKey(
    encoded = publicKey,
    algorithm = algorithm
)
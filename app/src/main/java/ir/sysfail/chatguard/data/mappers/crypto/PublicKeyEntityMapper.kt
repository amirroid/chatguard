package ir.sysfail.chatguard.data.mappers.crypto

import ir.sysfail.chatguard.core.database.entity.PublicKeyEntity
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey

fun PublicKeyEntity.toCryptoKey() = CryptoKey(
    encoded = publicKey,
    algorithm = algorithm
)
package ir.sysfail.chatguard.data.mappers.crypto

import ir.sysfail.chatguard.core.crypto.models.PrivateKey
import ir.sysfail.chatguard.core.crypto.models.PublicKey
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey

fun CryptoKey.toPublicKey() = PublicKey(encoded = encoded, algorithm = algorithm)
fun CryptoKey.toPrivateKey() = PrivateKey(encoded = encoded, algorithm = algorithm)


fun PrivateKey.toCryptoKey() = CryptoKey(encoded = encoded, algorithm = algorithm)
fun PublicKey.toCryptoKey() = CryptoKey(encoded = encoded, algorithm = algorithm)
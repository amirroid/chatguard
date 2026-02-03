package ir.amirroid.chatguard.data.mappers.crypto

import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey
import ir.amirroid.chatguard.domain.models.crypto.CryptoKey

fun CryptoKey.toPublicKey() = PublicKey(encoded = encoded, algorithm = algorithm)
fun CryptoKey.toPrivateKey() = PrivateKey(encoded = encoded, algorithm = algorithm)


fun PrivateKey.toCryptoKey() = CryptoKey(encoded = encoded, algorithm = algorithm)
fun PublicKey.toCryptoKey() = CryptoKey(encoded = encoded, algorithm = algorithm)
package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.core.crypto.abstraction.CipherEngine
import ir.amirroid.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.abstraction.KeyManager
import ir.amirroid.chatguard.core.crypto.abstraction.SharedSecretDeriver
import ir.amirroid.chatguard.core.crypto.abstraction.SignatureValidator
import ir.amirroid.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.amirroid.chatguard.core.crypto.implementation.DefaultCryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.amirroid.chatguard.core.crypto.implementation.EcdsaSignatureValidator
import ir.amirroid.chatguard.core.crypto.implementation.HkdfSecretDeriver
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val cryptoModule = module {
    factoryOf(::EcdhKeyManager).bind<KeyManager>()
    factoryOf(::AesGcmCipherEngine).bind<CipherEngine>()
    factoryOf(::HkdfSecretDeriver).bind<SharedSecretDeriver>()
    factoryOf(::EcdsaSignatureValidator).bind<SignatureValidator>()
    factoryOf(::DefaultCryptoOrchestrator).bind<CryptoOrchestrator>()
}
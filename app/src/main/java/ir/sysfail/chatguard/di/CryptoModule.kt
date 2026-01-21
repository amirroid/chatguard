package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.crypto.abstraction.CipherEngine
import ir.sysfail.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.sysfail.chatguard.core.crypto.abstraction.KeyManager
import ir.sysfail.chatguard.core.crypto.abstraction.SharedSecretDeriver
import ir.sysfail.chatguard.core.crypto.abstraction.SignatureValidator
import ir.sysfail.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.sysfail.chatguard.core.crypto.implementation.DefaultCryptoOrchestrator
import ir.sysfail.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.sysfail.chatguard.core.crypto.implementation.EcdsaSignatureValidator
import ir.sysfail.chatguard.core.crypto.implementation.HkdfSecretDeriver
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
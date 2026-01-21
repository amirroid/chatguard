package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.domain.usecase.crypto.CheckPublicKeyExistsUseCase
import ir.sysfail.chatguard.domain.usecase.crypto.GetOrGenerateIdentityKeyUseCase
import ir.sysfail.chatguard.domain.usecase.crypto.GetPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.AddUserPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.ExtractPoeticPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.GetPoeticSignedPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.PoeticMessageDecryptionUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.PoeticMessageEncryptionUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.VerifyPoeticPublicKeyUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::CheckPublicKeyExistsUseCase)
    factoryOf(::GetPublicKeyUseCase)
    factoryOf(::VerifyPoeticPublicKeyUseCase)
    factoryOf(::GetOrGenerateIdentityKeyUseCase)
    factoryOf(::GetPoeticSignedPublicKeyUseCase)
    factoryOf(::PoeticMessageEncryptionUseCase)
    factoryOf(::PoeticMessageDecryptionUseCase)
    factoryOf(::AddUserPublicKeyUseCase)
    factoryOf(::ExtractPoeticPublicKeyUseCase)
}
package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.domain.repository.SaveInternalIdentityKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.CheckIdentityKeyExistsUseCase
import ir.sysfail.chatguard.domain.usecase.key.CheckPublicKeyExistsUseCase
import ir.sysfail.chatguard.domain.usecase.key.GenerateIdentityKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.GetIdentityKeyPairFromFileUseCase
import ir.sysfail.chatguard.domain.usecase.key.GetIdentityKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.GetPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.AddUserPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.ClearCurrentKeysUseCase
import ir.sysfail.chatguard.domain.usecase.key.SaveIdentityKeysUseCase
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
    factoryOf(::GetIdentityKeyUseCase)
    factoryOf(::GetPoeticSignedPublicKeyUseCase)
    factoryOf(::PoeticMessageEncryptionUseCase)
    factoryOf(::PoeticMessageDecryptionUseCase)
    factoryOf(::AddUserPublicKeyUseCase)
    factoryOf(::ExtractPoeticPublicKeyUseCase)
    factoryOf(::CheckIdentityKeyExistsUseCase)
    factoryOf(::GenerateIdentityKeyUseCase)
    factoryOf(::GetIdentityKeyPairFromFileUseCase)
    factoryOf(::SaveInternalIdentityKeyUseCase)
    factoryOf(::ClearCurrentKeysUseCase)
    factoryOf(::SaveIdentityKeysUseCase)
}
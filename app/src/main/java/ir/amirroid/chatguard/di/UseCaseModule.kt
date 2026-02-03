package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.domain.repository.SaveInternalIdentityKeyUseCase
import ir.amirroid.chatguard.domain.usecase.key.CheckIdentityKeyExistsUseCase
import ir.amirroid.chatguard.domain.usecase.key.CheckPublicKeyExistsUseCase
import ir.amirroid.chatguard.domain.usecase.key.GenerateIdentityKeyUseCase
import ir.amirroid.chatguard.domain.usecase.key.GetIdentityKeyPairFromFileUseCase
import ir.amirroid.chatguard.domain.usecase.key.GetIdentityKeyUseCase
import ir.amirroid.chatguard.domain.usecase.key.GetPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.key.AddUserPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.key.ClearCurrentKeysUseCase
import ir.amirroid.chatguard.domain.usecase.key.SaveIdentityKeysUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.ExtractPoeticPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.GetPoeticSignedPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.PoeticMessageDecryptionUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.PoeticMessageEncryptionUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.VerifyPoeticPublicKeyUseCase
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
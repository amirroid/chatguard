package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.data.repository.CryptoRepositoryImpl
import ir.amirroid.chatguard.data.repository.DataStoreIdentityKeyRepository
import ir.amirroid.chatguard.data.repository.KeyRepositoryImpl
import ir.amirroid.chatguard.data.repository.SteganographyRepositoryImpl
import ir.amirroid.chatguard.data.repository.StorageRepositoryImpl
import ir.amirroid.chatguard.domain.repository.CryptoRepository
import ir.amirroid.chatguard.domain.repository.IdentityKeyRepository
import ir.amirroid.chatguard.domain.repository.KeyRepository
import ir.amirroid.chatguard.domain.repository.SteganographyRepository
import ir.amirroid.chatguard.domain.repository.StorageRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::DataStoreIdentityKeyRepository).bind<IdentityKeyRepository>()
    singleOf(::KeyRepositoryImpl).bind<KeyRepository>()
    singleOf(::CryptoRepositoryImpl).bind<CryptoRepository>()
    singleOf(::SteganographyRepositoryImpl).bind<SteganographyRepository>()
    singleOf(::StorageRepositoryImpl).bind<StorageRepository>()
}
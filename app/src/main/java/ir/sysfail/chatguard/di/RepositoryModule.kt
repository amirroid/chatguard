package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.data.repository.CryptoRepositoryImpl
import ir.sysfail.chatguard.data.repository.DataStoreIdentityKeyRepository
import ir.sysfail.chatguard.data.repository.KeyRepositoryImpl
import ir.sysfail.chatguard.data.repository.SteganographyRepositoryImpl
import ir.sysfail.chatguard.data.repository.StorageRepositoryImpl
import ir.sysfail.chatguard.domain.repository.CryptoRepository
import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository
import ir.sysfail.chatguard.domain.repository.KeyRepository
import ir.sysfail.chatguard.domain.repository.SteganographyRepository
import ir.sysfail.chatguard.domain.repository.StorageRepository
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
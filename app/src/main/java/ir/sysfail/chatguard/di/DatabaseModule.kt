package ir.sysfail.chatguard.di

import androidx.room.Room
import ir.sysfail.chatguard.core.database.AppDatabase
import ir.sysfail.chatguard.utils.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = Constants.DATABASE_FILE_NAME
        ).build()
    }
    single { get<AppDatabase>().publicDao() }
}
package ir.sysfail.chatguard.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.sysfail.chatguard.core.database.dao.PublicKeyDao
import ir.sysfail.chatguard.core.database.entity.PublicKeyEntity
import ir.sysfail.chatguard.utils.Constants

@Database(
    entities = [PublicKeyEntity::class],
    version = Constants.DATABASE_VERSION
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun publicDao(): PublicKeyDao
}
package ir.amirroid.chatguard.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.amirroid.chatguard.core.database.dao.PublicKeyDao
import ir.amirroid.chatguard.core.database.entity.PublicKeyEntity
import ir.amirroid.chatguard.utils.Constants

@Database(
    entities = [PublicKeyEntity::class],
    version = Constants.DATABASE_VERSION
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun publicDao(): PublicKeyDao
}
package ir.sysfail.chatguard.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.sysfail.chatguard.core.database.entity.PublicKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicKeyDao {

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM PublicKeyEntity
            WHERE appPackageName = :packageName
            AND username = :username
        )
    """
    )
    fun keyExists(
        packageName: String,
        username: String
    ): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(
        keyEntity: PublicKeyEntity
    )

    @Query(
        """
        SELECT * FROM PublicKeyEntity
        WHERE appPackageName = :packageName
        AND username = :name
        LIMIT 1
    """
    )
    suspend fun getKey(
        name: String,
        packageName: String
    ): PublicKeyEntity?
}
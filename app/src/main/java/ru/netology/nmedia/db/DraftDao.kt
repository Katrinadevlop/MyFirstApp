package ru.netology.nmedia.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Query("SELECT * FROM draft WHERE id = 0")
    fun get(): Flow<DraftEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: DraftEntity)

    @Query("DELETE FROM draft WHERE id = 0")
    suspend fun clear()
}

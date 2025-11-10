package ru.netology.nmedia.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DraftDao {
    @Query("SELECT * FROM draft WHERE id = 0")
    fun get(): LiveData<DraftEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: DraftEntity)

    @Query("DELETE FROM draft WHERE id = 0")
    suspend fun clear()
}

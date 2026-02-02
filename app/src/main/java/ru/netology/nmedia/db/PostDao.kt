package ru.netology.nmedia.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int

    @Query("SELECT MAX(id) FROM posts")
    suspend fun getMaxId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entities: List<PostEntity>)

    @Update
    suspend fun update(entity: PostEntity)

    @Query("UPDATE posts SET content = :content WHERE id = :id")
    suspend fun updateContentById(id: Long, content: String)

    @Query("UPDATE posts SET likedByMe = NOT likedByMe, likes = CASE WHEN likedByMe THEN likes - 1 ELSE likes + 1 END WHERE id = :id")
    suspend fun likeById(id: Long)

    @Query("UPDATE posts SET shares = shares + 1 WHERE id = :id")
    suspend fun shareById(id: Long)

    @Query("UPDATE posts SET views = views + 1 WHERE id = :id")
    suspend fun viewById(id: Long)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun removeById(id: Long)
    
    // Для задания №2: получение несинхронизированных постов
    @Query("SELECT * FROM posts WHERE isSynced = 0")
    suspend fun getUnsyncedPosts(): List<PostEntity>
}

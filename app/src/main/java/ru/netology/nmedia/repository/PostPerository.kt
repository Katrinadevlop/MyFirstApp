package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun get(): LiveData<List<Post>>
    fun share(id: Long)
    fun view(id: Long)
    
    // Suspend-методы
    suspend fun likeById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun addSuspend(post: Post)
    suspend fun updateContentByIdSuspend(id: Long, content: String)
    suspend fun refreshSuspend()
    suspend fun retrySyncUnsavedPostsSuspend()
    
    // Методы для задания №1: загрузка новых постов
    suspend fun getNewer(currentMaxId: Long): List<Post>
    suspend fun getMaxPostId(): Long
    suspend fun saveNewerPosts(posts: List<Post>)
}

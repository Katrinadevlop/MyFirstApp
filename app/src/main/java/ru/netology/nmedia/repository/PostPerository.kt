package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun get(): LiveData<List<Post>>
    fun like(id: Long, onError: (Exception) -> Unit = {})
    fun share(id: Long)
    fun view(id: Long)
    fun remove(id: Long, onError: (Exception) -> Unit = {})
    fun add(post: Post, onError: (Exception) -> Unit = {})
    fun updateContentById(id: Long, content: String, onError: (Exception) -> Unit = {})
    fun refresh(onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {})
    
    // Новые suspend-методы для задания
    suspend fun likeById(id: Long)
    suspend fun removeById(id: Long)
    
    // Метод для задания №2: повторная попытка синхронизации
    fun retrySyncUnsavedPosts(onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {})
    
    // Методы для задания №1: загрузка новых постов
    suspend fun getNewer(currentMaxId: Long): List<Post>
    suspend fun getMaxPostId(): Long
    suspend fun saveNewerPosts(posts: List<Post>)
}

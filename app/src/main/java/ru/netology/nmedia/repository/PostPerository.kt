package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: Flow<List<Post>>
    
    suspend fun likeById(id: Long)
    suspend fun removeById(id: Long)
    suspend fun add(post: Post)
    suspend fun updateContentById(id: Long, content: String)
    suspend fun refresh()
    suspend fun retrySyncUnsavedPosts()
    suspend fun getUnsyncedPostsCount(): Int
    suspend fun share(id: Long)
    suspend fun view(id: Long)
    
    // Методы для загрузки новых постов
    suspend fun getNewer(currentMaxId: Long): List<Post>
    suspend fun getMaxPostId(): Long
    suspend fun saveNewerPosts(posts: List<Post>)
}

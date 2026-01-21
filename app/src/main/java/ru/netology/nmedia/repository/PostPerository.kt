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
}

package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun get(): LiveData<List<Post>>
    fun like(id: Long)
    fun share(id: Long)
    fun view(id: Long)
    fun remove(id: Long)
    fun add(post: Post)
    fun updateContentById(id: Long, content: String)
}

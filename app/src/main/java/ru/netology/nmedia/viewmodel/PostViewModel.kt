package ru.netology.nmedia.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryInMemoryImpl

class PostViewModel : ViewModel() {
    private val repository: PostRepository = PostRepositoryInMemoryImpl()
    private val empty = Post(
        id = 0,
        author = "",
        content = "",
        published = "",
        likes = 0,
        likedByMe = false,
        shares = 0,
        views = 0,
        video = null,
    )
    val edited = MutableLiveData(empty)
    val data = repository.get()

    fun edit(post: Post) {
        edited.value = post
    }

    // Legacy save when inline editing is used
    fun add(content: String) {
        val text = content.trim()
        val current = edited.value ?: empty
        if (current.id == 0L) {
            repository.add(current.copy(content = text))
        } else {
            if (current.content != text) repository.updateContentById(current.id, text)
        }
        edited.value = empty
    }

    // New helpers for external editor Activity
    fun create(content: String) {
        repository.add(Post(id = 0, author = "", content = content.trim(), published = ""))
    }

    fun update(id: Long, content: String) {
        repository.updateContentById(id, content.trim())
    }

    fun like(id: Long) = repository.like(id)
    fun share(id: Long) = repository.share(id)
    fun view(id: Long) = repository.view(id)
    fun remove(id: Long) = repository.remove(id)

    fun cancelEdit() {
        edited.value = empty
    }
}

package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryFileImpl

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryFileImpl(application)
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

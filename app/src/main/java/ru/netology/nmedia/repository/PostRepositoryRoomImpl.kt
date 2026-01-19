package ru.netology.nmedia.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.db.PostEntity
import ru.netology.nmedia.dto.Post

class PostRepositoryRoomImpl(application: Application) : PostRepository {
    private val db = AppDb.get(application)
    private val dao = db.postDao()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun get(): LiveData<List<Post>> = dao.getAll().map { list -> list.map { it.toDto() } }

    override fun like(id: Long) {
        ioScope.launch { dao.likeById(id) }
    }

    override fun share(id: Long) {
        ioScope.launch { dao.shareById(id) }
    }

    override fun view(id: Long) {
        ioScope.launch { dao.viewById(id) }
    }

    override fun remove(id: Long) {
        ioScope.launch { dao.removeById(id) }
    }

    override fun add(post: Post) {
        val entity = PostEntity(
            id = 0,
            author = if (post.author.isBlank()) "My" else post.author,
            content = post.content,
            published = if (post.published.isBlank()) "now" else post.published,
            likes = 0,
            likedByMe = false,
            shares = 0,
            views = 0,
            video = post.video
        )
        ioScope.launch { dao.insert(entity) }
    }

    override fun updateContentById(id: Long, content: String) {
        ioScope.launch { dao.updateContentById(id, content) }
    }

    override fun refresh(callback: () -> Unit) {
        // Room implementation doesn't need network refresh
        callback()
    }
}

package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import kotlin.collections.map

class PostRepositoryInMemoryImpl : PostRepository {
    private var posts = List(5) {
        Post(
            id = it + 1L,
            author = "Нетология. Университет интернет-профессий будущего",
            content = "Привет, эта новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия помочь встать на путь роста и начать цепочку перемен http://netolo.gy/fyb",
            published = "21 мая в 18:36",
            likes = 100,
            likedByMe = false,
            shares = 10,
            views = 99,
        )
    }
    private var nextId: Long = (posts.maxOfOrNull { it.id } ?: 0L) + 1L
    private val data = MutableLiveData(posts)

    override fun get(): LiveData<List<Post>> = data

    override fun like(id: Long) {
        posts = posts.map {
            if (it.id != id)
                it
            else {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likes = if (it.likedByMe) it.likes - 1 else it.likes + 1,
                )
            }
        }
        data.value = posts
    }

    override fun share(id: Long) {
        posts = posts.map {
            if (it.id != id)
                it
            else
                it.copy(shares = it.shares + 1)
        }
        data.value = posts
    }

    override fun view(id: Long) {
        posts = posts.map {
            if (it.id != id)
                it
            else
                it.copy(views = it.views + 1)
        }
        data.value = posts
    }

    override fun remove(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    override fun add(post: Post) {
        val newPost = post.copy(
            id = nextId++,
            author =  "My",
            likedByMe = false,
            likes = 0,
            shares = 0,
            views = 0,
            published = "now",
        )
        posts = listOf(newPost) + posts
        data.value = posts
    }

    override fun updateContentById(id: Long, content: String) {
        posts = posts.map { if (it.id != id) it else it.copy(content = content) }
        data.value = posts
    }
}


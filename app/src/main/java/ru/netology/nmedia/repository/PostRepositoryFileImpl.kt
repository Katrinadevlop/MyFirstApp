package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post
import java.io.File

class PostRepositoryFileImpl(
    context: Context,
) : PostRepository {
    private val gson = Gson()
    private val file: File = File(context.filesDir, FILE_NAME)

    private var posts: List<Post>
    private var nextId: Long
    private val data: MutableLiveData<List<Post>>

    init {
        val loaded: List<Post>? = runCatching {
            if (file.exists()) {
                file.bufferedReader().use { reader ->
                    val type = object : TypeToken<List<Post>>() {}.type
                    gson.fromJson<List<Post>>(reader, type)
                }
            } else null
        }.getOrNull()

        posts = loaded ?: defaultPosts()
        nextId = (posts.maxOfOrNull { it.id } ?: 0L) + 1L
        data = MutableLiveData(posts)

        sync()
    }

    override fun get(): LiveData<List<Post>> = data

    override fun like(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(
                likedByMe = !it.likedByMe,
                likes = if (it.likedByMe) it.likes - 1 else it.likes + 1,
            )
        }
        data.value = posts
        sync()
    }

    override fun share(id: Long) {
        posts = posts.map { if (it.id != id) it else it.copy(shares = it.shares + 1) }
        data.value = posts
        sync()
    }

    override fun view(id: Long) {
        posts = posts.map { if (it.id != id) it else it.copy(views = it.views + 1) }
        data.value = posts
        sync()
    }

    override fun remove(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
        sync()
    }

    override fun add(post: Post) {
        val newPost = post.copy(
            id = nextId++,
            author = "My",
            likedByMe = false,
            likes = 0,
            shares = 0,
            views = 0,
            published = "now",
        )
        posts = listOf(newPost) + posts
        data.value = posts
        sync()
    }

    override fun updateContentById(id: Long, content: String) {
        posts = posts.map { if (it.id != id) it else it.copy(content = content) }
        data.value = posts
        sync()
    }

    private fun sync() {
        runCatching {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                writer.write(gson.toJson(posts))
            }
        }
    }

    private fun defaultPosts(): List<Post> = List(5) {
        Post(
            id = it + 1L,
            author = "Нетология. Университет интернет-профессий будущего",
            content = "Привет, эта новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия помочь встать на путь роста и начать цепочку перемен http://netolo.gy/fyb",
            published = "21 мая в 18:36",
            likes = 100,
            likedByMe = false,
            shares = 10,
            views = 99,
            video = if (it == 0) "https://rutube.ru/video/6550a91e7e523f9503bed47e4c46d0cb" else null,
        )
    }

    companion object {
        private const val FILE_NAME = "posts.json"
    }
}
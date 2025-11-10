package ru.netology.nmedia

import android.app.Application
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.db.PostEntity
import ru.netology.nmedia.dto.Post
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val db = AppDb.get(this)
        val dao = db.postDao()

        CoroutineScope(Dispatchers.IO).launch {
            if (dao.count() == 0) {
                val initial = loadFromFile() ?: defaultPosts()
                if (initial.isNotEmpty()) {
                    dao.insert(initial.map(PostEntity.Companion::fromDto))
                }
            }
        }
    }

    private fun loadFromFile(): List<Post>? = runCatching {
        val file = File(filesDir, FILE_NAME)
        if (!file.exists()) return null
        file.bufferedReader().use { reader ->
            val type = object : TypeToken<List<Post>>() {}.type
            Gson().fromJson<List<Post>>(reader, type)
        }
    }.getOrNull()

    private fun defaultPosts(): List<Post> = List(5) {
        Post(
            id = 0,
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

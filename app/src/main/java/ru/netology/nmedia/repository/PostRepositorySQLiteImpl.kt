package ru.netology.nmedia.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.db.DbHelper
import ru.netology.nmedia.dto.Post

class PostRepositorySQLiteImpl(application: Application) : PostRepository {
    private val db = DbHelper(application).writableDatabase
    private val data = MutableLiveData<List<Post>>(emptyList())

    init {
        data.value = queryAll()
    }

    override fun get(): LiveData<List<Post>> = data

    override fun like(id: Long) {
        db.execSQL(
            """
            UPDATE posts
            SET likedByMe = CASE likedByMe WHEN 0 THEN 1 ELSE 0 END,
                likes = CASE likedByMe WHEN 0 THEN likes + 1 ELSE likes - 1 END
            WHERE id = ?
            """.trimIndent(),
            arrayOf(id)
        )
        data.value = queryAll()
    }

    override fun share(id: Long) {
        db.execSQL("UPDATE posts SET shares = shares + 1 WHERE id = ?", arrayOf(id))
        data.value = queryAll()
    }

    override fun view(id: Long) {
        db.execSQL("UPDATE posts SET views = views + 1 WHERE id = ?", arrayOf(id))
        data.value = queryAll()
    }

    override fun remove(id: Long) {
        db.delete("posts", "id = ?", arrayOf(id.toString()))
        data.value = queryAll()
    }

    override fun add(post: Post) {
        val values = ContentValues().apply {
            put("author", if (post.author.isBlank()) "My" else post.author)
            put("content", post.content)
            put("published", if (post.published.isBlank()) "now" else post.published)
            put("likes", 0)
            put("likedByMe", 0)
            put("shares", 0)
            put("views", 0)
            put("video", post.video)
        }
        db.insert("posts", null, values)
        data.value = queryAll()
    }

    override fun updateContentById(id: Long, content: String) {
        val values = ContentValues().apply { put("content", content) }
        db.update("posts", values, "id = ?", arrayOf(id.toString()))
        data.value = queryAll()
    }

    private fun queryAll(): List<Post> {
        val cursor = db.query(
            "posts",
            arrayOf("id","author","content","published","likes","likedByMe","shares","views","video"),
            null, null, null, null,
            "id DESC"
        )
        return cursor.use { c -> generateSequence { if (c.moveToNext()) c else null }
            .map { it.toPost() }
            .toList() }
    }

    private fun Cursor.toPost(): Post = Post(
        id = getLong(getColumnIndexOrThrow("id")),
        author = getString(getColumnIndexOrThrow("author")),
        content = getString(getColumnIndexOrThrow("content")),
        published = getString(getColumnIndexOrThrow("published")),
        likes = getInt(getColumnIndexOrThrow("likes")),
        likedByMe = getInt(getColumnIndexOrThrow("likedByMe")) != 0,
        shares = getInt(getColumnIndexOrThrow("shares")),
        views = getInt(getColumnIndexOrThrow("views")),
        video = getString(getColumnIndexOrThrow("video"))
    )
}

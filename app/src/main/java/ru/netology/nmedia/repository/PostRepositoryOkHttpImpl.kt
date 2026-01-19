package ru.netology.nmedia.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.db.PostEntity
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepositoryOkHttpImpl(application: Application) : PostRepository {
    private val db = AppDb.get(application)
    private val dao = db.postDao()
    private val gson = Gson()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "http://10.0.2.2:9999"
    private val mediaType = "application/json".toMediaType()
    
    private val _data = MediatorLiveData<List<Post>>()

    init {
        val roomData = dao.getAll()
        _data.addSource(roomData) { entities ->
            _data.value = entities.map { it.toDto() }
        }
        refresh()
    }

    override fun get(): LiveData<List<Post>> = _data

    override fun like(id: Long) {
        // Сначала обновляем локально для быстрого отклика
        ioScope.launch {
            try {
                dao.likeById(id)
                val entity = dao.getById(id)
                if (entity != null) {
                    val post = entity.toDto()
                    
                    // Определяем метод в зависимости от состояния лайка
                    val url = "$baseUrl/api/posts/$id/likes"
                    val emptyBody = "".toRequestBody(mediaType)
                    
                    val request = Request.Builder()
                        .url(url)
                        .apply {
                            if (post.likedByMe) {
                                post(emptyBody)
                            } else {
                                delete()
                            }
                        }
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e(TAG, "Ошибка при лайке", e)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                response.body?.string()?.let { json ->
                                    val updatedPost = gson.fromJson(json, Post::class.java)
                                    ioScope.launch {
                                        dao.insert(PostEntity.fromDto(updatedPost))
                                    }
                                }
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при лайке", e)
            }
        }
    }

    override fun share(id: Long) {
        ioScope.launch {
            dao.shareById(id)
        }
    }

    override fun view(id: Long) {
        ioScope.launch {
            dao.viewById(id)
        }
    }

    override fun remove(id: Long) {
        ioScope.launch {
            try {
                dao.removeById(id)
                
                val request = Request.Builder()
                    .url("$baseUrl/api/posts/$id")
                    .delete()
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Ошибка при удалении с сервера", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "Пост $id успешно удален с сервера")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении", e)
            }
        }
    }

    override fun add(post: Post) {
        val json = gson.toJson(post)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$baseUrl/api/posts")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Ошибка при добавлении, сохраняем только локально", e)
                ioScope.launch {
                    val entity = PostEntity(
                        id = 0,
                        author = if (post.author.isBlank()) "Я" else post.author,
                        content = post.content,
                        published = if (post.published.isBlank()) "только что" else post.published,
                        likes = post.likes,
                        likedByMe = post.likedByMe,
                        shares = post.shares,
                        views = post.views,
                        video = post.video
                    )
                    dao.insert(entity)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { json ->
                        val savedPost = gson.fromJson(json, Post::class.java)
                        ioScope.launch {
                            dao.insert(PostEntity.fromDto(savedPost))
                        }
                    }
                }
            }
        })
    }

    override fun updateContentById(id: Long, content: String) {
        ioScope.launch {
            try {
                dao.updateContentById(id, content)
                
                val entity = dao.getById(id)
                if (entity != null) {
                    val post = entity.toDto()
                    val json = gson.toJson(post)
                    val body = json.toRequestBody(mediaType)
                    
                    val request = Request.Builder()
                        .url("$baseUrl/api/posts")
                        .post(body)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e(TAG, "Ошибка при обновлении на сервере", e)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                response.body?.string()?.let { json ->
                                    val updatedPost = gson.fromJson(json, Post::class.java)
                                    ioScope.launch {
                                        dao.insert(PostEntity.fromDto(updatedPost))
                                    }
                                }
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении", e)
            }
        }
    }

    override fun refresh(callback: () -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/api/posts")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Ошибка загрузки с сервера, используем локальные данные", e)
                callback()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        response.body?.string()?.let { json ->
                            val type = object : TypeToken<List<Post>>() {}.type
                            val posts: List<Post> = gson.fromJson(json, type)
                            Log.d(TAG, "Получено ${posts.size} постов с сервера")
                            
                            ioScope.launch {
                                posts.forEach { post ->
                                    dao.insert(PostEntity.fromDto(post))
                                }
                            }
                        }
                    }
                } finally {
                    callback()
                }
            }
        })
    }

    companion object {
        private const val TAG = "PostRepositoryOkHttp"
    }
}

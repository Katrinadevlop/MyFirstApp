package ru.netology.nmedia.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.RetrofitClient
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.db.PostEntity
import ru.netology.nmedia.dto.Post

class PostRepositoryHybridImpl(application: Application) : PostRepository {
    private val db = AppDb.get(application)
    private val dao = db.postDao()
    private val apiService = RetrofitClient.postApiService
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Используем MediatorLiveData для объединения данных из Room и сети
    private val _data = MediatorLiveData<List<Post>>()

    init {
        // Наблюдаем за изменениями в Room
        val roomData = dao.getAll()
        _data.addSource(roomData) { entities ->
            _data.value = entities.map { it.toDto() }
        }

        // Загружаем данные с сервера при инициализации
        refresh()
    }

    override fun get(): LiveData<List<Post>> = _data

    override fun like(id: Long) {
        ioScope.launch {
            try {
                // Сначала обновляем локально для быстрого отклика
                dao.likeById(id)

                // Получаем текущий пост из БД
                val entity = dao.getById(id)
                if (entity != null) {
                    val post = entity.toDto()
                    
                    // Отправляем запрос на сервер
                    val response = if (post.likedByMe) {
                        apiService.likeById(id)
                    } else {
                        apiService.unlikeById(id)
                    }

                    if (response.isSuccessful) {
                        val updatedPost = response.body()
                        if (updatedPost != null) {
                            // Обновляем БД данными с сервера
                            dao.insert(PostEntity.fromDto(updatedPost))
                        }
                    }
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
                // Удаляем локально
                dao.removeById(id)

                // Удаляем на сервере
                apiService.removeById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении", e)
            }
        }
    }

    override fun add(post: Post) {
        ioScope.launch {
            try {
                // Сохраняем на сервере
                val response = apiService.save(post)
                if (response.isSuccessful) {
                    val savedPost = response.body()
                    if (savedPost != null) {
                        // Сохраняем в БД с ID от сервера
                        dao.insert(PostEntity.fromDto(savedPost))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении, сохраняем только локально", e)
                // Если сервер недоступен, сохраняем только локально
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
    }

    override fun updateContentById(id: Long, content: String) {
        ioScope.launch {
            try {
                // Обновляем локально
                dao.updateContentById(id, content)

                // Получаем обновленный пост
                val entity = dao.getById(id)
                if (entity != null) {
                    val post = entity.toDto()
                    
                    // Отправляем на сервер
                    val response = apiService.save(post)
                    if (response.isSuccessful) {
                        val updatedPost = response.body()
                        if (updatedPost != null) {
                            dao.insert(PostEntity.fromDto(updatedPost))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении", e)
            }
        }
    }

    override fun refresh(callback: () -> Unit) {
        ioScope.launch {
            try {
                Log.d(TAG, "Загрузка постов с сервера...")
                val response = apiService.getAll()
                
                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    Log.d(TAG, "Получено ${posts.size} постов с сервера")
                    
                    // Сохраняем все посты с сервера в БД
                    posts.forEach { post ->
                        dao.insert(PostEntity.fromDto(post))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки с сервера, используем локальные данные", e)
            } finally {
                callback()
            }
        }
    }

    companion object {
        private const val TAG = "PostRepositoryHybrid"
    }
}

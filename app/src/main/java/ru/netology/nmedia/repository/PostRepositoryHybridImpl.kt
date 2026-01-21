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
        // Не вызываем refresh в init, так как ViewModel сам вызовет loadPosts()
    }

    override fun get(): LiveData<List<Post>> = _data

    override fun like(id: Long, onError: (Exception) -> Unit) {
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
                    } else {
                        dao.likeById(id) // Откатываем
                        onError(RuntimeException("Ошибка сервера: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при лайке", e)
                onError(e)
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

    override fun remove(id: Long, onError: (Exception) -> Unit) {
        ioScope.launch {
            try {
                val entity = dao.getById(id)
                // Удаляем локально
                dao.removeById(id)

                // Удаляем на сервере
                val response = apiService.removeById(id)
                if (!response.isSuccessful) {
                    if (entity != null) dao.insert(entity)
                    onError(RuntimeException("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении", e)
                onError(e)
            }
        }
    }

    override fun add(post: Post, onError: (Exception) -> Unit) {
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
                } else {
                    onError(RuntimeException("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении", e)
                onError(e)
            }
        }
    }

    override fun updateContentById(id: Long, content: String, onError: (Exception) -> Unit) {
        ioScope.launch {
            try {
                val oldEntity = dao.getById(id)
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
                    } else {
                        if (oldEntity != null) dao.insert(oldEntity)
                        onError(RuntimeException("Ошибка сервера: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении", e)
                onError(e)
            }
        }
    }

    override fun refresh(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
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
                    onSuccess()
                } else {
                    onError(RuntimeException("Ошибка сервера: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки с сервера", e)
                onError(e)
            }
        }
    }

    companion object {
        private const val TAG = "PostRepositoryHybrid"
    }
}

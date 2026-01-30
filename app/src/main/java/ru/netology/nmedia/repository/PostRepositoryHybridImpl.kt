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
                // Задание №2: Оптимистичное сохранение
                // 1. Сначала сохраняем в локальную БД с временным ID
                val localId = System.currentTimeMillis() // Используем timestamp как уникальный локальный ID
                val localPost = post.copy(
                    id = 0, // Room сам присвоит autoGenerate ID
                    isSynced = false,
                    localId = localId
                )
                val generatedId = dao.insert(PostEntity.fromDto(localPost))

                try {
                    // 2. Затем отправляем на сервер
                    val response = apiService.save(post)
                    if (response.isSuccessful) {
                        val savedPost = response.body()
                        if (savedPost != null) {
                            // 3. Удаляем локальную версию и сохраняем с ID от сервера
                            dao.removeById(generatedId)
                            dao.insert(PostEntity.fromDto(savedPost.copy(
                                isSynced = true,
                                localId = null
                            )))
                        }
                    } else {
                        // Оставляем пост с флагом isSynced = false
                        onError(RuntimeException("Ошибка сервера: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    // Оставляем пост с флагом isSynced = false
                    Log.e(TAG, "Ошибка при сохранении на сервере", e)
                    onError(e)
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

    // Новые suspend-методы для задания №1
    override suspend fun likeById(id: Long) {
        try {
            // 1. Сначала модифицируем запись в локальной БД для быстрого отклика
            val entityBefore = dao.getById(id)
            dao.likeById(id)

            // Получаем обновленный пост из БД
            val entity = dao.getById(id)
            if (entity != null) {
                val post = entity.toDto()
                
                try {
                    // 2. Затем отправляем запрос в API
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
                        // Откатываем изменения в БД при ошибке
                        if (entityBefore != null) {
                            dao.insert(entityBefore)
                        }
                        throw RuntimeException("Ошибка сервера: ${response.code()}")
                    }
                } catch (e: Exception) {
                    // Откатываем изменения в БД при ошибке сети
                    if (entityBefore != null) {
                        dao.insert(entityBefore)
                    }
                    Log.e(TAG, "Ошибка при лайке (API)", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при лайке", e)
            throw e
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            // 1. Сначала сохраняем пост и удаляем из локальной БД
            val entity = dao.getById(id)
            dao.removeById(id)

            try {
                // 2. Затем отправляем запрос на удаление в API
                val response = apiService.removeById(id)
                if (!response.isSuccessful) {
                    // Восстанавливаем пост в БД при ошибке
                    if (entity != null) {
                        dao.insert(entity)
                    }
                    throw RuntimeException("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                // Восстанавливаем пост в БД при ошибке сети
                if (entity != null) {
                    dao.insert(entity)
                }
                Log.e(TAG, "Ошибка при удалении (API)", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении", e)
            throw e
        }
    }

    // Метод для задания №2: повторная попытка синхронизации несохранённых постов
    override fun retrySyncUnsavedPosts(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        ioScope.launch {
            try {
                val unsyncedPosts = dao.getUnsyncedPosts()
                Log.d(TAG, "Найдено ${unsyncedPosts.size} несинхронизированных постов")
                
                var allSuccess = true
                for (entity in unsyncedPosts) {
                    try {
                        val post = entity.toDto()
                        // Отправляем пост на сервер (без isSynced и localId)
                        val postToSave = post.copy(
                            id = 0,
                            isSynced = true,
                            localId = null
                        )
                        val response = apiService.save(postToSave)
                        
                        if (response.isSuccessful) {
                            val savedPost = response.body()
                            if (savedPost != null) {
                                // Удаляем локальную версию и сохраняем с ID от сервера
                                dao.removeById(entity.id)
                                dao.insert(PostEntity.fromDto(savedPost.copy(
                                    isSynced = true,
                                    localId = null
                                )))
                                Log.d(TAG, "Пост ${entity.id} успешно синхронизирован")
                            }
                        } else {
                            allSuccess = false
                            Log.e(TAG, "Ошибка синхронизации поста ${entity.id}: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        allSuccess = false
                        Log.e(TAG, "Ошибка при синхронизации поста ${entity.id}", e)
                    }
                }
                
                if (allSuccess) {
                    onSuccess()
                } else {
                    onError(RuntimeException("Не все посты удалось синхронизировать"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при повторной синхронизации", e)
                onError(e)
            }
        }
    }

    companion object {
        private const val TAG = "PostRepositoryHybrid"
    }
}

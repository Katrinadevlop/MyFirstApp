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

    override suspend fun likeById(id: Long) {
        // Не разрешаем лайкать несинхронизированные посты
        val entity = dao.getById(id)
        if (entity != null && !entity.isSynced) {
            throw IllegalStateException("Нельзя лайкать несинхронизированный пост")
        }
        
        try {
            val entityBefore = dao.getById(id)
            dao.likeById(id)

            val updatedEntity = dao.getById(id)
            if (updatedEntity != null) {
                val post = updatedEntity.toDto()
                
                try {
                    val response = if (post.likedByMe) {
                        apiService.likeById(id)
                    } else {
                        apiService.unlikeById(id)
                    }

                    if (response.isSuccessful) {
                        val updatedPost = response.body()
                        if (updatedPost != null) {
                            dao.insert(PostEntity.fromDto(updatedPost))
                        }
                    } else {
                        if (entityBefore != null) {
                            dao.insert(entityBefore)
                        }
                        throw RuntimeException("Ошибка сервера: ${response.code()}")
                    }
                } catch (e: Exception) {
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
        // Не разрешаем удалять несинхронизированные посты (они и так не на сервере)
        val entity = dao.getById(id)
        if (entity != null && !entity.isSynced) {
            // Просто удаляем локальный пост
            dao.removeById(id)
            return
        }
        
        try {
            val savedEntity = dao.getById(id)
            dao.removeById(id)

            try {
                val response = apiService.removeById(id)
                if (!response.isSuccessful) {
                    if (savedEntity != null) {
                        dao.insert(savedEntity)
                    }
                    throw RuntimeException("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                if (savedEntity != null) {
                    dao.insert(savedEntity)
                }
                Log.e(TAG, "Ошибка при удалении (API)", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении", e)
            throw e
        }
    }

    // Suspend-методы
    override suspend fun addSuspend(post: Post) {
        try {
            // Задание №2: Оптимистичное сохранение
            // Используем отрицательный ID для локальных постов
            val localId = -(System.currentTimeMillis())
            val localPost = post.copy(
                id = localId,
                isSynced = false,
                localId = localId
            )
            dao.insert(PostEntity.fromDto(localPost))

            try {
                // Отправляем на сервер
                val response = apiService.save(post)
                if (response.isSuccessful) {
                    val savedPost = response.body()
                    if (savedPost != null) {
                        // Удаляем локальную версию и сохраняем с ID от сервера
                        dao.removeById(localId)
                        dao.insert(PostEntity.fromDto(savedPost.copy(
                            isSynced = true,
                            localId = null
                        )))
                    }
                } else {
                    Log.e(TAG, "Ошибка сервера при сохранении: ${response.code()}")
                    throw RuntimeException("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                // Оставляем пост с флагом isSynced = false
                Log.e(TAG, "Ошибка при сохранении на сервере", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении", e)
            throw e
        }
    }

    override suspend fun updateContentByIdSuspend(id: Long, content: String) {
        // Не разрешаем редактирование несинхронизированных постов
        val entity = dao.getById(id)
        if (entity != null && !entity.isSynced) {
            throw IllegalStateException("Нельзя редактировать несинхронизированный пост")
        }
        
        try {
            val oldEntity = dao.getById(id)
            dao.updateContentById(id, content)

            val updatedEntity = dao.getById(id)
            if (updatedEntity != null) {
                val post = updatedEntity.toDto()
                
                val response = apiService.save(post)
                if (response.isSuccessful) {
                    val updatedPost = response.body()
                    if (updatedPost != null) {
                        dao.insert(PostEntity.fromDto(updatedPost))
                    }
                } else {
                    if (oldEntity != null) dao.insert(oldEntity)
                    throw RuntimeException("Ошибка сервера: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении", e)
            throw e
        }
    }

    override suspend fun refreshSuspend() {
        try {
            Log.d(TAG, "Загрузка постов с сервера...")
            val response = apiService.getAll()
            
            if (response.isSuccessful) {
                val posts = response.body() ?: emptyList()
                Log.d(TAG, "Получено ${posts.size} постов с сервера")
                
                posts.forEach { post ->
                    dao.insert(PostEntity.fromDto(post))
                }
            } else {
                throw RuntimeException("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки с сервера", e)
            throw e
        }
    }

    override suspend fun retrySyncUnsavedPostsSuspend() {
        try {
            val unsyncedPosts = dao.getUnsyncedPosts()
            Log.d(TAG, "Найдено ${unsyncedPosts.size} несинхронизированных постов")
            
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
                        Log.e(TAG, "Ошибка синхронизации поста ${entity.id}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при синхронизации поста ${entity.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при повторной синхронизации", e)
            throw e
        }
    }

    // Методы для задания №1: загрузка новых постов
    override suspend fun getNewer(currentMaxId: Long): List<Post> {
        try {
            Log.d(TAG, "Запрос новых постов новее ID $currentMaxId")
            val response = apiService.getNewer(currentMaxId)
            
            if (response.isSuccessful) {
                val posts = response.body() ?: emptyList()
                Log.d(TAG, "Получено ${posts.size} новых постов")
                return posts
            } else {
                Log.e(TAG, "Ошибка сервера: ${response.code()}")
                throw RuntimeException("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке новых постов", e)
            throw e
        }
    }

    override suspend fun getMaxPostId(): Long {
        return dao.getMaxId() ?: 0L
    }

    override suspend fun saveNewerPosts(posts: List<Post>) {
        try {
            Log.d(TAG, "Сохранение ${posts.size} новых постов в БД")
            posts.forEach { post ->
                dao.insert(PostEntity.fromDto(post))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении новых постов", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "PostRepositoryHybrid"
    }
}

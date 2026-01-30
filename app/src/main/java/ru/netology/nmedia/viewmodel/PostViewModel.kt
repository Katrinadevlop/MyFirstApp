package ru.netology.nmedia.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryHybridImpl

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryHybridImpl(application)
    private val empty = Post(
        id = 0,
        author = "",
        content = "",
        published = "",
        likes = 0,
        likedByMe = false,
        shares = 0,
        views = 0,
        video = null,
    )
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    val edited = MutableLiveData(empty)
    val data = repository.get()
    
    private val _feedState = MutableLiveData(FeedModel())
    val feedState: LiveData<FeedModel> get() = _feedState
    
    // Для задания №1: управление новыми постами
    private val _newerPostsCount = MutableLiveData(0)
    val newerPostsCount: LiveData<Int> get() = _newerPostsCount
    
    private var newerPosts: List<Post> = emptyList()
    private var backgroundLoadingRunnable: Runnable? = null

    init {
        loadPosts()
        startBackgroundLoading()
    }

    fun loadPosts() {
        _feedState.value = FeedModel(loading = true)
        repository.refresh(
            onSuccess = {
                mainHandler.post {
                    _feedState.value = FeedModel()
                }
            },
            onError = { e ->
                mainHandler.post {
                    _feedState.value = FeedModel(error = true)
                }
            }
        )
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun add(content: String) {
        val text = content.trim()
        val current = edited.value ?: empty
        if (current.id == 0L) {
            repository.add(current.copy(content = text)) { e ->
                mainHandler.post {
                    _feedState.value = _feedState.value?.copy(error = true)
                }
            }
        } else {
            if (current.content != text) {
                repository.updateContentById(current.id, text) { e ->
                    mainHandler.post {
                        _feedState.value = _feedState.value?.copy(error = true)
                    }
                }
            }
        }
        edited.value = empty
    }

    fun create(content: String) {
        repository.add(Post(id = 0, author = "", content = content.trim(), published = "")) { e ->
            mainHandler.post {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }

    fun update(id: Long, content: String) {
        repository.updateContentById(id, content.trim()) { e ->
            mainHandler.post {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }

    fun like(id: Long) {
        repository.like(id) { e ->
            mainHandler.post {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }
    
    fun share(id: Long) = repository.share(id)
    fun view(id: Long) = repository.view(id)
    
    fun remove(id: Long) {
        repository.remove(id) { e ->
            mainHandler.post {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }

    fun cancelEdit() {
        edited.value = empty
    }

    fun refresh(callback: () -> Unit = {}) {
        _feedState.value = _feedState.value?.copy(loading = true)
        repository.refresh(
            onSuccess = {
                mainHandler.post {
                    _feedState.value = FeedModel()
                    callback()
                }
            },
            onError = { e ->
                mainHandler.post {
                    _feedState.value = FeedModel(error = true)
                    callback()
                }
            }
        )
    }

    // Новые методы для задания №1
    fun likeById(id: Long) {
        Thread {
            try {
                // Вызываем suspend-метод репозитория
                kotlinx.coroutines.runBlocking {
                    repository.likeById(id)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    _feedState.value = _feedState.value?.copy(error = true)
                }
            }
        }.start()
    }

    fun removeById(id: Long) {
        Thread {
            try {
                // Вызываем suspend-метод репозитория
                kotlinx.coroutines.runBlocking {
                    repository.removeById(id)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    _feedState.value = _feedState.value?.copy(error = true)
                }
            }
        }.start()
    }

    // Метод для задания №2: повторная синхронизация несохранённых постов
    fun retrySyncUnsavedPosts() {
        _feedState.value = _feedState.value?.copy(loading = true)
        repository.retrySyncUnsavedPosts(
            onSuccess = {
                mainHandler.post {
                    _feedState.value = FeedModel()
                }
            },
            onError = { e ->
                mainHandler.post {
                    _feedState.value = FeedModel(error = true)
                }
            }
        )
    }
    
    // Методы для задания №1: фоновая загрузка новых постов
    private fun startBackgroundLoading() {
        backgroundLoadingRunnable = object : Runnable {
            override fun run() {
                loadNewerPosts()
                mainHandler.postDelayed(this, 10_000) // Каждые 10 секунд
            }
        }
        mainHandler.postDelayed(backgroundLoadingRunnable!!, 10_000)
    }
    
    fun loadNewerPosts() {
        Thread {
            try {
                kotlinx.coroutines.runBlocking {
                    val maxId = repository.getMaxPostId()
                    val newPosts = repository.getNewer(maxId)
                    
                    if (newPosts.isNotEmpty()) {
                        newerPosts = newPosts
                        mainHandler.post {
                            _newerPostsCount.value = newPosts.size
                        }
                    }
                }
            } catch (e: Exception) {
                // Тихо игнорируем ошибки фоновой загрузки
                e.printStackTrace()
            }
        }.start()
    }
    
    fun showNewerPosts() {
        Thread {
            try {
                kotlinx.coroutines.runBlocking {
                    if (newerPosts.isNotEmpty()) {
                        repository.saveNewerPosts(newerPosts)
                        newerPosts = emptyList()
                        mainHandler.post {
                            _newerPostsCount.value = 0
                        }
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    _feedState.value = _feedState.value?.copy(error = true)
                }
            }
        }.start()
    }
    
    override fun onCleared() {
        super.onCleared()
        backgroundLoadingRunnable?.let { mainHandler.removeCallbacks(it) }
    }
}

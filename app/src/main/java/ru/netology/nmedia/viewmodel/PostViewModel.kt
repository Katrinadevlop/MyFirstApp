package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
    
    val edited = MutableLiveData(empty)
    val data = repository.get()
    
    private val _feedState = MutableLiveData(FeedModel())
    val feedState: LiveData<FeedModel> get() = _feedState
    
    // Для задания №1: управление новыми постами
    private val _newerPostsCount = MutableLiveData(0)
    val newerPostsCount: LiveData<Int> get() = _newerPostsCount
    
    private var newerPosts: List<Post> = emptyList()

    init {
        loadPosts()
        retrySyncUnsavedPosts()
        startBackgroundLoading()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _feedState.value = FeedModel(loading = true)
            try {
                repository.refreshSuspend()
                _feedState.value = FeedModel()
            } catch (e: Exception) {
                _feedState.value = FeedModel(error = true)
            }
        }
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun add(content: String) {
        val text = content.trim()
        val current = edited.value ?: empty
        viewModelScope.launch {
            try {
                if (current.id == 0L) {
                    repository.addSuspend(current.copy(content = text))
                } else {
                    if (current.content != text) {
                        repository.updateContentByIdSuspend(current.id, text)
                    }
                }
            } catch (e: Exception) {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
        edited.value = empty
    }

    fun create(content: String) {
        viewModelScope.launch {
            try {
                repository.addSuspend(Post(id = 0, author = "", content = content.trim(), published = ""))
            } catch (e: Exception) {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }

    fun update(id: Long, content: String) {
        viewModelScope.launch {
            try {
                repository.updateContentByIdSuspend(id, content.trim())
            } catch (e: Exception) {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }

    fun like(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id)
            } catch (e: Exception) {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }
    
    fun share(id: Long) = repository.share(id)
    fun view(id: Long) = repository.view(id)
    
    fun remove(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
            } catch (e: Exception) {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }

    fun cancelEdit() {
        edited.value = empty
    }

    fun refresh(callback: () -> Unit = {}) {
        viewModelScope.launch {
            _feedState.value = _feedState.value?.copy(loading = true)
            try {
                repository.refreshSuspend()
                _feedState.value = FeedModel()
            } catch (e: Exception) {
                _feedState.value = FeedModel(error = true)
            } finally {
                callback()
            }
        }
    }


    // Метод для задания №2: повторная синхронизация несохранённых постов
    fun retrySyncUnsavedPosts() {
        viewModelScope.launch {
            try {
                repository.retrySyncUnsavedPostsSuspend()
            } catch (e: Exception) {
                // Тихо игнорируем ошибки синхронизации при старте
                e.printStackTrace()
            }
        }
    }
    
    // Методы для задания №1: фоновая загрузка новых постов
    private fun startBackgroundLoading() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(10_000) // Начальная задержка
            while (true) {
                loadNewerPosts()
                kotlinx.coroutines.delay(10_000) // Каждые 10 секунд
            }
        }
    }
    
    fun loadNewerPosts() {
        viewModelScope.launch {
            try {
                val maxId = repository.getMaxPostId()
                val newPosts = repository.getNewer(maxId)
                
                if (newPosts.isNotEmpty()) {
                    newerPosts = newPosts
                    _newerPostsCount.value = newPosts.size
                }
            } catch (e: Exception) {
                // Тихо игнорируем ошибки фоновой загрузки
                e.printStackTrace()
            }
        }
    }
    
    fun showNewerPosts() {
        viewModelScope.launch {
            try {
                if (newerPosts.isNotEmpty()) {
                    repository.saveNewerPosts(newerPosts)
                    newerPosts = emptyList()
                    _newerPostsCount.value = 0
                }
            } catch (e: Exception) {
                _feedState.value = _feedState.value?.copy(error = true)
            }
        }
    }
}

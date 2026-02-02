package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.PhotoModel
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
    
    private val _edited = MutableStateFlow(empty)
    val edited: StateFlow<Post> = _edited.asStateFlow()
    
    private val _photo = MutableStateFlow<PhotoModel?>(null)
    val photo: StateFlow<PhotoModel?> = _photo.asStateFlow()
    
    val data = repository.data
    
    private val _feedState = MutableStateFlow(FeedModel())
    val feedState: StateFlow<FeedModel> = _feedState.asStateFlow()
    
    // Для задания №1: управление новыми постами
    private val _newerPostsCount = MutableStateFlow(0)
    val newerPostsCount: StateFlow<Int> = _newerPostsCount.asStateFlow()
    
    // Для задания №2: отслеживание несинхронизированных постов
    private val _unsyncedPostsCount = MutableStateFlow(0)
    val unsyncedPostsCount: StateFlow<Int> = _unsyncedPostsCount.asStateFlow()
    
    private var newerPosts: List<Post> = emptyList()

    init {
        loadPosts()
        retrySyncUnsavedPosts()
        startBackgroundLoading()
        updateUnsyncedPostsCount()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _feedState.value = FeedModel(loading = true)
            try {
                repository.refresh()
                _feedState.value = FeedModel()
            } catch (e: Exception) {
                _feedState.value = FeedModel(error = true)
            }
        }
    }

    fun edit(post: Post) {
        _edited.value = post
    }

    fun add(content: String) {
        val text = content.trim()
        val current = _edited.value
        viewModelScope.launch {
            try {
                if (current.id == 0L) {
                    repository.add(current.copy(content = text))
                } else {
                    if (current.content != text) {
                        repository.updateContentById(current.id, text)
                    }
                }
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(error = true)
            }
        }
        _edited.value = empty
    }

    fun create(content: String) {
        viewModelScope.launch {
            try {
                repository.add(Post(id = 0, author = "", content = content.trim(), published = ""))
                updateUnsyncedPostsCount()
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(error = true)
                updateUnsyncedPostsCount()
            }
        }
    }

    fun update(id: Long, content: String) {
        viewModelScope.launch {
            try {
                repository.updateContentById(id, content.trim())
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(error = true)
            }
        }
    }

    fun like(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id)
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(error = true)
            }
        }
    }
    
    fun share(id: Long) {
        viewModelScope.launch {
            repository.share(id)
        }
    }
    
    fun view(id: Long) {
        viewModelScope.launch {
            repository.view(id)
        }
    }
    
    fun remove(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(error = true)
            }
        }
    }

    fun cancelEdit() {
        _edited.value = empty
        _photo.value = null
    }
    
    fun setPhoto(photoModel: PhotoModel?) {
        _photo.value = photoModel
    }
    
    fun clearPhoto() {
        _photo.value = null
    }

    fun refresh(callback: () -> Unit = {}) {
        viewModelScope.launch {
            _feedState.value = _feedState.value.copy(loading = true)
            try {
                repository.refresh()
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
            _feedState.value = _feedState.value.copy(loading = true)
            try {
                repository.retrySyncUnsavedPosts()
                updateUnsyncedPostsCount()
                _feedState.value = FeedModel()
            } catch (e: Exception) {
                _feedState.value = _feedState.value.copy(error = true)
                e.printStackTrace()
            }
        }
    }
    
    private fun updateUnsyncedPostsCount() {
        viewModelScope.launch {
            try {
                _unsyncedPostsCount.value = repository.getUnsyncedPostsCount()
            } catch (e: Exception) {
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
                _feedState.value = _feedState.value.copy(error = true)
            }
        }
    }
}

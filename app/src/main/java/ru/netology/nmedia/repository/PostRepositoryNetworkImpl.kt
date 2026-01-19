package ru.netology.nmedia.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.RetrofitClient
import ru.netology.nmedia.dto.Post

class PostRepositoryNetworkImpl : PostRepository {
    private val apiService = RetrofitClient.postApiService
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    
    override fun get(): LiveData<List<Post>> {
        refresh {}
        return _posts
    }

    override fun like(id: Long) {
        ioScope.launch {
            try {
                // Find the current post to check if it's already liked
                val currentPosts = _posts.value ?: emptyList()
                val post = currentPosts.find { it.id == id } ?: return@launch
                
                val response = if (post.likedByMe) {
                    apiService.unlikeById(id)
                } else {
                    apiService.likeById(id)
                }
                
                if (response.isSuccessful) {
                    val updatedPost = response.body()
                    if (updatedPost != null) {
                        // Update the post in the list
                        val updatedPosts = currentPosts.map { 
                            if (it.id == id) updatedPost else it 
                        }
                        _posts.postValue(updatedPosts)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun share(id: Long) {
        ioScope.launch {
            val currentPosts = _posts.value ?: emptyList()
            val updatedPosts = currentPosts.map { post ->
                if (post.id == id) {
                    post.copy(shares = post.shares + 1)
                } else {
                    post
                }
            }
            _posts.postValue(updatedPosts)
        }
    }

    override fun view(id: Long) {
        ioScope.launch {
            val currentPosts = _posts.value ?: emptyList()
            val updatedPosts = currentPosts.map { post ->
                if (post.id == id) {
                    post.copy(views = post.views + 1)
                } else {
                    post
                }
            }
            _posts.postValue(updatedPosts)
        }
    }

    override fun remove(id: Long) {
        ioScope.launch {
            try {
                val response = apiService.removeById(id)
                if (response.isSuccessful) {
                    val currentPosts = _posts.value ?: emptyList()
                    _posts.postValue(currentPosts.filter { it.id != id })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun add(post: Post) {
        ioScope.launch {
            try {
                val response = apiService.save(post)
                if (response.isSuccessful) {
                    refresh {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun updateContentById(id: Long, content: String) {
        ioScope.launch {
            try {
                val currentPosts = _posts.value ?: emptyList()
                val post = currentPosts.find { it.id == id }
                if (post != null) {
                    val updatedPost = post.copy(content = content)
                    val response = apiService.save(updatedPost)
                    if (response.isSuccessful) {
                        refresh {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun refresh(callback: () -> Unit) {
        ioScope.launch {
            try {
                Log.d(TAG, "Запрос постов с сервера...")
                val response = apiService.getAll()
                Log.d(TAG, "Ответ получен: код ${response.code()}")
                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    Log.d(TAG, "Получено постов: ${posts.size}")
                    _posts.postValue(posts)
                } else {
                    Log.e(TAG, "Ошибка: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при запросе постов", e)
                e.printStackTrace()
            } finally {
                callback()
            }
        }
    }

    companion object {
        private const val TAG = "PostRepositoryNetwork"
    }
}

package ru.netology.nmedia.repository

import kotlinx.coroutines.*
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.*

class PostRepositoryAsync(private val api: ApiService) {
    
    /**
     * Асинхронное получение постов с авторами (с использованием async/await)
     */
    suspend fun getPostsWithAuthorsAsync(): List<PostWithAuthor> = coroutineScope {
        // 1. Получаем все посты
        val posts = withContext(Dispatchers.IO) { api.getPosts() }
        
        // 2. Параллельно получаем авторов для всех постов
        posts.map { post ->
            async(Dispatchers.IO) {
                val author = api.getAuthorById(post.authorId)
                PostWithAuthor(post = post, author = author)
            }
        }.awaitAll()
    }
    
    /**
     * Асинхронное получение постов с авторами и комментариями (с авторами комментариев)
     */
    suspend fun getPostsWithAuthorsAndCommentsAsync(): List<PostWithAuthor> = coroutineScope {
        // 1. Получаем все посты
        val posts = withContext(Dispatchers.IO) { api.getPosts() }
        
        // 2. Параллельно обрабатываем каждый пост
        posts.map { post ->
            async(Dispatchers.IO) {
                // Параллельно получаем автора поста и комментарии
                val authorDeferred = async { api.getAuthorById(post.authorId) }
                val commentsDeferred = async { api.getCommentsByPostId(post.id) }
                
                val author = authorDeferred.await()
                val comments = commentsDeferred.await()
                
                // Параллельно получаем авторов для всех комментариев
                val commentsWithAuthors = comments.map { comment ->
                    async {
                        val commentAuthor = api.getAuthorById(comment.authorId)
                        CommentWithAuthor(comment = comment, author = commentAuthor)
                    }
                }.awaitAll()
                
                PostWithAuthor(post = post, author = author, comments = commentsWithAuthors)
            }
        }.awaitAll()
    }
    
    /**
     * Оптимизированная асинхронная версия с кэшированием авторов
     */
    suspend fun getPostsWithAuthorsAndCommentsAsyncCached(): List<PostWithAuthor> = coroutineScope {
        // Кэш для авторов (потокобезопасный)
        val authorsCache = mutableMapOf<Long, Deferred<Author>>()
        
        // Функция для получения автора с кэшированием
        suspend fun getAuthorCached(authorId: Long): Author {
            val deferred = synchronized(authorsCache) {
                authorsCache.getOrPut(authorId) {
                    async(Dispatchers.IO) { api.getAuthorById(authorId) }
                }
            }
            return deferred.await()
        }
        
        // 1. Получаем все посты
        val posts = withContext(Dispatchers.IO) { api.getPosts() }
        
        // 2. Параллельно обрабатываем каждый пост
        posts.map { post ->
            async(Dispatchers.IO) {
                // Параллельно получаем автора поста и комментарии
                val authorDeferred = async { getAuthorCached(post.authorId) }
                val commentsDeferred = async { api.getCommentsByPostId(post.id) }
                
                val author = authorDeferred.await()
                val comments = commentsDeferred.await()
                
                // Параллельно получаем авторов для всех комментариев
                val commentsWithAuthors = comments.map { comment ->
                    async {
                        val commentAuthor = getAuthorCached(comment.authorId)
                        CommentWithAuthor(comment = comment, author = commentAuthor)
                    }
                }.awaitAll()
                
                PostWithAuthor(post = post, author = author, comments = commentsWithAuthors)
            }
        }.awaitAll()
    }
}

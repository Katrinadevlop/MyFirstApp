package ru.netology.nmedia.repository

import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.*

class PostRepository(private val api: ApiService) {
    
    /**
     * Последовательное получение постов с авторами и комментариями
     */
    fun getPostsWithAuthorsSequential(): List<PostWithAuthor> {
        // 1. Получаем все посты
        val posts = api.getPosts()
        
        // 2. Для каждого поста получаем автора
        return posts.map { post ->
            val author = api.getAuthorById(post.authorId)
            PostWithAuthor(post = post, author = author)
        }
    }
    
    /**
     * Последовательное получение постов с авторами и комментариями (с авторами комментариев)
     */
    fun getPostsWithAuthorsAndCommentsSequential(): List<PostWithAuthor> {
        // 1. Получаем все посты
        val posts = api.getPosts()
        
        // 2. Для каждого поста получаем автора и комментарии
        return posts.map { post ->
            val author = api.getAuthorById(post.authorId)
            
            // 3. Получаем комментарии для поста
            val comments = api.getCommentsByPostId(post.id)
            
            // 4. Для каждого комментария получаем автора
            val commentsWithAuthors = comments.map { comment ->
                val commentAuthor = api.getAuthorById(comment.authorId)
                CommentWithAuthor(comment = comment, author = commentAuthor)
            }
            
            PostWithAuthor(post = post, author = author, comments = commentsWithAuthors)
        }
    }
    
    /**
     * Оптимизированная версия - кэширует авторов чтобы не запрашивать их повторно
     */
    fun getPostsWithAuthorsAndCommentsCached(): List<PostWithAuthor> {
        // Кэш для авторов
        val authorsCache = mutableMapOf<Long, Author>()
        
        // Функция для получения автора с кэшированием
        fun getAuthorCached(authorId: Long): Author {
            return authorsCache.getOrPut(authorId) {
                api.getAuthorById(authorId)
            }
        }
        
        // 1. Получаем все посты
        val posts = api.getPosts()
        
        // 2. Для каждого поста получаем автора и комментарии
        return posts.map { post ->
            val author = getAuthorCached(post.authorId)
            
            // 3. Получаем комментарии для поста
            val comments = api.getCommentsByPostId(post.id)
            
            // 4. Для каждого комментария получаем автора
            val commentsWithAuthors = comments.map { comment ->
                val commentAuthor = getAuthorCached(comment.authorId)
                CommentWithAuthor(comment = comment, author = commentAuthor)
            }
            
            PostWithAuthor(post = post, author = author, comments = commentsWithAuthors)
        }
    }
}

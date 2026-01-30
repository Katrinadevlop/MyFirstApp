package ru.netology.nmedia

import kotlinx.coroutines.runBlocking
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.PostWithAuthor
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryAsync
import kotlin.system.measureTimeMillis

fun main() {
    println("=== Демонстрация получения постов с авторами ===\n")
    
    val apiService = ApiService()
    val repository = PostRepository(apiService)
    val repositoryAsync = PostRepositoryAsync(apiService)
    
    // Демонстрация последовательного подхода
    println("1. ПОСЛЕДОВАТЕЛЬНЫЙ ПОДХОД (без комментариев)")
    println("-".repeat(50))
    val timeSequential = measureTimeMillis {
        try {
            val posts = repository.getPostsWithAuthorsSequential()
            printPosts(posts)
        } catch (e: Exception) {
            println("Ошибка: ${e.message}")
            println("Убедитесь, что сервер запущен на localhost:9999")
        }
    }
    println("Время выполнения: $timeSequential мс\n")
    
    // Демонстрация последовательного подхода с комментариями
    println("2. ПОСЛЕДОВАТЕЛЬНЫЙ ПОДХОД (с комментариями)")
    println("-".repeat(50))
    val timeSequentialComments = measureTimeMillis {
        try {
            val posts = repository.getPostsWithAuthorsAndCommentsSequential()
            printPostsWithComments(posts)
        } catch (e: Exception) {
            println("Ошибка: ${e.message}")
        }
    }
    println("Время выполнения: $timeSequentialComments мс\n")
    
    // Демонстрация оптимизированного последовательного подхода с кэшированием
    println("3. ПОСЛЕДОВАТЕЛЬНЫЙ ПОДХОД С КЭШИРОВАНИЕМ")
    println("-".repeat(50))
    val timeSequentialCached = measureTimeMillis {
        try {
            val posts = repository.getPostsWithAuthorsAndCommentsCached()
            printPostsWithComments(posts)
        } catch (e: Exception) {
            println("Ошибка: ${e.message}")
        }
    }
    println("Время выполнения: $timeSequentialCached мс\n")
    
    // Демонстрация асинхронного подхода
    println("4. АСИНХРОННЫЙ ПОДХОД с async/await (без комментариев)")
    println("-".repeat(50))
    val timeAsync = measureTimeMillis {
        runBlocking {
            try {
                val posts = repositoryAsync.getPostsWithAuthorsAsync()
                printPosts(posts)
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }
    println("Время выполнения: $timeAsync мс\n")
    
    // Демонстрация асинхронного подхода с комментариями
    println("5. АСИНХРОННЫЙ ПОДХОД с async/await (с комментариями)")
    println("-".repeat(50))
    val timeAsyncComments = measureTimeMillis {
        runBlocking {
            try {
                val posts = repositoryAsync.getPostsWithAuthorsAndCommentsAsync()
                printPostsWithComments(posts)
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }
    println("Время выполнения: $timeAsyncComments мс\n")
    
    // Демонстрация асинхронного подхода с кэшированием
    println("6. АСИНХРОННЫЙ ПОДХОД с кэшированием")
    println("-".repeat(50))
    val timeAsyncCached = measureTimeMillis {
        runBlocking {
            try {
                val posts = repositoryAsync.getPostsWithAuthorsAndCommentsAsyncCached()
                printPostsWithComments(posts)
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }
    println("Время выполнения: $timeAsyncCached мс\n")
    
    // Сравнение производительности
    println("=== СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ ===")
    println("-".repeat(50))
    println("Последовательный (без комментариев):     $timeSequential мс")
    println("Последовательный (с комментариями):      $timeSequentialComments мс")
    println("Последовательный с кэшированием:         $timeSequentialCached мс")
    println("Асинхронный (без комментариев):          $timeAsync мс")
    println("Асинхронный (с комментариями):           $timeAsyncComments мс")
    println("Асинхронный с кэшированием:              $timeAsyncCached мс")
}

fun printPosts(posts: List<PostWithAuthor>) {
    posts.forEach { postWithAuthor ->
        println("Пост #${postWithAuthor.post.id}")
        println("  Автор: ${postWithAuthor.author.name}")
        println("  Содержание: ${postWithAuthor.post.content.take(50)}...")
        println("  Лайков: ${postWithAuthor.post.likes}")
        println()
    }
}

fun printPostsWithComments(posts: List<PostWithAuthor>) {
    posts.forEach { postWithAuthor ->
        println("Пост #${postWithAuthor.post.id}")
        println("  Автор: ${postWithAuthor.author.name}")
        println("  Содержание: ${postWithAuthor.post.content.take(50)}...")
        println("  Лайков: ${postWithAuthor.post.likes}")
        println("  Комментариев: ${postWithAuthor.comments.size}")
        
        postWithAuthor.comments.forEach { commentWithAuthor ->
            println("    Комментарий #${commentWithAuthor.comment.id}")
            println("      Автор: ${commentWithAuthor.author.name}")
            println("      Текст: ${commentWithAuthor.comment.content.take(40)}...")
        }
        println()
    }
}

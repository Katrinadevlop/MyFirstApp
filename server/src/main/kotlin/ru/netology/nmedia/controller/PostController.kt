package ru.netology.nmedia.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.service.PostService
import kotlin.random.Random

@RestController
@RequestMapping("/api/posts", "/api/slow/posts")
class PostController(private val service: PostService) {
    
    // Функция для симуляции случайной ошибки сервера (50% случаев)
    private fun maybeThrowError() {
        if (Random.nextBoolean()) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Случайная ошибка сервера"
            )
        }
    }
    
    @GetMapping
    fun getAll(): List<Post> {
        maybeThrowError()
        return service.getAll()
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Post {
        maybeThrowError()
        return service.getById(id)
    }

    @PostMapping
    fun save(@RequestBody dto: Post): Post {
        maybeThrowError()
        return service.save(dto)
    }

    @DeleteMapping("/{id}")
    fun removeById(@PathVariable id: Long) {
        maybeThrowError()
        service.removeById(id)
    }

    @PostMapping("/{id}/likes")
    fun likeById(@PathVariable id: Long): Post {
        maybeThrowError()
        return service.likeById(id)
    }

    @DeleteMapping("/{id}/likes")
    fun unlikeById(@PathVariable id: Long): Post {
        maybeThrowError()
        return service.unlikeById(id)
    }
}

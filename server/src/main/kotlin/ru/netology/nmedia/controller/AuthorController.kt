package ru.netology.nmedia.controller

import org.springframework.web.bind.annotation.*
import ru.netology.nmedia.dto.Author
import ru.netology.nmedia.service.AuthorService

@RestController
@RequestMapping("/api/authors")
class AuthorController(private val service: AuthorService) {
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Author = service.getById(id)
}

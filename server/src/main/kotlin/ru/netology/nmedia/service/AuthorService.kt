package ru.netology.nmedia.service

import org.springframework.stereotype.Service
import ru.netology.nmedia.dto.Author
import ru.netology.nmedia.exception.NotFoundException

@Service
class AuthorService {
    private val authors: Map<Long, Author> = mapOf(
        1L to Author(id = 1L, name = "Netology", avatar = "netology.jpg"),
        2L to Author(id = 2L, name = "Сбер", avatar = "sber.jpg"),
        3L to Author(id = 3L, name = "Тинькофф", avatar = "tcs.jpg"),
    )

    fun getById(id: Long): Author = authors[id] ?: throw NotFoundException()
}

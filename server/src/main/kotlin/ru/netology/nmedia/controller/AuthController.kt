package ru.netology.nmedia.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.netology.nmedia.dto.AuthenticationRequest
import ru.netology.nmedia.dto.RegistrationRequest
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.service.UserService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val service: UserService,
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegistrationRequest): Token = service.register(request)

    @PostMapping("/login")
    fun login(@RequestBody request: AuthenticationRequest): Token = service.login(request)
}

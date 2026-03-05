package ru.netology.nmedia.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.netology.nmedia.dto.AuthenticationRequest
import ru.netology.nmedia.dto.RegistrationRequest
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.entity.UserEntity
import ru.netology.nmedia.exception.BadRequestException
import ru.netology.nmedia.exception.ForbiddenException
import ru.netology.nmedia.repository.UserRepository
import ru.netology.nmedia.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder

@Service
@Transactional
class UserService(
    private val repository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    fun register(request: RegistrationRequest): Token {
        val login = request.login.trim()
        val pass = request.pass
        val name = request.name.trim()

        if (login.isBlank() || pass.isBlank() || name.isBlank()) {
            throw BadRequestException("login, pass, name must be non-empty")
        }

        if (repository.findByLogin(login) != null) {
            throw BadRequestException("login is already taken")
        }

        val user = repository.save(
            UserEntity(
                login = login,
                pass = passwordEncoder.encode(pass),
                name = name,
                avatar = null,
            )
        )

        return Token(jwtTokenProvider.generate(login = user.login, id = user.id))
    }

    fun login(request: AuthenticationRequest): Token {
        val login = request.login.trim()
        val pass = request.pass

        if (login.isBlank() || pass.isBlank()) {
            throw BadRequestException("login and pass must be non-empty")
        }

        val user = repository.findByLogin(login) ?: throw BadRequestException("user not found")

        if (!passwordEncoder.matches(pass, user.pass)) {
            throw ForbiddenException("wrong password")
        }

        return Token(jwtTokenProvider.generate(login = user.login, id = user.id))
    }
}

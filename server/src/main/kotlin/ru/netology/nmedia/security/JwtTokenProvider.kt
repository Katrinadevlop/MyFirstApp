package ru.netology.nmedia.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.ttl-seconds:86400}") private val ttlSeconds: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val parser = Jwts.parser().verifyWith(key).build()

    fun generate(login: String, id: Long): String {
        val now = Date()
        val exp = Date(now.time + ttlSeconds * 1000)

        return Jwts.builder()
            .subject(login)
            .claim("id", id)
            .issuedAt(now)
            .expiration(exp)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    fun validate(token: String): Boolean = try {
        parser.parseSignedClaims(token)
        true
    } catch (e: Exception) {
        false
    }

    fun getLogin(token: String): String = parser.parseSignedClaims(token).payload.subject
}

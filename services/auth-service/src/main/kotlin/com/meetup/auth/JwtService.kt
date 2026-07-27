package com.meetup.auth

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Выпуск и разбор JWT-токенов (HS256). Секрет общий с gateway,
 * который валидирует access-токены на входе в систему.
 *
 * @param secret симметричный секрет подписи (минимум 32 байта).
 * @property accessTtlMinutes время жизни access-токена в минутах.
 * @property refreshTtlDays время жизни refresh-токена в днях.
 */
@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-ttl-minutes}") private val accessTtlMinutes: Long,
    @Value("\${jwt.refresh-ttl-days}") private val refreshTtlDays: Long,
) {
    /** Ключ подписи, построенный из секрета. */
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    /** Выпускает короткоживущий access-токен для [userId]. */
    fun issueAccessToken(userId: UUID): String =
        issue(userId, type = "access", ttl = Duration.ofMinutes(accessTtlMinutes))

    /** Выпускает долгоживущий refresh-токен для [userId]. */
    fun issueRefreshToken(userId: UUID): String =
        issue(userId, type = "refresh", ttl = Duration.ofDays(refreshTtlDays))

    /**
     * Проверяет подпись и срок действия токена и возвращает идентификатор
     * пользователя из subject.
     *
     * @param token строка JWT.
     * @param expectedType ожидаемый тип токена: "access" или "refresh".
     * @throws JwtException если подпись неверна, срок истёк или тип не совпадает.
     */
    fun parseUserId(token: String, expectedType: String): UUID {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        if (claims["type"] != expectedType) throw JwtException("Unexpected token type")
        return UUID.fromString(claims.subject)
    }

    /** Собирает и подписывает токен типа [type] со сроком жизни [ttl]. */
    private fun issue(userId: UUID, type: String, ttl: Duration): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(Date())
            .expiration(Date.from(Instant.now().plus(ttl)))
            .signWith(key)
            .compact()
}

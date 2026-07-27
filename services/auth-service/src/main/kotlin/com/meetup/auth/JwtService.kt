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

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-ttl-minutes}") private val accessTtlMinutes: Long,
    @Value("\${jwt.refresh-ttl-days}") private val refreshTtlDays: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun issueAccessToken(userId: UUID): String =
        issue(userId, type = "access", ttl = Duration.ofMinutes(accessTtlMinutes))

    fun issueRefreshToken(userId: UUID): String =
        issue(userId, type = "refresh", ttl = Duration.ofDays(refreshTtlDays))

    fun parseUserId(token: String, expectedType: String): UUID {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        if (claims["type"] != expectedType) throw JwtException("Unexpected token type")
        return UUID.fromString(claims.subject)
    }

    private fun issue(userId: UUID, type: String, ttl: Duration): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(Date())
            .expiration(Date.from(Instant.now().plus(ttl)))
            .signWith(key)
            .compact()
}

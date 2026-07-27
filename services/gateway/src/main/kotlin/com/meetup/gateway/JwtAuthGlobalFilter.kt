package com.meetup.gateway

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Глобальный фильтр аутентификации: проверяет access-токен и проставляет
 * доверенный заголовок X-User-Id для сервисов за шлюзом. Любой X-User-Id,
 * присланный клиентом, отбрасывается — подделать личность нельзя.
 *
 * @param secret симметричный секрет подписи JWT, общий с auth-service.
 */
@Component
class JwtAuthGlobalFilter(@Value("\${jwt.secret}") secret: String) : GlobalFilter, Ordered {
    /** Ключ проверки подписи, построенный из секрета. */
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    /** Префиксы путей, доступных без токена (регистрация, логин, refresh). */
    private val publicPrefixes = listOf("/api/auth/")

    /**
     * Пропускает публичные пути без проверки; для остальных валидирует
     * Bearer-токен и подменяет X-User-Id на значение из токена.
     * Невалидный или отсутствующий токен — ответ 401.
     */
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.uri.path
        if (publicPrefixes.any { path.startsWith(it) }) {
            return chain.filter(stripSpoofableHeaders(exchange))
        }

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange)
        }

        val userId = try {
            val claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(authHeader.removePrefix("Bearer ")).payload
            if (claims["type"] != "access") return reject(exchange)
            claims.subject
        } catch (e: JwtException) {
            return reject(exchange)
        }

        val mutated = exchange.mutate()
            .request(
                exchange.request.mutate()
                    .headers { it.remove("X-User-Id") }
                    .header("X-User-Id", userId)
                    .build(),
            )
            .build()
        return chain.filter(mutated)
    }

    /** Фильтр выполняется раньше маршрутизации (отрицательный порядок). */
    override fun getOrder(): Int = -100

    /** Удаляет клиентский X-User-Id даже на публичных путях. */
    private fun stripSpoofableHeaders(exchange: ServerWebExchange): ServerWebExchange =
        exchange.mutate()
            .request(exchange.request.mutate().headers { it.remove("X-User-Id") }.build())
            .build()

    /** Завершает запрос ответом 401 Unauthorized. */
    private fun reject(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}

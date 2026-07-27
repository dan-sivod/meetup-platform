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
 * Validates the access token and injects a trusted X-User-Id header for
 * downstream services. Any client-supplied X-User-Id is dropped.
 */
@Component
class JwtAuthGlobalFilter(@Value("\${jwt.secret}") secret: String) : GlobalFilter, Ordered {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    private val publicPrefixes = listOf("/api/auth/")

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

    override fun getOrder(): Int = -100

    private fun stripSpoofableHeaders(exchange: ServerWebExchange): ServerWebExchange =
        exchange.mutate()
            .request(exchange.request.mutate().headers { it.remove("X-User-Id") }.build())
            .build()

    private fun reject(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}

package com.meetup.auth

import com.meetup.common.events.Topics
import com.meetup.common.events.UserRegistered
import io.jsonwebtoken.JwtException
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:Size(min = 8, max = 128) val password: String,
    @field:NotBlank val displayName: String,
)

data class LoginRequest(val email: String, val password: String)

data class RefreshRequest(val refreshToken: String)

data class TokenResponse(val userId: UUID, val accessToken: String, val refreshToken: String)

@RestController
@RequestMapping("/auth")
class AuthController(
    private val credentials: CredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val kafka: KafkaTemplate<String, Any>,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): TokenResponse {
        if (credentials.existsByEmail(request.email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already registered")
        }
        val credential = credentials.save(
            Credential(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                displayName = request.displayName,
            ),
        )
        kafka.send(
            Topics.USER_REGISTERED,
            credential.id.toString(),
            UserRegistered(credential.id, credential.email, credential.displayName),
        )
        return tokensFor(credential.id)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse {
        val credential = credentials.findByEmail(request.email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        if (!passwordEncoder.matches(request.password, credential.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }
        return tokensFor(credential.id)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): TokenResponse {
        val userId = try {
            jwtService.parseUserId(request.refreshToken, expectedType = "refresh")
        } catch (e: JwtException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
        }
        return tokensFor(userId)
    }

    private fun tokensFor(userId: UUID) = TokenResponse(
        userId = userId,
        accessToken = jwtService.issueAccessToken(userId),
        refreshToken = jwtService.issueRefreshToken(userId),
    )
}

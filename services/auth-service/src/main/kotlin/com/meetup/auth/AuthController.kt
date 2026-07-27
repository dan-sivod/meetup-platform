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

/**
 * Тело запроса на регистрацию.
 *
 * @property email адрес электронной почты (валидируется формат).
 * @property password пароль длиной от 8 до 128 символов.
 * @property displayName отображаемое имя пользователя.
 */
data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:Size(min = 8, max = 128) val password: String,
    @field:NotBlank val displayName: String,
)

/**
 * Тело запроса на вход.
 *
 * @property email адрес почты, указанный при регистрации.
 * @property password пароль в открытом виде (передаётся только по TLS).
 */
data class LoginRequest(val email: String, val password: String)

/**
 * Тело запроса на обновление пары токенов.
 *
 * @property refreshToken действующий refresh-токен.
 */
data class RefreshRequest(val refreshToken: String)

/**
 * Ответ с парой токенов.
 *
 * @property userId идентификатор пользователя.
 * @property accessToken короткоживущий токен для запросов через gateway.
 * @property refreshToken долгоживущий токен для обновления пары.
 */
data class TokenResponse(val userId: UUID, val accessToken: String, val refreshToken: String)

/**
 * HTTP-эндпоинты аутентификации: регистрация, вход, обновление токенов.
 * Единственные маршруты платформы, доступные без JWT.
 *
 * @property credentials репозиторий учётных данных.
 * @property passwordEncoder кодировщик BCrypt для хэширования и проверки паролей.
 * @property jwtService сервис выпуска и разбора JWT.
 * @property kafka продюсер доменных событий (публикует user.registered).
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val credentials: CredentialRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val kafka: KafkaTemplate<String, Any>,
) {
    /**
     * Регистрирует пользователя, публикует событие user.registered
     * (по нему users-service создаст профиль) и сразу выдаёт пару токенов.
     *
     * @throws ResponseStatusException 409, если почта уже занята.
     */
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

    /**
     * Проверяет пару почта/пароль и выдаёт токены.
     *
     * @throws ResponseStatusException 401 при неверных учётных данных
     * (одинаковый ответ для «нет пользователя» и «неверный пароль»).
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse {
        val credential = credentials.findByEmail(request.email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        if (!passwordEncoder.matches(request.password, credential.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }
        return tokensFor(credential.id)
    }

    /**
     * Обменивает действующий refresh-токен на новую пару токенов.
     *
     * @throws ResponseStatusException 401, если токен просрочен, подделан
     * или не является refresh-токеном.
     */
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): TokenResponse {
        val userId = try {
            jwtService.parseUserId(request.refreshToken, expectedType = "refresh")
        } catch (e: JwtException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
        }
        return tokensFor(userId)
    }

    /** Выпускает свежую пару access/refresh для [userId]. */
    private fun tokensFor(userId: UUID) = TokenResponse(
        userId = userId,
        accessToken = jwtService.issueAccessToken(userId),
        refreshToken = jwtService.issueRefreshToken(userId),
    )
}

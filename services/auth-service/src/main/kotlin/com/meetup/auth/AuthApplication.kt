package com.meetup.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Точка входа auth-service — сервиса аутентификации.
 * Отвечает за регистрацию, логин и выпуск JWT-токенов.
 */
@SpringBootApplication
class AuthApplication {
    /** Кодировщик паролей BCrypt: хэширование при регистрации и проверка при логине. */
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}

/** Запускает Spring Boot приложение auth-service. */
fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}

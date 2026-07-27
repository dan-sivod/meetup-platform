package com.meetup.users

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа users-service — сервиса профилей пользователей.
 * Профили создаются автоматически по событию user.registered.
 */
@SpringBootApplication
class UsersApplication

/** Запускает Spring Boot приложение users-service. */
fun main(args: Array<String>) {
    runApplication<UsersApplication>(*args)
}

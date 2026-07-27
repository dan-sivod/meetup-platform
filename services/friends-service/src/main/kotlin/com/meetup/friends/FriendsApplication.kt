package com.meetup.friends

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа friends-service — сервиса графа дружбы.
 * Хранит заявки, принятые связи и блокировки; отдаёт ACL-проверку
 * «являются ли двое друзьями» для других сервисов.
 */
@SpringBootApplication
class FriendsApplication

/** Запускает Spring Boot приложение friends-service. */
fun main(args: Array<String>) {
    runApplication<FriendsApplication>(*args)
}

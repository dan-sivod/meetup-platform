package com.meetup.notifications

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа notifications-service — сервиса уведомлений.
 * Слушает все доменные события и превращает их в in-app inbox.
 */
@SpringBootApplication
class NotificationsApplication

/** Запускает Spring Boot приложение notifications-service. */
fun main(args: Array<String>) {
    runApplication<NotificationsApplication>(*args)
}

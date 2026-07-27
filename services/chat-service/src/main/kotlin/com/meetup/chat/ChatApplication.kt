package com.meetup.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа chat-service — сервиса общения.
 * Realtime-доставка по WebSocket (STOMP), история — по REST;
 * комната создаётся автоматически на каждую встречу.
 */
@SpringBootApplication
class ChatApplication

/** Запускает Spring Boot приложение chat-service. */
fun main(args: Array<String>) {
    runApplication<ChatApplication>(*args)
}

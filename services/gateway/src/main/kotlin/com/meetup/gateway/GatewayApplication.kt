package com.meetup.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа gateway — единой точки входа платформы (Spring Cloud Gateway).
 * Маршрутизирует запросы с префиксом /api к сервисам и валидирует JWT
 * на каждом запросе.
 */
@SpringBootApplication
class GatewayApplication

/** Запускает Spring Boot приложение gateway. */
fun main(args: Array<String>) {
    runApplication<GatewayApplication>(*args)
}

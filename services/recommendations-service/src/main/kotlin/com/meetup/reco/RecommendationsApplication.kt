package com.meetup.reco

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа recommendations-service — сервиса рекомендаций мест.
 * Строит скоринг по сигналам из Kafka: избранное и подтверждённые встречи.
 */
@SpringBootApplication
class RecommendationsApplication

/** Запускает Spring Boot приложение recommendations-service. */
fun main(args: Array<String>) {
    runApplication<RecommendationsApplication>(*args)
}

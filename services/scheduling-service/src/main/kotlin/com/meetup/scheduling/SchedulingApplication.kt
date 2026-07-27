package com.meetup.scheduling

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа scheduling-service — сервиса согласования времени встречи.
 * Участники голосуют за предложенные слоты (логика в духе When2meet).
 */
@SpringBootApplication
class SchedulingApplication

/** Запускает Spring Boot приложение scheduling-service. */
fun main(args: Array<String>) {
    runApplication<SchedulingApplication>(*args)
}

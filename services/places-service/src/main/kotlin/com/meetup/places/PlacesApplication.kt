package com.meetup.places

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа places-service — каталога мест для встреч.
 * Хранит места с координатами, geo-поиск поблизости и избранное.
 */
@SpringBootApplication
class PlacesApplication

/** Запускает Spring Boot приложение places-service. */
fun main(args: Array<String>) {
    runApplication<PlacesApplication>(*args)
}

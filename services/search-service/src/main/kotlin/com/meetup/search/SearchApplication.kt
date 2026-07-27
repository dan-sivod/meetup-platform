package com.meetup.search

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа search-service — сервиса поиска.
 * Индексирует людей, встречи и места по доменным событиям.
 */
@SpringBootApplication
class SearchApplication

/** Запускает Spring Boot приложение search-service. */
fun main(args: Array<String>) {
    runApplication<SearchApplication>(*args)
}

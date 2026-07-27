package com.meetup.media

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа media-service — сервиса фото и видео встреч.
 * Хранит файлы альбомов; для разработки — локальный диск, в проде — S3.
 */
@SpringBootApplication
class MediaApplication

/** Запускает Spring Boot приложение media-service. */
fun main(args: Array<String>) {
    runApplication<MediaApplication>(*args)
}

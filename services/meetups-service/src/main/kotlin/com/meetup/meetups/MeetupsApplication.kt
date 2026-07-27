package com.meetup.meetups

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

/**
 * Точка входа meetups-service — ядра домена «встреча».
 * Владеет агрегатом Meetup: приглашения, RSVP, подтверждение и отмена.
 */
@SpringBootApplication
class MeetupsApplication {
    /**
     * HTTP-клиент для синхронных вызовов friends-service
     * (ACL-проверка «можно ли пригласить пользователя»).
     *
     * @param baseUrl базовый адрес friends-service из конфигурации.
     */
    @Bean
    fun friendsRestClient(@Value("\${clients.friends-base-url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()
}

/** Запускает Spring Boot приложение meetups-service. */
fun main(args: Array<String>) {
    runApplication<MeetupsApplication>(*args)
}

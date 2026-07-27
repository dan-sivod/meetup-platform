package com.meetup.meetups

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

@SpringBootApplication
class MeetupsApplication {
    @Bean
    fun friendsRestClient(@Value("\${clients.friends-base-url}") baseUrl: String): RestClient =
        RestClient.builder().baseUrl(baseUrl).build()
}

fun main(args: Array<String>) {
    runApplication<MeetupsApplication>(*args)
}

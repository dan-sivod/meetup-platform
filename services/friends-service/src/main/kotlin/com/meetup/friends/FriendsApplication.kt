package com.meetup.friends

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FriendsApplication

fun main(args: Array<String>) {
    runApplication<FriendsApplication>(*args)
}

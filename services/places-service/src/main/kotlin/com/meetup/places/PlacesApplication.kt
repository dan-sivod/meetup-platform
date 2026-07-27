package com.meetup.places

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PlacesApplication

fun main(args: Array<String>) {
    runApplication<PlacesApplication>(*args)
}

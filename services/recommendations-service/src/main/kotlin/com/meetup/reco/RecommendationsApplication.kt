package com.meetup.reco

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RecommendationsApplication

fun main(args: Array<String>) {
    runApplication<RecommendationsApplication>(*args)
}

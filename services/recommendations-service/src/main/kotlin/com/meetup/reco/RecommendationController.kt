package com.meetup.reco

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/recommendations")
class RecommendationController(private val service: RecommendationService) {

    @GetMapping("/places")
    fun places(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(defaultValue = "10") limit: Int,
    ): List<PlaceScore> = service.recommendPlaces(userId, limit.coerceIn(1, 50))
}

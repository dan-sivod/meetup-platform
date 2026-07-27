package com.meetup.reco

import com.meetup.common.events.MeetupConfirmed
import com.meetup.common.events.PlaceFavorited
import com.meetup.common.events.Topics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PlaceScore(val placeId: UUID, val score: Double)

/**
 * Event-sourced in-memory scoring: favorites and confirmed meetups raise a
 * place's score per user and globally. The store is rebuilt from the topic
 * on restart (auto-offset-reset: earliest); production would persist
 * projections or delegate to an ML ranker.
 */
@Service
class RecommendationService {
    private val userScores = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Double>>()
    private val globalScores = ConcurrentHashMap<UUID, Double>()

    @KafkaListener(topics = [Topics.PLACE_FAVORITED])
    fun onPlaceFavorited(event: PlaceFavorited) {
        addScore(event.userId, event.placeId, weight = 3.0)
    }

    @KafkaListener(topics = [Topics.MEETUP_CONFIRMED])
    fun onMeetupConfirmed(event: MeetupConfirmed) {
        val placeId = event.placeId ?: return
        event.participantIds.forEach { addScore(it, placeId, weight = 1.0) }
    }

    fun recommendPlaces(userId: UUID, limit: Int): List<PlaceScore> {
        val personal = userScores[userId].orEmpty()
        // Personal signal dominates; global popularity breaks ties and fills gaps.
        val combined = (personal.keys + globalScores.keys).associateWith { placeId ->
            (personal[placeId] ?: 0.0) * 2.0 + (globalScores[placeId] ?: 0.0)
        }
        return combined.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { PlaceScore(it.key, it.value) }
    }

    private fun addScore(userId: UUID, placeId: UUID, weight: Double) {
        userScores.computeIfAbsent(userId) { ConcurrentHashMap() }
            .merge(placeId, weight) { a, b -> a + b }
        globalScores.merge(placeId, weight) { a, b -> a + b }
    }
}

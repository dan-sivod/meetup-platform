package com.meetup.reco

import com.meetup.common.events.MeetupConfirmed
import com.meetup.common.events.PlaceFavorited
import com.meetup.common.events.Topics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Место с рекомендательным баллом.
 *
 * @property placeId идентификатор места.
 * @property score итоговый балл: чем выше, тем релевантнее.
 */
data class PlaceScore(val placeId: UUID, val score: Double)

/**
 * Event-sourced скоринг мест в памяти: избранное и подтверждённые встречи
 * повышают балл места для пользователя и глобально. Хранилище
 * восстанавливается перечитыванием топиков при рестарте
 * (auto-offset-reset: earliest); в продакшене проекции персистятся
 * или заменяются ML-ранкером.
 */
@Service
class RecommendationService {
    /** Персональные баллы: пользователь → (место → балл). */
    private val userScores = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, Double>>()

    /** Глобальная популярность мест: место → суммарный балл. */
    private val globalScores = ConcurrentHashMap<UUID, Double>()

    /** Сильный сигнал: пользователь добавил место в избранное (вес 3). */
    @KafkaListener(topics = [Topics.PLACE_FAVORITED])
    fun onPlaceFavorited(event: PlaceFavorited) {
        addScore(event.userId, event.placeId, weight = 3.0)
    }

    /** Слабый сигнал: встреча в этом месте подтвердилась (вес 1 каждому участнику). */
    @KafkaListener(topics = [Topics.MEETUP_CONFIRMED])
    fun onMeetupConfirmed(event: MeetupConfirmed) {
        val placeId = event.placeId ?: return
        event.participantIds.forEach { addScore(it, placeId, weight = 1.0) }
    }

    /**
     * Топ-[limit] мест для пользователя [userId].
     * Персональный сигнал доминирует (умножается на 2); глобальная
     * популярность добивает пробелы и разрешает ничьи.
     */
    fun recommendPlaces(userId: UUID, limit: Int): List<PlaceScore> {
        val personal = userScores[userId].orEmpty()
        val combined = (personal.keys + globalScores.keys).associateWith { placeId ->
            (personal[placeId] ?: 0.0) * 2.0 + (globalScores[placeId] ?: 0.0)
        }
        return combined.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { PlaceScore(it.key, it.value) }
    }

    /** Прибавляет [weight] к персональному и глобальному баллу места. */
    private fun addScore(userId: UUID, placeId: UUID, weight: Double) {
        userScores.computeIfAbsent(userId) { ConcurrentHashMap() }
            .merge(placeId, weight) { a, b -> a + b }
        globalScores.merge(placeId, weight) { a, b -> a + b }
    }
}

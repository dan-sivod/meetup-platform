package com.meetup.meetups

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * Тело запроса на создание встречи.
 *
 * @property title название встречи.
 * @property description описание; может отсутствовать.
 * @property invitedUserIds пользователи, приглашаемые сразу при создании.
 */
data class CreateMeetupRequest(
    val title: String,
    val description: String? = null,
    val invitedUserIds: List<UUID> = emptyList(),
)

/**
 * Тело запроса на приглашение.
 *
 * @property userId приглашаемый пользователь.
 */
data class InviteRequest(val userId: UUID)

/**
 * Тело ответа на приглашение.
 *
 * @property status новый RSVP-статус участника.
 */
data class RsvpRequest(val status: RsvpStatus)

/**
 * Тело запроса на подтверждение встречи.
 *
 * @property startsAt время начала (UTC).
 * @property placeId выбранное место; может отсутствовать.
 */
data class ConfirmRequest(val startsAt: Instant, val placeId: UUID? = null)

/**
 * HTTP-эндпоинты агрегата «встреча». Личность вызывающего берётся
 * из доверенного заголовка X-User-Id, проставленного gateway.
 *
 * @property service доменная логика встреч.
 */
@RestController
@RequestMapping("/meetups")
class MeetupController(private val service: MeetupService) {

    /** Создаёт встречу от имени текущего пользователя (он становится организатором). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateMeetupRequest,
    ): Meetup = service.create(userId, request.title, request.description, request.invitedUserIds)

    /** Приглашает пользователя на встречу (только организатор, только друзей). */
    @PostMapping("/{id}/invite")
    fun invite(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: InviteRequest,
    ): Participant = service.invite(id, userId, request.userId)

    /** Фиксирует ответ текущего пользователя на приглашение. */
    @PutMapping("/{id}/rsvp")
    fun rsvp(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: RsvpRequest,
    ): Participant = service.rsvp(id, userId, request.status)

    /** Подтверждает время и место встречи (только организатор). */
    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ConfirmRequest,
    ): Meetup = service.confirm(id, userId, request.startsAt, request.placeId)

    /** Отменяет встречу (только организатор). */
    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): Meetup =
        service.cancel(id, userId)

    /** Лента встреч текущего пользователя. */
    @GetMapping("/feed")
    fun feed(@RequestHeader("X-User-Id") userId: UUID): List<Meetup> = service.feed(userId)

    /** Возвращает встречу по идентификатору. */
    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): Meetup = service.find(id)

    /** Список участников встречи с их RSVP-статусами. */
    @GetMapping("/{id}/participants")
    fun participants(@PathVariable id: UUID): List<Participant> = service.participantsOf(id)
}

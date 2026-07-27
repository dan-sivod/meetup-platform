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

data class CreateMeetupRequest(
    val title: String,
    val description: String? = null,
    val invitedUserIds: List<UUID> = emptyList(),
)

data class InviteRequest(val userId: UUID)

data class RsvpRequest(val status: RsvpStatus)

data class ConfirmRequest(val startsAt: Instant, val placeId: UUID? = null)

@RestController
@RequestMapping("/meetups")
class MeetupController(private val service: MeetupService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateMeetupRequest,
    ): Meetup = service.create(userId, request.title, request.description, request.invitedUserIds)

    @PostMapping("/{id}/invite")
    fun invite(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: InviteRequest,
    ): Participant = service.invite(id, userId, request.userId)

    @PutMapping("/{id}/rsvp")
    fun rsvp(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: RsvpRequest,
    ): Participant = service.rsvp(id, userId, request.status)

    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ConfirmRequest,
    ): Meetup = service.confirm(id, userId, request.startsAt, request.placeId)

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): Meetup =
        service.cancel(id, userId)

    @GetMapping("/feed")
    fun feed(@RequestHeader("X-User-Id") userId: UUID): List<Meetup> = service.feed(userId)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): Meetup = service.find(id)

    @GetMapping("/{id}/participants")
    fun participants(@PathVariable id: UUID): List<Participant> = service.participantsOf(id)
}

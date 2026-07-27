package com.meetup.meetups

import com.meetup.common.events.MeetupCancelled
import com.meetup.common.events.MeetupConfirmed
import com.meetup.common.events.MeetupCreated
import com.meetup.common.events.RsvpChanged
import com.meetup.common.events.Topics
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class MeetupService(
    private val meetups: MeetupRepository,
    private val participants: ParticipantRepository,
    private val friendsClient: FriendsClient,
    private val kafka: KafkaTemplate<String, Any>,
) {
    @Transactional
    fun create(hostId: UUID, title: String, description: String?, invitedUserIds: List<UUID>): Meetup {
        val meetup = meetups.save(Meetup(hostId = hostId, title = title, description = description))
        participants.save(Participant(meetupId = meetup.id, userId = hostId, rsvp = RsvpStatus.GOING))
        invitedUserIds.distinct().filter { it != hostId }.forEach { invite(meetup, hostId, it) }
        kafka.send(
            Topics.MEETUP_CREATED,
            meetup.id.toString(),
            MeetupCreated(meetup.id, hostId, meetup.title, invitedUserIds),
        )
        return meetup
    }

    @Transactional
    fun invite(meetupId: UUID, callerId: UUID, userId: UUID): Participant {
        val meetup = find(meetupId)
        requireHost(meetup, callerId)
        requireActive(meetup)
        return invite(meetup, callerId, userId)
    }

    @Transactional
    fun rsvp(meetupId: UUID, userId: UUID, status: RsvpStatus): Participant {
        val meetup = find(meetupId)
        requireActive(meetup)
        val participant = participants.findByMeetupIdAndUserId(meetupId, userId)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not invited to this meetup")
        participant.rsvp = status
        participant.respondedAt = Instant.now()
        val saved = participants.save(participant)
        kafka.send(
            Topics.RSVP_CHANGED,
            meetupId.toString(),
            RsvpChanged(meetupId, meetup.title, meetup.hostId, userId, status.name),
        )
        return saved
    }

    @Transactional
    fun confirm(meetupId: UUID, callerId: UUID, startsAt: Instant, placeId: UUID?): Meetup {
        val meetup = find(meetupId)
        requireHost(meetup, callerId)
        requireActive(meetup)
        meetup.status = MeetupStatus.CONFIRMED
        meetup.startsAt = startsAt
        meetup.placeId = placeId
        val saved = meetups.save(meetup)
        kafka.send(
            Topics.MEETUP_CONFIRMED,
            meetupId.toString(),
            MeetupConfirmed(
                meetupId = meetupId,
                hostId = meetup.hostId,
                title = meetup.title,
                startsAt = startsAt,
                placeId = placeId,
                participantIds = activeParticipantIds(meetupId),
            ),
        )
        return saved
    }

    @Transactional
    fun cancel(meetupId: UUID, callerId: UUID): Meetup {
        val meetup = find(meetupId)
        requireHost(meetup, callerId)
        requireActive(meetup)
        meetup.status = MeetupStatus.CANCELLED
        val saved = meetups.save(meetup)
        kafka.send(
            Topics.MEETUP_CANCELLED,
            meetupId.toString(),
            MeetupCancelled(meetupId, meetup.hostId, meetup.title, activeParticipantIds(meetupId)),
        )
        return saved
    }

    fun feed(userId: UUID): List<Meetup> = meetups.findFeedFor(userId)

    fun find(meetupId: UUID): Meetup = meetups.findById(meetupId).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found")
    }

    fun participantsOf(meetupId: UUID): List<Participant> = participants.findByMeetupId(meetupId)

    private fun invite(meetup: Meetup, hostId: UUID, userId: UUID): Participant {
        if (!friendsClient.areFriends(hostId, userId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Can only invite friends: $userId")
        }
        if (participants.existsByMeetupIdAndUserId(meetup.id, userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already invited: $userId")
        }
        return participants.save(Participant(meetupId = meetup.id, userId = userId))
    }

    private fun activeParticipantIds(meetupId: UUID): List<UUID> =
        participants.findByMeetupId(meetupId)
            .filter { it.rsvp != RsvpStatus.DECLINED }
            .map { it.userId }

    private fun requireHost(meetup: Meetup, callerId: UUID) {
        if (meetup.hostId != callerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can do this")
        }
    }

    private fun requireActive(meetup: Meetup) {
        if (meetup.status == MeetupStatus.CANCELLED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Meetup is cancelled")
        }
    }
}

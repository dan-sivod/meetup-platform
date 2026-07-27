package com.meetup.meetups

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

enum class MeetupStatus { PLANNED, CONFIRMED, CANCELLED }

enum class RsvpStatus { INVITED, GOING, MAYBE, DECLINED }

@Entity
@Table(name = "meetups")
class Meetup(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val hostId: UUID,
    @Column(nullable = false)
    var title: String,
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MeetupStatus = MeetupStatus.PLANNED,
    var startsAt: Instant? = null,
    var placeId: UUID? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "participants",
    uniqueConstraints = [UniqueConstraint(columnNames = ["meetupId", "userId"])],
)
class Participant(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val meetupId: UUID,
    @Column(nullable = false)
    val userId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var rsvp: RsvpStatus = RsvpStatus.INVITED,
    var respondedAt: Instant? = null,
)

interface MeetupRepository : JpaRepository<Meetup, UUID> {
    @Query(
        """
        select m from Meetup m
        where m.status <> com.meetup.meetups.MeetupStatus.CANCELLED
          and (m.hostId = :userId or m.id in (
              select p.meetupId from Participant p where p.userId = :userId
          ))
        order by m.startsAt asc nulls last
        """,
    )
    fun findFeedFor(userId: UUID): List<Meetup>
}

interface ParticipantRepository : JpaRepository<Participant, UUID> {
    fun findByMeetupId(meetupId: UUID): List<Participant>
    fun findByMeetupIdAndUserId(meetupId: UUID, userId: UUID): Participant?
    fun existsByMeetupIdAndUserId(meetupId: UUID, userId: UUID): Boolean
}

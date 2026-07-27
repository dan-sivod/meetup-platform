package com.meetup.scheduling

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

enum class PollStatus { OPEN, CLOSED }

@Entity
@Table(name = "polls")
class Poll(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val meetupId: UUID,
    @Column(nullable = false)
    val createdBy: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PollStatus = PollStatus.OPEN,
    var selectedOptionId: UUID? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "poll_options")
class PollOption(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val pollId: UUID,
    /** Stored in UTC; clients render in the participant's timezone. */
    @Column(nullable = false)
    val startsAt: Instant,
    val endsAt: Instant? = null,
)

@Entity
@Table(
    name = "poll_votes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["optionId", "userId"])],
)
class PollVote(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val optionId: UUID,
    @Column(nullable = false)
    val userId: UUID,
    val votedAt: Instant = Instant.now(),
)

interface PollRepository : JpaRepository<Poll, UUID> {
    fun findByMeetupId(meetupId: UUID): List<Poll>
}

interface PollOptionRepository : JpaRepository<PollOption, UUID> {
    fun findByPollId(pollId: UUID): List<PollOption>
}

interface PollVoteRepository : JpaRepository<PollVote, UUID> {
    fun findByOptionIdIn(optionIds: List<UUID>): List<PollVote>
    fun existsByOptionIdAndUserId(optionId: UUID, userId: UUID): Boolean
    fun deleteByOptionIdAndUserId(optionId: UUID, userId: UUID)
}

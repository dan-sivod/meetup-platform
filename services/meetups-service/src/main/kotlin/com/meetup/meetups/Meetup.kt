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

/** Жизненный цикл встречи. */
enum class MeetupStatus {
    /** Встреча создана, время и место ещё обсуждаются. */
    PLANNED,

    /** Организатор зафиксировал время (и, возможно, место). */
    CONFIRMED,

    /** Встреча отменена; дальнейшие изменения запрещены. */
    CANCELLED,
}

/** Ответ участника на приглашение (RSVP). */
enum class RsvpStatus {
    /** Приглашён, но ещё не ответил. */
    INVITED,

    /** Придёт. */
    GOING,

    /** Возможно придёт. */
    MAYBE,

    /** Не придёт. */
    DECLINED,
}

/**
 * Агрегат «встреча». Ссылается на место и участников по UUID —
 * данные мест и профилей живут в своих сервисах.
 *
 * @property id идентификатор встречи.
 * @property hostId организатор; только он может приглашать, подтверждать и отменять.
 * @property title название встречи.
 * @property description описание; может отсутствовать.
 * @property status текущий статус жизненного цикла.
 * @property startsAt время начала (UTC); null, пока встреча не подтверждена.
 * @property placeId выбранное место из places-service; null, если не выбрано.
 * @property createdAt момент создания встречи.
 */
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

/**
 * Участие пользователя во встрече и его текущий ответ.
 *
 * @property id идентификатор записи об участии.
 * @property meetupId встреча, к которой относится запись.
 * @property userId приглашённый пользователь.
 * @property rsvp текущий ответ на приглашение.
 * @property respondedAt момент последнего ответа; null, если ответа ещё не было.
 */
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

/** Репозиторий встреч. */
interface MeetupRepository : JpaRepository<Meetup, UUID> {
    /**
     * Лента пользователя: неотменённые встречи, где он организатор
     * или участник, отсортированные по времени начала.
     */
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

/** Репозиторий записей об участии. */
interface ParticipantRepository : JpaRepository<Participant, UUID> {
    /** Все участники встречи. */
    fun findByMeetupId(meetupId: UUID): List<Participant>

    /** Запись об участии конкретного пользователя; null, если он не приглашён. */
    fun findByMeetupIdAndUserId(meetupId: UUID, userId: UUID): Participant?

    /** Проверяет, приглашён ли пользователь на встречу. */
    fun existsByMeetupIdAndUserId(meetupId: UUID, userId: UUID): Boolean
}

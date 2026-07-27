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

/** Состояние опроса по выбору времени. */
enum class PollStatus {
    /** Опрос открыт, участники голосуют. */
    OPEN,

    /** Опрос закрыт, победивший слот зафиксирован. */
    CLOSED,
}

/**
 * Опрос по выбору времени для встречи.
 *
 * @property id идентификатор опроса.
 * @property meetupId встреча, для которой выбирается время.
 * @property createdBy автор опроса (только он может его закрыть).
 * @property status открыт или закрыт.
 * @property selectedOptionId победивший слот; null, пока опрос открыт.
 * @property createdAt момент создания опроса.
 */
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

/**
 * Вариант ответа опроса — конкретный временной слот.
 *
 * @property id идентификатор варианта.
 * @property pollId опрос, к которому относится слот.
 * @property startsAt начало слота в UTC; клиенты показывают его в часовом поясе участника.
 * @property endsAt конец слота; может отсутствовать для «открытых» вариантов.
 */
@Entity
@Table(name = "poll_options")
class PollOption(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val pollId: UUID,
    @Column(nullable = false)
    val startsAt: Instant,
    val endsAt: Instant? = null,
)

/**
 * Голос участника за временной слот. Пара (слот, пользователь) уникальна —
 * повторное голосование не создаёт дубликата.
 *
 * @property id идентификатор голоса.
 * @property optionId слот, за который отдан голос.
 * @property userId проголосовавший участник.
 * @property votedAt момент голосования.
 */
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

/** Репозиторий опросов. */
interface PollRepository : JpaRepository<Poll, UUID> {
    /** Все опросы, созданные для встречи. */
    fun findByMeetupId(meetupId: UUID): List<Poll>
}

/** Репозиторий временных слотов. */
interface PollOptionRepository : JpaRepository<PollOption, UUID> {
    /** Все слоты опроса. */
    fun findByPollId(pollId: UUID): List<PollOption>
}

/** Репозиторий голосов. */
interface PollVoteRepository : JpaRepository<PollVote, UUID> {
    /** Голоса по списку слотов — для подсчёта результатов одним запросом. */
    fun findByOptionIdIn(optionIds: List<UUID>): List<PollVote>

    /** Проверяет, голосовал ли пользователь за слот. */
    fun existsByOptionIdAndUserId(optionId: UUID, userId: UUID): Boolean

    /** Отзывает голос пользователя за слот. */
    fun deleteByOptionIdAndUserId(optionId: UUID, userId: UUID)
}

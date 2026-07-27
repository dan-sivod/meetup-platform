package com.meetup.scheduling

import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/**
 * Тело запроса на создание опроса.
 *
 * @property meetupId встреча, для которой выбирается время.
 * @property options предлагаемые временные слоты.
 */
data class CreatePollRequest(val meetupId: UUID, val options: List<SlotOption>) {
    /**
     * Предлагаемый слот.
     *
     * @property startsAt начало слота (UTC).
     * @property endsAt конец слота; может отсутствовать.
     */
    data class SlotOption(val startsAt: Instant, val endsAt: Instant? = null)
}

/**
 * Результат голосования по одному слоту.
 *
 * @property optionId идентификатор слота.
 * @property startsAt начало слота.
 * @property endsAt конец слота; может отсутствовать.
 * @property votes количество голосов.
 * @property voterIds кто проголосовал за этот слот.
 */
data class OptionResult(
    val optionId: UUID,
    val startsAt: Instant,
    val endsAt: Instant?,
    val votes: Int,
    val voterIds: List<UUID>,
)

/**
 * Сводные результаты опроса.
 *
 * @property pollId идентификатор опроса.
 * @property meetupId встреча, для которой проводится опрос.
 * @property status открыт или закрыт.
 * @property selectedOptionId победивший слот; null, пока опрос открыт.
 * @property options результаты по слотам, отсортированные по числу голосов.
 */
data class PollResults(
    val pollId: UUID,
    val meetupId: UUID,
    val status: PollStatus,
    val selectedOptionId: UUID?,
    val options: List<OptionResult>,
)

/**
 * HTTP-эндпоинты опросов по выбору времени: создание, голосование,
 * отзыв голоса, закрытие с фиксацией победителя.
 *
 * @property polls репозиторий опросов.
 * @property options репозиторий слотов.
 * @property votes репозиторий голосов.
 */
@RestController
@RequestMapping("/polls")
class PollController(
    private val polls: PollRepository,
    private val options: PollOptionRepository,
    private val votes: PollVoteRepository,
) {
    /**
     * Создаёт опрос с набором слотов.
     *
     * @throws ResponseStatusException 400, если не передано ни одного слота.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreatePollRequest,
    ): PollResults {
        if (request.options.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll needs at least one option")
        }
        val poll = polls.save(Poll(meetupId = request.meetupId, createdBy = userId))
        request.options.forEach {
            options.save(PollOption(pollId = poll.id, startsAt = it.startsAt, endsAt = it.endsAt))
        }
        return results(poll.id)
    }

    /**
     * Голос текущего пользователя за слот. Идемпотентен: повторный голос
     * за тот же слот не меняет результат.
     *
     * @throws ResponseStatusException 404 — слот не найден;
     * 400 — слот принадлежит другому опросу; 409 — опрос закрыт.
     */
    @PostMapping("/{pollId}/options/{optionId}/votes")
    fun vote(
        @PathVariable pollId: UUID,
        @PathVariable optionId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): PollResults {
        val poll = findOpen(pollId)
        val option = options.findById(optionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found")
        }
        if (option.pollId != poll.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Option belongs to another poll")
        }
        if (!votes.existsByOptionIdAndUserId(optionId, userId)) {
            votes.save(PollVote(optionId = optionId, userId = userId))
        }
        return results(pollId)
    }

    /** Отзывает голос текущего пользователя за слот (пока опрос открыт). */
    @DeleteMapping("/{pollId}/options/{optionId}/votes")
    @Transactional
    fun unvote(
        @PathVariable pollId: UUID,
        @PathVariable optionId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): PollResults {
        findOpen(pollId)
        votes.deleteByOptionIdAndUserId(optionId, userId)
        return results(pollId)
    }

    /**
     * Закрывает опрос и фиксирует слот с максимумом голосов.
     * Доступно только автору опроса.
     *
     * @throws ResponseStatusException 403 — закрывает не автор;
     * 409 — опрос уже закрыт или не содержит слотов.
     */
    @PostMapping("/{pollId}/close")
    fun close(@PathVariable pollId: UUID, @RequestHeader("X-User-Id") userId: UUID): PollResults {
        val poll = findOpen(pollId)
        if (poll.createdBy != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the poll creator can close it")
        }
        val winner = results(pollId).options.maxByOrNull { it.votes }
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Poll has no options")
        poll.status = PollStatus.CLOSED
        poll.selectedOptionId = winner.optionId
        polls.save(poll)
        return results(pollId)
    }

    /** Текущие результаты опроса. */
    @GetMapping("/{pollId}")
    fun get(@PathVariable pollId: UUID): PollResults = results(pollId)

    /** Все опросы встречи с результатами. */
    @GetMapping
    fun byMeetup(@RequestParam meetupId: UUID): List<PollResults> =
        polls.findByMeetupId(meetupId).map { results(it.id) }

    /**
     * Возвращает опрос, убедившись, что он открыт.
     *
     * @throws ResponseStatusException 404 — опрос не найден; 409 — закрыт.
     */
    private fun findOpen(pollId: UUID): Poll {
        val poll = polls.findById(pollId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found")
        }
        if (poll.status != PollStatus.OPEN) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Poll is closed")
        }
        return poll
    }

    /** Собирает сводные результаты: голоса группируются по слотам одним запросом. */
    private fun results(pollId: UUID): PollResults {
        val poll = polls.findById(pollId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found")
        }
        val pollOptions = options.findByPollId(pollId)
        val votesByOption = votes.findByOptionIdIn(pollOptions.map { it.id }).groupBy { it.optionId }
        return PollResults(
            pollId = poll.id,
            meetupId = poll.meetupId,
            status = poll.status,
            selectedOptionId = poll.selectedOptionId,
            options = pollOptions.map { option ->
                val optionVotes = votesByOption[option.id].orEmpty()
                OptionResult(
                    optionId = option.id,
                    startsAt = option.startsAt,
                    endsAt = option.endsAt,
                    votes = optionVotes.size,
                    voterIds = optionVotes.map { it.userId },
                )
            }.sortedByDescending { it.votes },
        )
    }
}

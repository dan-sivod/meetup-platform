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

data class CreatePollRequest(val meetupId: UUID, val options: List<SlotOption>) {
    data class SlotOption(val startsAt: Instant, val endsAt: Instant? = null)
}

data class OptionResult(
    val optionId: UUID,
    val startsAt: Instant,
    val endsAt: Instant?,
    val votes: Int,
    val voterIds: List<UUID>,
)

data class PollResults(
    val pollId: UUID,
    val meetupId: UUID,
    val status: PollStatus,
    val selectedOptionId: UUID?,
    val options: List<OptionResult>,
)

@RestController
@RequestMapping("/polls")
class PollController(
    private val polls: PollRepository,
    private val options: PollOptionRepository,
    private val votes: PollVoteRepository,
) {
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

    @GetMapping("/{pollId}")
    fun get(@PathVariable pollId: UUID): PollResults = results(pollId)

    @GetMapping
    fun byMeetup(@RequestParam meetupId: UUID): List<PollResults> =
        polls.findByMeetupId(meetupId).map { results(it.id) }

    private fun findOpen(pollId: UUID): Poll {
        val poll = polls.findById(pollId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found")
        }
        if (poll.status != PollStatus.OPEN) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Poll is closed")
        }
        return poll
    }

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

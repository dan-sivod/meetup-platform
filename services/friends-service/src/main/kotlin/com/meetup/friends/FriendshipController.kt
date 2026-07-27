package com.meetup.friends

import com.meetup.common.events.FriendshipAccepted
import com.meetup.common.events.FriendshipRequested
import com.meetup.common.events.Topics
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
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
 * Тело заявки в друзья.
 *
 * @property addresseeId пользователь, которому отправляется заявка.
 */
data class FriendRequestBody(val addresseeId: UUID)

/**
 * Тело запроса на блокировку.
 *
 * @property userId пользователь, которого нужно заблокировать.
 */
data class BlockRequestBody(val userId: UUID)

/**
 * Ответ ACL-проверки дружбы.
 *
 * @property friends true, если пользователи — подтверждённые друзья.
 */
data class FriendCheckResponse(val friends: Boolean)

/**
 * HTTP-эндпоинты графа дружбы: заявки, ответы, блокировки, списки
 * и внутренняя ACL-проверка для других сервисов.
 *
 * @property friendships репозиторий связей.
 * @property kafka продюсер событий friendship.requested / friendship.accepted.
 */
@RestController
@RequestMapping("/friendships")
class FriendshipController(
    private val friendships: FriendshipRepository,
    private val kafka: KafkaTemplate<String, Any>,
) {
    /**
     * Отправляет заявку в друзья от текущего пользователя.
     * Публикует событие friendship.requested (уведомление адресату).
     *
     * @throws ResponseStatusException 400 — заявка самому себе;
     * 403 — между пользователями есть блокировка;
     * 409 — связь уже существует или ждёт ответа.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun request(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody body: FriendRequestBody,
    ): Friendship {
        if (userId == body.addresseeId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot befriend yourself")
        }
        val existing = friendships.findBetween(userId, body.addresseeId)
        if (existing.any { it.status == FriendshipStatus.BLOCKED }) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Blocked")
        }
        if (existing.any { it.status == FriendshipStatus.PENDING || it.status == FriendshipStatus.ACCEPTED }) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists or is pending")
        }
        val friendship = friendships.save(Friendship(requesterId = userId, addresseeId = body.addresseeId))
        kafka.send(
            Topics.FRIENDSHIP_REQUESTED,
            friendship.id.toString(),
            FriendshipRequested(friendship.id, friendship.requesterId, friendship.addresseeId),
        )
        return friendship
    }

    /**
     * Принимает входящую заявку (только адресат) и публикует
     * событие friendship.accepted.
     */
    @PostMapping("/{id}/accept")
    fun accept(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): Friendship {
        val friendship = respond(id, userId, FriendshipStatus.ACCEPTED)
        kafka.send(
            Topics.FRIENDSHIP_ACCEPTED,
            friendship.id.toString(),
            FriendshipAccepted(friendship.id, friendship.requesterId, friendship.addresseeId),
        )
        return friendship
    }

    /** Отклоняет входящую заявку (только адресат). */
    @PostMapping("/{id}/decline")
    fun decline(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): Friendship =
        respond(id, userId, FriendshipStatus.DECLINED)

    /**
     * Блокирует пользователя: удаляет существующие связи между парой
     * и создаёт запись BLOCKED, запрещающую новые заявки.
     */
    @PostMapping("/block")
    fun block(@RequestHeader("X-User-Id") userId: UUID, @RequestBody body: BlockRequestBody): Friendship {
        friendships.deleteAll(friendships.findBetween(userId, body.userId))
        return friendships.save(
            Friendship(
                requesterId = userId,
                addresseeId = body.userId,
                status = FriendshipStatus.BLOCKED,
                respondedAt = Instant.now(),
            ),
        )
    }

    /** Возвращает идентификаторы всех подтверждённых друзей текущего пользователя. */
    @GetMapping("/friends")
    fun friends(@RequestHeader("X-User-Id") userId: UUID): List<UUID> =
        friendships.findAcceptedFor(userId)
            .map { if (it.requesterId == userId) it.addresseeId else it.requesterId }

    /** Возвращает входящие заявки, ожидающие ответа текущего пользователя. */
    @GetMapping("/pending")
    fun pending(@RequestHeader("X-User-Id") userId: UUID): List<Friendship> =
        friendships.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING)

    /**
     * Внутренняя ACL-проверка: являются ли [userA] и [userB] друзьями.
     * Используется meetups-service перед приглашением на встречу.
     */
    @GetMapping("/check")
    fun check(@RequestParam userA: UUID, @RequestParam userB: UUID): FriendCheckResponse =
        FriendCheckResponse(
            friendships.findBetween(userA, userB).any { it.status == FriendshipStatus.ACCEPTED },
        )

    /**
     * Общая логика ответа на заявку: проверяет права адресата и то,
     * что заявка ещё не обработана, затем выставляет статус [status].
     *
     * @throws ResponseStatusException 404 — заявка не найдена;
     * 403 — отвечает не адресат; 409 — заявка уже обработана.
     */
    private fun respond(id: UUID, userId: UUID, status: FriendshipStatus): Friendship {
        val friendship = friendships.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found")
        }
        if (friendship.addresseeId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the addressee can respond")
        }
        if (friendship.status != FriendshipStatus.PENDING) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Request already handled")
        }
        friendship.status = status
        friendship.respondedAt = Instant.now()
        return friendships.save(friendship)
    }
}

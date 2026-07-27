package com.meetup.meetups

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * Ответ friends-service на ACL-проверку.
 *
 * @property friends true, если пользователи — подтверждённые друзья.
 */
data class FriendCheckResponse(val friends: Boolean = false)

/**
 * Синхронный клиент friends-service. Правило домена: приглашать
 * на встречу можно только друзей, поэтому проверка выполняется
 * до создания записи об участии.
 *
 * @property friendsRestClient HTTP-клиент с базовым адресом friends-service.
 */
@Component
class FriendsClient(private val friendsRestClient: RestClient) {
    /** Возвращает true, если [userA] и [userB] — подтверждённые друзья. */
    fun areFriends(userA: UUID, userB: UUID): Boolean =
        friendsRestClient.get()
            .uri("/friendships/check?userA={a}&userB={b}", userA, userB)
            .retrieve()
            .body(FriendCheckResponse::class.java)
            ?.friends ?: false
}

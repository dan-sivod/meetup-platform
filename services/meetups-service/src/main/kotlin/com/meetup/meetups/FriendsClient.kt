package com.meetup.meetups

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

data class FriendCheckResponse(val friends: Boolean = false)

/** Synchronous ACL check against friends-service: only friends can be invited. */
@Component
class FriendsClient(private val friendsRestClient: RestClient) {
    fun areFriends(userA: UUID, userB: UUID): Boolean =
        friendsRestClient.get()
            .uri("/friendships/check?userA={a}&userB={b}", userA, userB)
            .retrieve()
            .body(FriendCheckResponse::class.java)
            ?.friends ?: false
}

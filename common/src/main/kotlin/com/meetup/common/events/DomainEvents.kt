package com.meetup.common.events

import java.time.Instant
import java.util.UUID

data class UserRegistered(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val occurredAt: Instant = Instant.now(),
)

data class FriendshipRequested(
    val friendshipId: UUID,
    val requesterId: UUID,
    val addresseeId: UUID,
    val occurredAt: Instant = Instant.now(),
)

data class FriendshipAccepted(
    val friendshipId: UUID,
    val requesterId: UUID,
    val addresseeId: UUID,
    val occurredAt: Instant = Instant.now(),
)

data class MeetupCreated(
    val meetupId: UUID,
    val hostId: UUID,
    val title: String,
    val invitedUserIds: List<UUID>,
    val occurredAt: Instant = Instant.now(),
)

data class MeetupConfirmed(
    val meetupId: UUID,
    val hostId: UUID,
    val title: String,
    val startsAt: Instant,
    val placeId: UUID?,
    val participantIds: List<UUID>,
    val occurredAt: Instant = Instant.now(),
)

data class MeetupCancelled(
    val meetupId: UUID,
    val hostId: UUID,
    val title: String,
    val participantIds: List<UUID>,
    val occurredAt: Instant = Instant.now(),
)

data class RsvpChanged(
    val meetupId: UUID,
    val meetupTitle: String,
    val hostId: UUID,
    val userId: UUID,
    val status: String,
    val occurredAt: Instant = Instant.now(),
)

data class MessageSent(
    val messageId: UUID,
    val roomId: UUID,
    val meetupId: UUID?,
    val senderId: UUID,
    val preview: String,
    val occurredAt: Instant = Instant.now(),
)

data class PlaceCreated(
    val placeId: UUID,
    val name: String,
    val category: String?,
    val occurredAt: Instant = Instant.now(),
)

data class PlaceFavorited(
    val placeId: UUID,
    val userId: UUID,
    val placeName: String,
    val category: String?,
    val occurredAt: Instant = Instant.now(),
)

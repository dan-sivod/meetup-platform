package com.meetup.notifications

import com.meetup.common.events.FriendshipAccepted
import com.meetup.common.events.FriendshipRequested
import com.meetup.common.events.MeetupCancelled
import com.meetup.common.events.MeetupConfirmed
import com.meetup.common.events.MeetupCreated
import com.meetup.common.events.RsvpChanged
import com.meetup.common.events.Topics
import com.meetup.common.events.UserRegistered
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Projects domain events into per-user in-app notifications.
 * Push/email channels would plug in here as additional delivery adapters.
 */
@Component
class NotificationEventsListener(private val notifications: NotificationRepository) {

    @KafkaListener(topics = [Topics.USER_REGISTERED])
    fun onUserRegistered(event: UserRegistered) {
        save(event.userId, "welcome", "Добро пожаловать в Meetup!", "Найдите друзей и создайте первую встречу.")
    }

    @KafkaListener(topics = [Topics.FRIENDSHIP_REQUESTED])
    fun onFriendshipRequested(event: FriendshipRequested) {
        save(event.addresseeId, "friend_request", "Новая заявка в друзья", refId = event.friendshipId)
    }

    @KafkaListener(topics = [Topics.FRIENDSHIP_ACCEPTED])
    fun onFriendshipAccepted(event: FriendshipAccepted) {
        save(event.requesterId, "friend_accepted", "Заявка в друзья принята", refId = event.friendshipId)
    }

    @KafkaListener(topics = [Topics.MEETUP_CREATED])
    fun onMeetupCreated(event: MeetupCreated) {
        event.invitedUserIds.forEach {
            save(it, "meetup_invite", "Вас пригласили: ${event.title}", refId = event.meetupId)
        }
    }

    @KafkaListener(topics = [Topics.MEETUP_CONFIRMED])
    fun onMeetupConfirmed(event: MeetupConfirmed) {
        event.participantIds.forEach {
            save(
                it,
                "meetup_confirmed",
                "Встреча подтверждена: ${event.title}",
                "Начало: ${event.startsAt}",
                event.meetupId,
            )
        }
    }

    @KafkaListener(topics = [Topics.MEETUP_CANCELLED])
    fun onMeetupCancelled(event: MeetupCancelled) {
        event.participantIds.filter { it != event.hostId }.forEach {
            save(it, "meetup_cancelled", "Встреча отменена: ${event.title}", refId = event.meetupId)
        }
    }

    @KafkaListener(topics = [Topics.RSVP_CHANGED])
    fun onRsvpChanged(event: RsvpChanged) {
        if (event.userId == event.hostId) return
        save(
            event.hostId,
            "rsvp_changed",
            "Ответ на «${event.meetupTitle}»: ${event.status}",
            refId = event.meetupId,
        )
    }

    private fun save(userId: UUID, type: String, title: String, body: String? = null, refId: UUID? = null) {
        notifications.save(Notification(userId = userId, type = type, title = title, body = body, refId = refId))
    }
}

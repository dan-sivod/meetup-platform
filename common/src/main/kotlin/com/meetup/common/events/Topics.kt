package com.meetup.common.events

/** Kafka topic names shared by all services. */
object Topics {
    const val USER_REGISTERED = "user.registered"
    const val FRIENDSHIP_REQUESTED = "friendship.requested"
    const val FRIENDSHIP_ACCEPTED = "friendship.accepted"
    const val MEETUP_CREATED = "meetup.created"
    const val MEETUP_CONFIRMED = "meetup.confirmed"
    const val MEETUP_CANCELLED = "meetup.cancelled"
    const val RSVP_CHANGED = "rsvp.changed"
    const val MESSAGE_SENT = "message.sent"
    const val PLACE_CREATED = "place.created"
    const val PLACE_FAVORITED = "place.favorited"
}

package com.meetup.common.events

/** Имена Kafka-топиков, общие для всех сервисов платформы. */
object Topics {
    /** Пользователь зарегистрировался в auth-service. */
    const val USER_REGISTERED = "user.registered"

    /** Отправлена заявка в друзья. */
    const val FRIENDSHIP_REQUESTED = "friendship.requested"

    /** Заявка в друзья принята. */
    const val FRIENDSHIP_ACCEPTED = "friendship.accepted"

    /** Создана новая встреча. */
    const val MEETUP_CREATED = "meetup.created"

    /** Встреча подтверждена: зафиксированы время и место. */
    const val MEETUP_CONFIRMED = "meetup.confirmed"

    /** Встреча отменена организатором. */
    const val MEETUP_CANCELLED = "meetup.cancelled"

    /** Участник изменил свой ответ на приглашение (RSVP). */
    const val RSVP_CHANGED = "rsvp.changed"

    /** Отправлено сообщение в чат. */
    const val MESSAGE_SENT = "message.sent"

    /** В каталог добавлено новое место. */
    const val PLACE_CREATED = "place.created"

    /** Пользователь добавил место в избранное. */
    const val PLACE_FAVORITED = "place.favorited"
}

package com.meetup.common.events

import java.time.Instant
import java.util.UUID

/**
 * Событие: пользователь зарегистрировался.
 *
 * @property userId идентификатор нового пользователя (общий для всей платформы).
 * @property email адрес электронной почты, указанный при регистрации.
 * @property displayName отображаемое имя пользователя.
 * @property occurredAt момент возникновения события.
 */
data class UserRegistered(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: отправлена заявка в друзья.
 *
 * @property friendshipId идентификатор записи о дружбе.
 * @property requesterId кто отправил заявку.
 * @property addresseeId кому адресована заявка.
 * @property occurredAt момент возникновения события.
 */
data class FriendshipRequested(
    val friendshipId: UUID,
    val requesterId: UUID,
    val addresseeId: UUID,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: заявка в друзья принята.
 *
 * @property friendshipId идентификатор записи о дружбе.
 * @property requesterId автор исходной заявки (получит уведомление).
 * @property addresseeId пользователь, принявший заявку.
 * @property occurredAt момент возникновения события.
 */
data class FriendshipAccepted(
    val friendshipId: UUID,
    val requesterId: UUID,
    val addresseeId: UUID,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: создана встреча. По нему chat-service создаёт комнату,
 * notifications-service рассылает приглашения, search-service индексирует.
 *
 * @property meetupId идентификатор встречи.
 * @property hostId организатор встречи.
 * @property title название встречи.
 * @property invitedUserIds приглашённые на момент создания пользователи.
 * @property occurredAt момент возникновения события.
 */
data class MeetupCreated(
    val meetupId: UUID,
    val hostId: UUID,
    val title: String,
    val invitedUserIds: List<UUID>,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: встреча подтверждена — организатор зафиксировал время и место.
 *
 * @property meetupId идентификатор встречи.
 * @property hostId организатор встречи.
 * @property title название встречи.
 * @property startsAt подтверждённое время начала (UTC).
 * @property placeId выбранное место; null, если встреча без привязки к месту.
 * @property participantIds участники, не отклонившие приглашение.
 * @property occurredAt момент возникновения события.
 */
data class MeetupConfirmed(
    val meetupId: UUID,
    val hostId: UUID,
    val title: String,
    val startsAt: Instant,
    val placeId: UUID?,
    val participantIds: List<UUID>,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: встреча отменена организатором.
 *
 * @property meetupId идентификатор встречи.
 * @property hostId организатор встречи.
 * @property title название встречи.
 * @property participantIds участники, которых нужно уведомить об отмене.
 * @property occurredAt момент возникновения события.
 */
data class MeetupCancelled(
    val meetupId: UUID,
    val hostId: UUID,
    val title: String,
    val participantIds: List<UUID>,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: участник изменил свой ответ на приглашение (RSVP).
 *
 * @property meetupId идентификатор встречи.
 * @property meetupTitle название встречи (для текста уведомления).
 * @property hostId организатор — адресат уведомления об изменении.
 * @property userId участник, изменивший ответ.
 * @property status новый статус ответа (GOING / MAYBE / DECLINED).
 * @property occurredAt момент возникновения события.
 */
data class RsvpChanged(
    val meetupId: UUID,
    val meetupTitle: String,
    val hostId: UUID,
    val userId: UUID,
    val status: String,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: отправлено сообщение в чат.
 *
 * @property messageId идентификатор сообщения.
 * @property roomId комната, в которую отправлено сообщение.
 * @property meetupId встреча, к которой привязана комната; null для личных чатов.
 * @property senderId автор сообщения.
 * @property preview первые символы текста для превью в уведомлении.
 * @property occurredAt момент возникновения события.
 */
data class MessageSent(
    val messageId: UUID,
    val roomId: UUID,
    val meetupId: UUID?,
    val senderId: UUID,
    val preview: String,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: в каталог добавлено новое место (индексируется search-service).
 *
 * @property placeId идентификатор места.
 * @property name название места.
 * @property category категория (кафе, парк и т.п.); может отсутствовать.
 * @property occurredAt момент возникновения события.
 */
data class PlaceCreated(
    val placeId: UUID,
    val name: String,
    val category: String?,
    val occurredAt: Instant = Instant.now(),
)

/**
 * Событие: пользователь добавил место в избранное
 * (сигнал для recommendations-service).
 *
 * @property placeId идентификатор места.
 * @property userId пользователь, добавивший место в избранное.
 * @property placeName название места.
 * @property category категория места; может отсутствовать.
 * @property occurredAt момент возникновения события.
 */
data class PlaceFavorited(
    val placeId: UUID,
    val userId: UUID,
    val placeName: String,
    val category: String?,
    val occurredAt: Instant = Instant.now(),
)

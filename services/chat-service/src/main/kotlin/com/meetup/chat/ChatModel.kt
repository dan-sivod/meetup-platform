package com.meetup.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

/**
 * Комната чата.
 *
 * @property id идентификатор комнаты.
 * @property meetupId встреча, к которой привязана комната; null для личных чатов.
 * @property title название комнаты (копия названия встречи).
 * @property createdAt момент создания комнаты.
 */
@Entity
@Table(name = "chat_rooms")
class ChatRoom(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(unique = true)
    val meetupId: UUID? = null,
    @Column(nullable = false)
    var title: String,
    val createdAt: Instant = Instant.now(),
)

/**
 * Сообщение в чате.
 *
 * @property id идентификатор сообщения.
 * @property roomId комната, в которую отправлено сообщение.
 * @property senderId автор сообщения.
 * @property text текст сообщения (до 4000 символов).
 * @property sentAt момент отправки.
 */
@Entity
@Table(name = "chat_messages")
class ChatMessage(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val roomId: UUID,
    @Column(nullable = false)
    val senderId: UUID,
    @Column(nullable = false, length = 4000)
    val text: String,
    val sentAt: Instant = Instant.now(),
)

/** Репозиторий комнат чата. */
interface ChatRoomRepository : JpaRepository<ChatRoom, UUID> {
    /** Комната, привязанная к встрече; null, если ещё не создана. */
    fun findByMeetupId(meetupId: UUID): ChatRoom?
}

/** Репозиторий сообщений. */
interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    /** Последние сообщения комнаты, новые первыми, с постраничным ограничением. */
    fun findByRoomIdOrderBySentAtDesc(roomId: UUID, pageable: Pageable): List<ChatMessage>
}

package com.meetup.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chat_rooms")
class ChatRoom(
    @Id
    val id: UUID = UUID.randomUUID(),
    /** Null for direct-message rooms; set for meetup rooms. */
    @Column(unique = true)
    val meetupId: UUID? = null,
    @Column(nullable = false)
    var title: String,
    val createdAt: Instant = Instant.now(),
)

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

interface ChatRoomRepository : JpaRepository<ChatRoom, UUID> {
    fun findByMeetupId(meetupId: UUID): ChatRoom?
}

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    fun findByRoomIdOrderBySentAtDesc(roomId: UUID, pageable: Pageable): List<ChatMessage>
}

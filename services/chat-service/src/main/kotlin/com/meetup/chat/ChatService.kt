package com.meetup.chat

import com.meetup.common.events.MeetupCreated
import com.meetup.common.events.MessageSent
import com.meetup.common.events.Topics
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ChatService(
    private val rooms: ChatRoomRepository,
    private val messages: ChatMessageRepository,
    private val kafka: KafkaTemplate<String, Any>,
    private val broker: SimpMessagingTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Every meetup automatically gets its own chat room. */
    @KafkaListener(topics = [Topics.MEETUP_CREATED])
    fun onMeetupCreated(event: MeetupCreated) {
        if (rooms.findByMeetupId(event.meetupId) != null) return
        val room = rooms.save(ChatRoom(meetupId = event.meetupId, title = event.title))
        log.info("Created chat room {} for meetup {}", room.id, event.meetupId)
    }

    fun postMessage(roomId: UUID, senderId: UUID, text: String): ChatMessage {
        val room = rooms.findById(roomId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")
        }
        val message = messages.save(ChatMessage(roomId = roomId, senderId = senderId, text = text))
        broker.convertAndSend("/topic/rooms/$roomId", message)
        kafka.send(
            Topics.MESSAGE_SENT,
            roomId.toString(),
            MessageSent(
                messageId = message.id,
                roomId = roomId,
                meetupId = room.meetupId,
                senderId = senderId,
                preview = text.take(80),
            ),
        )
        return message
    }
}

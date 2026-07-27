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

/**
 * Логика чата: создание комнат по событиям встреч и отправка сообщений
 * с realtime-рассылкой подписчикам.
 *
 * @property rooms репозиторий комнат.
 * @property messages репозиторий сообщений.
 * @property kafka продюсер события message.sent.
 * @property broker STOMP-брокер для рассылки сообщений подписчикам комнаты.
 */
@Service
class ChatService(
    private val rooms: ChatRoomRepository,
    private val messages: ChatMessageRepository,
    private val kafka: KafkaTemplate<String, Any>,
    private val broker: SimpMessagingTemplate,
) {
    /** Логгер компонента. */
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Создаёт комнату для новой встречи по событию meetup.created.
     * Идемпотентен: повторная доставка события не создаёт дубликат.
     */
    @KafkaListener(topics = [Topics.MEETUP_CREATED])
    fun onMeetupCreated(event: MeetupCreated) {
        if (rooms.findByMeetupId(event.meetupId) != null) return
        val room = rooms.save(ChatRoom(meetupId = event.meetupId, title = event.title))
        log.info("Created chat room {} for meetup {}", room.id, event.meetupId)
    }

    /**
     * Сохраняет сообщение, рассылает его подписчикам комнаты через
     * /topic/rooms/{roomId} и публикует message.sent в Kafka.
     *
     * @param roomId комната назначения.
     * @param senderId автор сообщения.
     * @param text текст сообщения.
     * @throws ResponseStatusException 404, если комната не найдена.
     */
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

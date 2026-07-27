package com.meetup.chat

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.stereotype.Controller
import java.util.UUID

/**
 * Тело REST-запроса на отправку сообщения.
 *
 * @property text текст сообщения.
 */
data class SendMessageRequest(val text: String)

/**
 * Сообщение, приходящее по WebSocket (STOMP).
 *
 * @property senderId автор сообщения (в проде берётся из JWT при handshake).
 * @property text текст сообщения.
 */
data class WsMessage(val senderId: UUID, val text: String)

/**
 * REST-эндпоинты чата: поиск комнаты по встрече, отправка и история.
 *
 * @property chatService логика отправки сообщений.
 * @property rooms репозиторий комнат.
 * @property messages репозиторий сообщений.
 */
@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
    private val rooms: ChatRoomRepository,
    private val messages: ChatMessageRepository,
) {
    /**
     * Возвращает комнату встречи.
     *
     * @throws ResponseStatusException 404, если комната ещё не создана.
     */
    @GetMapping("/rooms/by-meetup/{meetupId}")
    fun roomByMeetup(@PathVariable meetupId: UUID): ChatRoom =
        rooms.findByMeetupId(meetupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")

    /** Отправляет сообщение от текущего пользователя через REST. */
    @PostMapping("/rooms/{roomId}/messages")
    fun send(
        @PathVariable roomId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: SendMessageRequest,
    ): ChatMessage = chatService.postMessage(roomId, userId, request.text)

    /** История комнаты: последние [limit] сообщений (1–200), новые первыми. */
    @GetMapping("/rooms/{roomId}/messages")
    fun history(
        @PathVariable roomId: UUID,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<ChatMessage> =
        messages.findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(0, limit.coerceIn(1, 200)))
}

/**
 * WebSocket-обработчик входящих сообщений.
 *
 * @property chatService логика отправки сообщений.
 */
@Controller
class ChatWsController(private val chatService: ChatService) {
    /** Клиент шлёт в /app/rooms/{roomId}; рассылка идёт в /topic/rooms/{roomId}. */
    @MessageMapping("/rooms/{roomId}")
    fun onWsMessage(@DestinationVariable roomId: UUID, @Payload message: WsMessage) {
        chatService.postMessage(roomId, message.senderId, message.text)
    }
}

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

data class SendMessageRequest(val text: String)

/** Payload for messages arriving over the WebSocket (STOMP) channel. */
data class WsMessage(val senderId: UUID, val text: String)

@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
    private val rooms: ChatRoomRepository,
    private val messages: ChatMessageRepository,
) {
    @GetMapping("/rooms/by-meetup/{meetupId}")
    fun roomByMeetup(@PathVariable meetupId: UUID): ChatRoom =
        rooms.findByMeetupId(meetupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found")

    @PostMapping("/rooms/{roomId}/messages")
    fun send(
        @PathVariable roomId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: SendMessageRequest,
    ): ChatMessage = chatService.postMessage(roomId, userId, request.text)

    @GetMapping("/rooms/{roomId}/messages")
    fun history(
        @PathVariable roomId: UUID,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<ChatMessage> =
        messages.findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(0, limit.coerceIn(1, 200)))
}

@Controller
class ChatWsController(private val chatService: ChatService) {
    /** Client sends to /app/rooms/{roomId}; broadcast goes to /topic/rooms/{roomId}. */
    @MessageMapping("/rooms/{roomId}")
    fun onWsMessage(@DestinationVariable roomId: UUID, @Payload message: WsMessage) {
        chatService.postMessage(roomId, message.senderId, message.text)
    }
}

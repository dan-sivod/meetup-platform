package com.meetup.notifications

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/notifications")
class NotificationController(private val notifications: NotificationRepository) {

    @GetMapping
    fun inbox(@RequestHeader("X-User-Id") userId: UUID): List<Notification> =
        notifications.findByUserIdOrderByCreatedAtDesc(userId)

    @PostMapping("/{id}/read")
    fun markRead(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): Notification {
        val notification = notifications.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found")
        }
        if (notification.userId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification")
        }
        notification.read = true
        return notifications.save(notification)
    }

    @PostMapping("/read-all")
    fun markAllRead(@RequestHeader("X-User-Id") userId: UUID): Int {
        val unread = notifications.findByUserIdAndReadFalse(userId)
        unread.forEach { it.read = true }
        notifications.saveAll(unread)
        return unread.size
    }
}

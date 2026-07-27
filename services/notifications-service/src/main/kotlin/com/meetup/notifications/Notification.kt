package com.meetup.notifications

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val userId: UUID,
    @Column(nullable = false)
    val type: String,
    @Column(nullable = false)
    val title: String,
    val body: String? = null,
    /** Id of the related entity (meetup, friendship, ...) for deep links. */
    val refId: UUID? = null,
    var read: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>
    fun findByUserIdAndReadFalse(userId: UUID): List<Notification>
}

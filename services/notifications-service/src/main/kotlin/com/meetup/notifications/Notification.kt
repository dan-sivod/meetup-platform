package com.meetup.notifications

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/**
 * In-app уведомление пользователя.
 *
 * @property id идентификатор уведомления.
 * @property userId адресат уведомления.
 * @property type машиночитаемый тип (meetup_invite, friend_request...) для маршрутизации в UI.
 * @property title заголовок, показываемый пользователю.
 * @property body развёрнутый текст; может отсутствовать.
 * @property refId идентификатор связанной сущности (встречи, заявки...) для deep-link.
 * @property read прочитано ли уведомление.
 * @property createdAt момент создания.
 */
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
    val refId: UUID? = null,
    var read: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

/** Репозиторий уведомлений. */
interface NotificationRepository : JpaRepository<Notification, UUID> {
    /** Все уведомления пользователя, новые первыми. */
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>

    /** Непрочитанные уведомления пользователя. */
    fun findByUserIdAndReadFalse(userId: UUID): List<Notification>
}

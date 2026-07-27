package com.meetup.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/**
 * Учётные данные пользователя. Единственное место в системе,
 * где хранится хэш пароля; остальные сервисы знают только userId.
 *
 * @property id идентификатор пользователя, общий для всей платформы.
 * @property email уникальный адрес электронной почты (логин).
 * @property passwordHash BCrypt-хэш пароля; исходный пароль нигде не хранится.
 * @property displayName отображаемое имя, передаётся в users-service через событие.
 * @property createdAt момент регистрации.
 */
@Entity
@Table(name = "credentials")
class Credential(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(unique = true, nullable = false)
    val email: String,
    @Column(nullable = false)
    var passwordHash: String,
    @Column(nullable = false)
    var displayName: String,
    val createdAt: Instant = Instant.now(),
)

/** Репозиторий учётных данных. */
interface CredentialRepository : JpaRepository<Credential, UUID> {
    /** Находит учётные данные по адресу почты; null, если пользователь не зарегистрирован. */
    fun findByEmail(email: String): Credential?

    /** Проверяет, занят ли адрес почты. */
    fun existsByEmail(email: String): Boolean
}

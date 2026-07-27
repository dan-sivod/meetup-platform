package com.meetup.users

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/** Кому виден профиль пользователя. */
enum class Visibility {
    /** Виден всем пользователям платформы. */
    PUBLIC,

    /** Виден только друзьям (по умолчанию). */
    FRIENDS_ONLY,

    /** Скрыт от всех, кроме владельца. */
    PRIVATE,
}

/**
 * Публичный профиль пользователя. Учётные данные (пароль) хранятся
 * отдельно в auth-service.
 *
 * @property id идентификатор пользователя — тот же, что и в auth-service.
 * @property email адрес почты (копия из события регистрации).
 * @property displayName отображаемое имя.
 * @property bio краткое описание «о себе»; может отсутствовать.
 * @property avatarUrl ссылка на аватар; может отсутствовать.
 * @property timezone часовой пояс IANA (например, "America/New_York") для отображения времени встреч.
 * @property visibility настройка приватности профиля.
 * @property createdAt момент создания профиля.
 */
@Entity
@Table(name = "user_profiles")
class UserProfile(
    @Id
    val id: UUID,
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false)
    var displayName: String,
    var bio: String? = null,
    var avatarUrl: String? = null,
    var timezone: String = "UTC",
    @Enumerated(EnumType.STRING)
    var visibility: Visibility = Visibility.FRIENDS_ONLY,
    val createdAt: Instant = Instant.now(),
)

/** Репозиторий профилей пользователей. */
interface UserProfileRepository : JpaRepository<UserProfile, UUID>

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

enum class Visibility { PUBLIC, FRIENDS_ONLY, PRIVATE }

@Entity
@Table(name = "user_profiles")
class UserProfile(
    /** Same id as in auth-service — the platform-wide user id. */
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

interface UserProfileRepository : JpaRepository<UserProfile, UUID>

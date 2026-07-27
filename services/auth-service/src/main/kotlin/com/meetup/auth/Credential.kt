package com.meetup.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

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

interface CredentialRepository : JpaRepository<Credential, UUID> {
    fun findByEmail(email: String): Credential?
    fun existsByEmail(email: String): Boolean
}

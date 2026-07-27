package com.meetup.friends

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

enum class FriendshipStatus { PENDING, ACCEPTED, DECLINED, BLOCKED }

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["requesterId", "addresseeId"])],
)
class Friendship(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val requesterId: UUID,
    @Column(nullable = false)
    val addresseeId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FriendshipStatus = FriendshipStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    var respondedAt: Instant? = null,
)

interface FriendshipRepository : JpaRepository<Friendship, UUID> {
    @Query(
        """
        select f from Friendship f
        where (f.requesterId = :a and f.addresseeId = :b)
           or (f.requesterId = :b and f.addresseeId = :a)
        """,
    )
    fun findBetween(a: UUID, b: UUID): List<Friendship>

    @Query(
        """
        select f from Friendship f
        where f.status = com.meetup.friends.FriendshipStatus.ACCEPTED
          and (f.requesterId = :userId or f.addresseeId = :userId)
        """,
    )
    fun findAcceptedFor(userId: UUID): List<Friendship>

    fun findByAddresseeIdAndStatus(addresseeId: UUID, status: FriendshipStatus): List<Friendship>
}

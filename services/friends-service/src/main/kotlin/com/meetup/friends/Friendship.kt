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

/** Состояние связи между двумя пользователями. */
enum class FriendshipStatus {
    /** Заявка отправлена и ждёт ответа адресата. */
    PENDING,

    /** Заявка принята — пользователи друзья. */
    ACCEPTED,

    /** Заявка отклонена. */
    DECLINED,

    /** Инициатор заблокировал адресата: новые заявки невозможны. */
    BLOCKED,
}

/**
 * Ребро графа дружбы — направленная запись «кто кого позвал».
 * Для принятой дружбы связь считается симметричной.
 *
 * @property id идентификатор записи.
 * @property requesterId инициатор заявки (или блокировки).
 * @property addresseeId адресат заявки.
 * @property status текущее состояние связи.
 * @property createdAt момент создания заявки.
 * @property respondedAt момент ответа адресата; null, пока заявка в ожидании.
 */
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

/** Репозиторий связей графа дружбы. */
interface FriendshipRepository : JpaRepository<Friendship, UUID> {
    /** Возвращает все записи между пользователями [a] и [b] независимо от направления. */
    @Query(
        """
        select f from Friendship f
        where (f.requesterId = :a and f.addresseeId = :b)
           or (f.requesterId = :b and f.addresseeId = :a)
        """,
    )
    fun findBetween(a: UUID, b: UUID): List<Friendship>

    /** Возвращает все принятые дружбы пользователя [userId] в обоих направлениях. */
    @Query(
        """
        select f from Friendship f
        where f.status = com.meetup.friends.FriendshipStatus.ACCEPTED
          and (f.requesterId = :userId or f.addresseeId = :userId)
        """,
    )
    fun findAcceptedFor(userId: UUID): List<Friendship>

    /** Возвращает записи, адресованные пользователю, в заданном статусе (например, входящие заявки). */
    fun findByAddresseeIdAndStatus(addresseeId: UUID, status: FriendshipStatus): List<Friendship>
}

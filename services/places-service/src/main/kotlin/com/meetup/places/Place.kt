package com.meetup.places

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "places")
class Place(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var name: String,
    var category: String? = null,
    var address: String? = null,
    @Column(nullable = false)
    var latitude: Double,
    @Column(nullable = false)
    var longitude: Double,
    val createdBy: UUID,
    val createdAt: Instant = Instant.now(),
)

@Entity
@Table(
    name = "place_favorites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["placeId", "userId"])],
)
class PlaceFavorite(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val placeId: UUID,
    @Column(nullable = false)
    val userId: UUID,
    val createdAt: Instant = Instant.now(),
)

interface PlaceRepository : JpaRepository<Place, UUID>

interface PlaceFavoriteRepository : JpaRepository<PlaceFavorite, UUID> {
    fun findByUserId(userId: UUID): List<PlaceFavorite>
    fun existsByPlaceIdAndUserId(placeId: UUID, userId: UUID): Boolean
}

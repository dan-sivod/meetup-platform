package com.meetup.places

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

/**
 * Место для встречи: кафе, парк, квартира и т.п.
 *
 * @property id идентификатор места.
 * @property name название места.
 * @property category категория (кафе, бар, парк...); может отсутствовать.
 * @property address человекочитаемый адрес; может отсутствовать.
 * @property latitude широта в градусах.
 * @property longitude долгота в градусах.
 * @property createdBy пользователь, добавивший место в каталог.
 * @property createdAt момент добавления.
 */
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

/**
 * Отметка «избранное»: пользователь сохранил место себе.
 * Пара (место, пользователь) уникальна.
 *
 * @property id идентификатор отметки.
 * @property placeId избранное место.
 * @property userId владелец отметки.
 * @property createdAt момент добавления в избранное.
 */
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

/** Репозиторий каталога мест. */
interface PlaceRepository : JpaRepository<Place, UUID>

/** Репозиторий отметок «избранное». */
interface PlaceFavoriteRepository : JpaRepository<PlaceFavorite, UUID> {
    /** Все избранные места пользователя. */
    fun findByUserId(userId: UUID): List<PlaceFavorite>

    /** Проверяет, есть ли место в избранном у пользователя. */
    fun existsByPlaceIdAndUserId(placeId: UUID, userId: UUID): Boolean
}

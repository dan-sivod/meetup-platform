package com.meetup.places

import com.meetup.common.events.PlaceCreated
import com.meetup.common.events.PlaceFavorited
import com.meetup.common.events.Topics
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CreatePlaceRequest(
    val name: String,
    val category: String? = null,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
)

data class NearbyPlace(val place: Place, val distanceKm: Double)

@RestController
@RequestMapping("/places")
class PlaceController(
    private val places: PlaceRepository,
    private val favorites: PlaceFavoriteRepository,
    private val kafka: KafkaTemplate<String, Any>,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreatePlaceRequest,
    ): Place {
        val place = places.save(
            Place(
                name = request.name,
                category = request.category,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
                createdBy = userId,
            ),
        )
        kafka.send(
            Topics.PLACE_CREATED,
            place.id.toString(),
            PlaceCreated(place.id, place.name, place.category),
        )
        return place
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): Place = find(id)

    /**
     * Naive haversine scan over the catalog. In production this becomes a
     * PostGIS/geo-index query or a call to an external Places API.
     */
    @GetMapping("/nearby")
    fun nearby(
        @RequestParam lat: Double,
        @RequestParam lon: Double,
        @RequestParam(defaultValue = "5.0") radiusKm: Double,
    ): List<NearbyPlace> =
        places.findAll()
            .map { NearbyPlace(it, haversineKm(lat, lon, it.latitude, it.longitude)) }
            .filter { it.distanceKm <= radiusKm }
            .sortedBy { it.distanceKm }

    @PostMapping("/{id}/favorite")
    fun favorite(@PathVariable id: UUID, @RequestHeader("X-User-Id") userId: UUID): PlaceFavorite {
        val place = find(id)
        if (favorites.existsByPlaceIdAndUserId(id, userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Already favorited")
        }
        val favorite = favorites.save(PlaceFavorite(placeId = id, userId = userId))
        kafka.send(
            Topics.PLACE_FAVORITED,
            id.toString(),
            PlaceFavorited(id, userId, place.name, place.category),
        )
        return favorite
    }

    @GetMapping("/favorites")
    fun favorites(@RequestHeader("X-User-Id") userId: UUID): List<Place> =
        places.findAllById(favorites.findByUserId(userId).map { it.placeId })

    private fun find(id: UUID): Place = places.findById(id).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found")
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * earthRadiusKm * asin(sqrt(a))
    }
}

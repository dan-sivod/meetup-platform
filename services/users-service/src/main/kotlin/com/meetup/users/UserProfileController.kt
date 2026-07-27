package com.meetup.users

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val timezone: String? = null,
    val visibility: Visibility? = null,
)

@RestController
@RequestMapping("/users")
class UserProfileController(private val profiles: UserProfileRepository) {

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): UserProfile =
        profiles.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

    @GetMapping
    fun getBatch(@RequestParam ids: List<UUID>): List<UserProfile> = profiles.findAllById(ids)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") callerId: UUID,
        @RequestBody request: UpdateProfileRequest,
    ): UserProfile {
        if (id != callerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Can only edit own profile")
        }
        val profile = get(id)
        request.displayName?.let { profile.displayName = it }
        request.bio?.let { profile.bio = it }
        request.avatarUrl?.let { profile.avatarUrl = it }
        request.timezone?.let { profile.timezone = it }
        request.visibility?.let { profile.visibility = it }
        return profiles.save(profile)
    }
}

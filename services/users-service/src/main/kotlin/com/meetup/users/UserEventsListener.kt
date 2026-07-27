package com.meetup.users

import com.meetup.common.events.Topics
import com.meetup.common.events.UserRegistered
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class UserEventsListener(private val profiles: UserProfileRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [Topics.USER_REGISTERED])
    fun onUserRegistered(event: UserRegistered) {
        if (profiles.existsById(event.userId)) return
        profiles.save(
            UserProfile(
                id = event.userId,
                email = event.email,
                displayName = event.displayName,
            ),
        )
        log.info("Created profile for user {}", event.userId)
    }
}

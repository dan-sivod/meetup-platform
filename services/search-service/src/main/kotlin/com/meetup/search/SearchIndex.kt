package com.meetup.search

import com.meetup.common.events.MeetupCreated
import com.meetup.common.events.PlaceCreated
import com.meetup.common.events.Topics
import com.meetup.common.events.UserRegistered
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class DocType { USER, MEETUP, PLACE }

data class SearchDocument(val id: UUID, val type: DocType, val title: String, val text: String)

/**
 * In-memory inverted-scan index rebuilt from Kafka on restart
 * (auto-offset-reset: earliest). Production swaps this for OpenSearch;
 * the consumer contract stays identical.
 */
@Service
class SearchIndex {
    private val documents = ConcurrentHashMap<UUID, SearchDocument>()

    @KafkaListener(topics = [Topics.USER_REGISTERED])
    fun onUserRegistered(event: UserRegistered) {
        index(SearchDocument(event.userId, DocType.USER, event.displayName, "${event.displayName} ${event.email}"))
    }

    @KafkaListener(topics = [Topics.MEETUP_CREATED])
    fun onMeetupCreated(event: MeetupCreated) {
        index(SearchDocument(event.meetupId, DocType.MEETUP, event.title, event.title))
    }

    @KafkaListener(topics = [Topics.PLACE_CREATED])
    fun onPlaceCreated(event: PlaceCreated) {
        index(SearchDocument(event.placeId, DocType.PLACE, event.name, "${event.name} ${event.category ?: ""}"))
    }

    fun search(query: String, type: DocType?, limit: Int): List<SearchDocument> {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return documents.values
            .asSequence()
            .filter { type == null || it.type == type }
            .filter { doc -> terms.all { doc.text.lowercase().contains(it) } }
            .take(limit)
            .toList()
    }

    private fun index(document: SearchDocument) {
        documents[document.id] = document
    }
}

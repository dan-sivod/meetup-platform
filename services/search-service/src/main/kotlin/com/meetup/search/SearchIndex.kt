package com.meetup.search

import com.meetup.common.events.MeetupCreated
import com.meetup.common.events.PlaceCreated
import com.meetup.common.events.Topics
import com.meetup.common.events.UserRegistered
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Тип документа в поисковом индексе. */
enum class DocType {
    /** Профиль пользователя. */
    USER,

    /** Встреча. */
    MEETUP,

    /** Место. */
    PLACE,
}

/**
 * Документ поискового индекса.
 *
 * @property id идентификатор исходной сущности.
 * @property type тип документа.
 * @property title заголовок для выдачи.
 * @property text полный текст, по которому выполняется поиск.
 */
data class SearchDocument(val id: UUID, val type: DocType, val title: String, val text: String)

/**
 * In-memory индекс с линейным сканом, восстанавливаемый из Kafka
 * при рестарте (auto-offset-reset: earliest). В продакшене заменяется
 * на OpenSearch — контракт консьюмера остаётся тем же.
 */
@Service
class SearchIndex {
    /** Все документы индекса: id сущности → документ. */
    private val documents = ConcurrentHashMap<UUID, SearchDocument>()

    /** Индексирует нового пользователя по имени и почте. */
    @KafkaListener(topics = [Topics.USER_REGISTERED])
    fun onUserRegistered(event: UserRegistered) {
        index(SearchDocument(event.userId, DocType.USER, event.displayName, "${event.displayName} ${event.email}"))
    }

    /** Индексирует новую встречу по названию. */
    @KafkaListener(topics = [Topics.MEETUP_CREATED])
    fun onMeetupCreated(event: MeetupCreated) {
        index(SearchDocument(event.meetupId, DocType.MEETUP, event.title, event.title))
    }

    /** Индексирует новое место по названию и категории. */
    @KafkaListener(topics = [Topics.PLACE_CREATED])
    fun onPlaceCreated(event: PlaceCreated) {
        index(SearchDocument(event.placeId, DocType.PLACE, event.name, "${event.name} ${event.category ?: ""}"))
    }

    /**
     * Поиск документов, содержащих все слова запроса (без учёта регистра).
     *
     * @param query строка запроса; разбивается на слова по пробелам.
     * @param type ограничение по типу документа; null — искать по всем.
     * @param limit максимум результатов.
     */
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

    /** Добавляет или заменяет документ в индексе. */
    private fun index(document: SearchDocument) {
        documents[document.id] = document
    }
}

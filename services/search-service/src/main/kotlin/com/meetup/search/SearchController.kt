package com.meetup.search

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * HTTP-эндпоинты поиска.
 *
 * @property index поисковый индекс.
 */
@RestController
@RequestMapping("/search")
class SearchController(private val index: SearchIndex) {

    /**
     * Поиск по людям, встречам и местам.
     *
     * @param q строка запроса.
     * @param type ограничение по типу документа; не задан — искать по всем.
     * @param limit максимум результатов (ограничен 1–100).
     */
    @GetMapping
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false) type: DocType?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<SearchDocument> = index.search(q, type, limit.coerceIn(1, 100))
}

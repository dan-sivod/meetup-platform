package com.meetup.search

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(private val index: SearchIndex) {

    @GetMapping
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false) type: DocType?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<SearchDocument> = index.search(q, type, limit.coerceIn(1, 100))
}

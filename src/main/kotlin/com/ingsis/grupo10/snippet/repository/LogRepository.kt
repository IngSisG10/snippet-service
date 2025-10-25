package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.Log
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LogRepository : JpaRepository<Log, UUID> {
    fun findBySnippetIdAndTagName(
        snippetId: UUID,
        tagName: String,
    ): List<Log>

    fun findByTestId(testId: UUID): List<Log>

    fun findBySnippetIdOrderByDateDesc(snippetId: UUID): List<Log>

    fun findFirstBySnippetIdAndTagNameOrderByDateDesc(
        snippetId: UUID,
        tagName: String,
    ): Log?
}

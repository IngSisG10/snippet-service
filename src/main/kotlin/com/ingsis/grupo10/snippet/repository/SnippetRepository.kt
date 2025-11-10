package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.Snippet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnippetRepository : JpaRepository<Snippet, UUID> {
    /**
     * Finds all snippets owned by a specific user.
     *
     * @param ownerId The owner's UUID
     * @return List of snippets owned by the user
     */
    fun findByOwnerId(ownerId: String): List<Snippet>
}

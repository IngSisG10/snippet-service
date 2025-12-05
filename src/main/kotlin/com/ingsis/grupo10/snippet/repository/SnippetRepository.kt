package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.Snippet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SnippetRepository : JpaRepository<Snippet, UUID> {
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable,
    ): Page<Snippet>

    @Query(
        """
        SELECT s FROM Snippet s 
        WHERE s.id IN :snippetIds 
        AND (:name IS NULL OR :name = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
        AND (:language IS NULL OR :language = '' OR LOWER(s.language.name) = LOWER(CAST(:language AS string)))
        ORDER BY s.createdAt DESC
    """,
    )
    fun findFilteredSnippets(
        @Param("snippetIds") snippetIds: List<UUID>,
        @Param("name") name: String?,
        @Param("language") language: String?,
        pageable: Pageable,
    ): Page<Snippet>
}

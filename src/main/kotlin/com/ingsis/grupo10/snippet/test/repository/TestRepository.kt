package com.ingsis.grupo10.snippet.test.repository

import com.ingsis.grupo10.snippet.models.Test
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TestRepository : JpaRepository<Test, UUID> {
    fun findBySnippetId(snippetId: UUID): List<Test>
}

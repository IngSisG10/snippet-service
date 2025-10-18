package com.ingsis.grupo10.snippet.snippet.repository

import com.ingsis.grupo10.snippet.models.Snippet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnippetRepository : JpaRepository<Snippet, UUID>

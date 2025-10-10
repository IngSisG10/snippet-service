package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "snippet_log")
data class SnippetLog(
    @Id
    val id: UUID,
    @ManyToOne
    val snippet: Snippet,
    @ManyToOne
    val log: Log,
    val createdAt: String,
)

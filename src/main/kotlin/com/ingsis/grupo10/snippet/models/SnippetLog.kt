package com.ingsis.grupo10.snippet.models

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

// Ejecuciones del codigo
// ejecuciones del test

@Entity
@Table(name = "snippet_log")
data class SnippetLog(
    @Id
    val id: UUID,
    @ManyToOne
    @JoinColumn(name = "snippet_id", nullable = false)
    @JsonBackReference
    val snippet: Snippet,
    @ManyToOne
    @JoinColumn(name = "log_id", nullable = false)
    @JsonBackReference
    val log: Log,
    val createdAt: String,
)

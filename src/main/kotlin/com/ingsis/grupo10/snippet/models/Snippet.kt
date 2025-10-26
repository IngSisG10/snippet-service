package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "Snippet")
data class Snippet(
    @Id
    val id: UUID,
    val name: String,
    val codeUrl: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    val language: Language,
    val description: String?,
    val version: String,
    val ownerId: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    @OneToMany(mappedBy = "snippet")
    val logs: Set<Log> = emptySet(),
)

package com.ingsis.grupo10.snippet.models

import jakarta.persistence.CascadeType
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
class Snippet(
    @Id
    val id: UUID,
    val name: String,
    val codeUrl: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    val language: Language,
    val description: String?,
    val version: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    @OneToMany(mappedBy = "snippet", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    val logs: Set<Log> = emptySet(),
    @OneToMany(mappedBy = "snippet", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    val tests: Set<Test> = emptySet(),
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Snippet) return false
        return id == other.id
    }

    override fun toString(): String = "Snippet(id=$id, name='$name', version='$version')"
}

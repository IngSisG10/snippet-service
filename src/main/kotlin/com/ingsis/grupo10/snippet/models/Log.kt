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
@Table(name = "Log")
data class Log(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    val tag: Tag,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id")
    val snippet: Snippet?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    val test: Test?,
    val date: LocalDateTime,
    @OneToMany(mappedBy = "log")
    val dataEntries: Set<Data> = emptySet(),
)

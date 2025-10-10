package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "Log")
data class Log(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    val tag: Tag,
    val data: String,
    val date: String,
    @OneToMany(mappedBy = "log")
    val snippetLogs: Set<SnippetLog> = emptySet(),
)

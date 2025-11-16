package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "lint_config")
data class LintConfig(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val userId: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val config: String, // JSON string with linting rules
)

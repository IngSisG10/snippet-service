package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "Tag")
data class Tag(
    @Id
    val id: UUID,
    val name: String,
)

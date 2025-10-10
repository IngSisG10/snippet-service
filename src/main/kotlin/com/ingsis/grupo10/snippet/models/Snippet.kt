package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "Snippet")
data class Snippet(
    @Id
    val id: String,
    val name: String,
    val code: String,
    val languageId: String,
    val description: String,
    val version: String,
    val ownerId: String,
    val createdAt: String,
    val updatedAt: String,
)

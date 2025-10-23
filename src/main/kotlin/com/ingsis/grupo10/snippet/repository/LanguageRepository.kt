package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.Language
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LanguageRepository : JpaRepository<Language, UUID> {
    fun findByName(name: String): Language?
}

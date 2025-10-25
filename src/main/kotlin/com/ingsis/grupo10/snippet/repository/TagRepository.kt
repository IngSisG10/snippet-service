package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.Tag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TagRepository : JpaRepository<Tag, UUID> {
    fun findByName(name: String): Tag?
}

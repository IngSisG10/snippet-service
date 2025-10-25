package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.FormatConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FormatConfigRepository : JpaRepository<FormatConfig, UUID> {
    fun findByUserId(userId: UUID): FormatConfig?
}

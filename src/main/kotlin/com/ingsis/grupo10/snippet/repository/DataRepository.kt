package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.Data
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DataRepository : JpaRepository<Data, UUID> {
    fun findByLogId(logId: UUID): List<Data>
}

package com.ingsis.grupo10.snippet.repository

import com.ingsis.grupo10.snippet.models.LintConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LintConfigRepository : JpaRepository<LintConfig, UUID> {
    fun findByUserId(userId: String): LintConfig?
}

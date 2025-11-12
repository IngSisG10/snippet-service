package com.ingsis.grupo10.snippet.models

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "Data")
class Data(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    val log: Log,
    val name: String,
    val data: String,
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Data) return false
        return id == other.id
    }

    override fun toString(): String = "Data(id=$id, name='$name')"
}

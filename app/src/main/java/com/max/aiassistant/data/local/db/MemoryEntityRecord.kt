package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_entities",
    indices = [
        Index(value = ["name"]),
        Index(value = ["type"])
    ]
)
data class MemoryEntityRecord(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    @ColumnInfo(name = "canonical_name")
    val canonicalName: String?,
    val summary: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

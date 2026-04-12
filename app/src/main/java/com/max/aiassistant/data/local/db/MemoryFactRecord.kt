package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_facts",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntityRecord::class,
            parentColumns = ["id"],
            childColumns = ["entity_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_message_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["entity_id"]),
        Index(value = ["fact_type"]),
        Index(value = ["source_message_id"])
    ]
)
data class MemoryFactRecord(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "fact_type")
    val factType: String,
    val value: String,
    val confidence: Double?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "source_message_id")
    val sourceMessageId: String?
)

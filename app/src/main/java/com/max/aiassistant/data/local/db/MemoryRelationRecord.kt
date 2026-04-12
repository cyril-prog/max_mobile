package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_relations",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntityRecord::class,
            parentColumns = ["id"],
            childColumns = ["from_entity_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemoryEntityRecord::class,
            parentColumns = ["id"],
            childColumns = ["to_entity_id"],
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
        Index(value = ["from_entity_id"]),
        Index(value = ["to_entity_id"]),
        Index(value = ["relation_type"]),
        Index(value = ["source_message_id"])
    ]
)
data class MemoryRelationRecord(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "from_entity_id")
    val fromEntityId: String,
    @ColumnInfo(name = "relation_type")
    val relationType: String,
    @ColumnInfo(name = "to_entity_id")
    val toEntityId: String,
    val confidence: Double?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "source_message_id")
    val sourceMessageId: String?
)

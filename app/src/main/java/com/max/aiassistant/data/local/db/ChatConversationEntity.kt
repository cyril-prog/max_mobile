package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_conversations",
    indices = [
        Index(value = ["last_opened_at"]),
        Index(value = ["last_message_at"])
    ]
)
data class ChatConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    @ColumnInfo(name = "title_status")
    val titleStatus: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "last_opened_at")
    val lastOpenedAt: Long,
    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long?,
    @ColumnInfo(name = "message_count")
    val messageCount: Int
)

object ConversationTitleStatus {
    const val PENDING = "pending"
    const val READY = "ready"
}

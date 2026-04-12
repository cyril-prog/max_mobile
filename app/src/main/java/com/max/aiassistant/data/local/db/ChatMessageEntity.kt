package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversation_id", "created_at"]),
        Index(value = ["conversation_id"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    val role: String,
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "image_uri")
    val imageUri: String?,
    val status: String
)

object ChatMessageRole {
    const val USER = "user"
    const val ASSISTANT = "assistant"
}

object ChatMessageStatus {
    const val COMPLETED = "completed"
    const val STREAMING = "streaming"
    const val ERROR = "error"
}

package com.max.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getById(messageId: String): ChatMessageEntity?

    @Query(
        """
        SELECT * FROM chat_messages
        WHERE conversation_id = :conversationId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentMessages(
        conversationId: String,
        limit: Int
    ): Flow<List<ChatMessageEntity>>

    @Query(
        """
        SELECT * FROM chat_messages
        WHERE conversation_id = :conversationId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentMessages(
        conversationId: String,
        limit: Int
    ): List<ChatMessageEntity>

    @Query(
        """
        UPDATE chat_messages
        SET content = :content, status = :status
        WHERE id = :messageId
        """
    )
    suspend fun updateContentAndStatus(
        messageId: String,
        content: String,
        status: String
    )
}

package com.max.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ChatConversationEntity)

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getById(conversationId: String): ChatConversationEntity?

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId LIMIT 1")
    fun observeById(conversationId: String): Flow<ChatConversationEntity?>

    @Query(
        """
        SELECT * FROM chat_conversations
        ORDER BY last_opened_at DESC, updated_at DESC
        """
    )
    fun observeAll(): Flow<List<ChatConversationEntity>>

    @Query(
        """
        SELECT * FROM chat_conversations
        ORDER BY last_opened_at DESC, updated_at DESC
        LIMIT 1
        """
    )
    suspend fun getLastOpenedConversation(): ChatConversationEntity?

    @Query(
        """
        UPDATE chat_conversations
        SET last_opened_at = :openedAt, updated_at = :updatedAt
        WHERE id = :conversationId
        """
    )
    suspend fun touchOpened(
        conversationId: String,
        openedAt: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE chat_conversations
        SET
            updated_at = :updatedAt,
            last_message_at = :lastMessageAt,
            last_opened_at = :lastOpenedAt,
            message_count = message_count + 1
        WHERE id = :conversationId
        """
    )
    suspend fun onMessageAdded(
        conversationId: String,
        updatedAt: Long,
        lastMessageAt: Long,
        lastOpenedAt: Long
    )

    @Query(
        """
        UPDATE chat_conversations
        SET title = :title, title_status = :titleStatus, updated_at = :updatedAt
        WHERE id = :conversationId
        """
    )
    suspend fun updateTitle(
        conversationId: String,
        title: String,
        titleStatus: String,
        updatedAt: Long
    )

    @Query(
        """
        DELETE FROM chat_conversations
        WHERE id = :conversationId
        """
    )
    suspend fun deleteById(conversationId: String)
}

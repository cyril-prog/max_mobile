package com.max.aiassistant.data.local.db

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val database: MaxDatabase
) {

    private val conversationDao = database.chatConversationDao()
    private val messageDao = database.chatMessageDao()

    suspend fun getLastOpenedConversationOrCreate(): ChatConversationEntity {
        val existingConversation = conversationDao.getLastOpenedConversation()
        if (existingConversation != null) {
            return existingConversation
        }
        return createConversation()
    }

    suspend fun createConversation(now: Long = System.currentTimeMillis()): ChatConversationEntity {
        val conversation = ChatConversationEntity(
            id = UUID.randomUUID().toString(),
            title = DEFAULT_CONVERSATION_TITLE,
            titleStatus = ConversationTitleStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            lastOpenedAt = now,
            lastMessageAt = null,
            messageCount = 0
        )
        conversationDao.insert(conversation)
        return conversation
    }

    suspend fun getConversation(conversationId: String): ChatConversationEntity? {
        return conversationDao.getById(conversationId)
    }

    fun observeConversation(conversationId: String): Flow<ChatConversationEntity?> {
        return conversationDao.observeById(conversationId)
    }

    fun observeConversations(): Flow<List<ChatConversationEntity>> {
        return conversationDao.observeAll()
    }

    fun observeRecentMessages(
        conversationId: String,
        limit: Int
    ): Flow<List<ChatMessageEntity>> {
        return messageDao.observeRecentMessages(conversationId, limit)
    }

    suspend fun touchConversationOpened(conversationId: String, now: Long = System.currentTimeMillis()) {
        conversationDao.touchOpened(
            conversationId = conversationId,
            openedAt = now,
            updatedAt = now
        )
    }

    suspend fun insertMessage(message: ChatMessageEntity) {
        database.withTransaction {
            messageDao.insert(message)
            conversationDao.onMessageAdded(
                conversationId = message.conversationId,
                updatedAt = message.createdAt,
                lastMessageAt = message.createdAt,
                lastOpenedAt = message.createdAt
            )
        }
    }

    suspend fun upsertAssistantMessage(
        conversationId: String,
        messageId: String,
        content: String,
        status: String,
        createdAt: Long = System.currentTimeMillis()
    ) {
        database.withTransaction {
            val existing = messageDao.getById(messageId)
            if (existing == null) {
                messageDao.insert(
                    ChatMessageEntity(
                        id = messageId,
                        conversationId = conversationId,
                        role = ChatMessageRole.ASSISTANT,
                        content = content,
                        createdAt = createdAt,
                        imageUri = null,
                        status = status
                    )
                )
                conversationDao.onMessageAdded(
                    conversationId = conversationId,
                    updatedAt = createdAt,
                    lastMessageAt = createdAt,
                    lastOpenedAt = createdAt
                )
            } else {
                messageDao.updateContentAndStatus(
                    messageId = messageId,
                    content = content,
                    status = status
                )
            }
        }
    }

    suspend fun getRecentMessagesForContext(
        conversationId: String,
        limit: Int
    ): List<ChatMessageEntity> {
        return messageDao.getRecentMessagesExcludingRole(
            conversationId = conversationId,
            limit = limit,
            excludedRole = ChatMessageRole.TOOL
        )
            .asReversed()
    }

    suspend fun updateConversationTitle(
        conversationId: String,
        title: String,
        now: Long = System.currentTimeMillis()
    ) {
        conversationDao.updateTitle(
            conversationId = conversationId,
            title = title,
            titleStatus = ConversationTitleStatus.READY,
            updatedAt = now
        )
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteById(conversationId)
    }

    companion object {
        const val DEFAULT_CONVERSATION_TITLE = "Nouvelle conversation"
    }
}

package com.max.aiassistant.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatConversationEntity::class,
        ChatMessageEntity::class,
        MemoryEntityRecord::class,
        MemoryRelationRecord::class,
        MemoryFactRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MaxDatabase : RoomDatabase() {

    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryGraphDao(): MemoryGraphDao

    companion object {
        @Volatile
        private var INSTANCE: MaxDatabase? = null

        fun getInstance(context: Context): MaxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MaxDatabase::class.java,
                    "max_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

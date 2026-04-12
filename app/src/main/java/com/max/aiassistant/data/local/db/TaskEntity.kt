package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updated_at"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val note: String,
    val status: String,
    val priority: String,
    @ColumnInfo(name = "deadline_date")
    val deadlineDate: String,
    val category: String,
    @ColumnInfo(name = "estimated_duration")
    val estimatedDuration: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

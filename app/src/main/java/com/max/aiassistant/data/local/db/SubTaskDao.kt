package com.max.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SubTaskDao {

    @Query("SELECT * FROM sub_tasks WHERE id = :subTaskId LIMIT 1")
    suspend fun getById(subTaskId: String): SubTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subTask: SubTaskEntity)

    @Query("DELETE FROM sub_tasks WHERE id = :subTaskId")
    suspend fun deleteById(subTaskId: String)
}

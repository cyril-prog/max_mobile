package com.max.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryGraphDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntity(entity: MemoryEntityRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRelation(relation: MemoryRelationRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFact(fact: MemoryFactRecord)

    @Query(
        """
        SELECT * FROM memory_entities
        WHERE name LIKE '%' || :query || '%' OR canonical_name LIKE '%' || :query || '%'
        ORDER BY updated_at DESC
        LIMIT :limit
        """
    )
    suspend fun searchEntities(
        query: String,
        limit: Int = 20
    ): List<MemoryEntityRecord>

    @Query(
        """
        SELECT * FROM memory_relations
        WHERE from_entity_id = :entityId OR to_entity_id = :entityId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    suspend fun getRelationsForEntity(
        entityId: String,
        limit: Int = 50
    ): List<MemoryRelationRecord>

    @Query(
        """
        SELECT * FROM memory_facts
        WHERE entity_id = :entityId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    suspend fun getFactsForEntity(
        entityId: String,
        limit: Int = 50
    ): List<MemoryFactRecord>
}

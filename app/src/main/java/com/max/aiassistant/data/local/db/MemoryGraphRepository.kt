package com.max.aiassistant.data.local.db

import java.util.UUID

class MemoryGraphRepository(
    private val memoryGraphDao: MemoryGraphDao
) {

    data class EntityInput(
        val name: String,
        val type: String,
        val canonicalName: String? = null,
        val summary: String? = null
    )

    data class RelationInput(
        val fromEntityName: String,
        val relationType: String,
        val toEntityName: String,
        val confidence: Double? = null
    )

    data class FactInput(
        val entityName: String,
        val factType: String,
        val value: String,
        val confidence: Double? = null
    )

    data class SaveResult(
        val entityCount: Int,
        val relationCount: Int,
        val factCount: Int
    )

    data class SearchMatch(
        val entityId: String,
        val name: String,
        val type: String,
        val canonicalName: String?,
        val summary: String?,
        val facts: List<FactMatch>,
        val relations: List<RelationMatch>
    )

    data class FactMatch(
        val factType: String,
        val value: String,
        val confidence: Double?
    )

    data class RelationMatch(
        val relationType: String,
        val otherEntityName: String,
        val direction: String,
        val confidence: Double?
    )

    suspend fun storeImportantMemory(
        entities: List<EntityInput>,
        relations: List<RelationInput>,
        facts: List<FactInput>,
        sourceMessageId: String? = null
    ): SaveResult {
        val now = System.currentTimeMillis()
        val entityIdsByName = mutableMapOf<String, String>()
        var savedEntities = 0
        var savedRelations = 0
        var savedFacts = 0

        fun rememberEntityAlias(alias: String?, entityId: String) {
            val normalizedAlias = alias.normalizeMemoryKey()
            if (normalizedAlias.isNotBlank()) {
                entityIdsByName[normalizedAlias] = entityId
            }
        }

        suspend fun ensureEntity(
            name: String,
            type: String = "unknown",
            canonicalName: String? = null,
            summary: String? = null
        ): String {
            val normalizedName = name.trim()
            val normalizedCanonical = canonicalName?.trim()?.takeIf { it.isNotBlank() } ?: normalizedName
            val entityId = buildStableId("ent", "$type|${normalizedCanonical.normalizeMemoryKey()}")

            memoryGraphDao.upsertEntity(
                MemoryEntityRecord(
                    id = entityId,
                    type = type.trim().ifBlank { "unknown" },
                    name = normalizedName,
                    canonicalName = normalizedCanonical,
                    summary = summary?.trim()?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
            )
            savedEntities += 1

            rememberEntityAlias(normalizedName, entityId)
            rememberEntityAlias(normalizedCanonical, entityId)
            return entityId
        }

        entities.forEach { entity ->
            ensureEntity(
                name = entity.name,
                type = entity.type,
                canonicalName = entity.canonicalName,
                summary = entity.summary
            )
        }

        relations.forEach { relation ->
            val fromEntityId = entityIdsByName[relation.fromEntityName.normalizeMemoryKey()]
                ?: ensureEntity(relation.fromEntityName)
            val toEntityId = entityIdsByName[relation.toEntityName.normalizeMemoryKey()]
                ?: ensureEntity(relation.toEntityName)

            memoryGraphDao.upsertRelation(
                MemoryRelationRecord(
                    id = buildStableId(
                        "rel",
                        "$fromEntityId|${relation.relationType.normalizeMemoryKey()}|$toEntityId"
                    ),
                    fromEntityId = fromEntityId,
                    relationType = relation.relationType.trim(),
                    toEntityId = toEntityId,
                    confidence = relation.confidence,
                    createdAt = now,
                    sourceMessageId = sourceMessageId
                )
            )
            savedRelations += 1
        }

        facts.forEach { fact ->
            val entityId = entityIdsByName[fact.entityName.normalizeMemoryKey()]
                ?: ensureEntity(fact.entityName)

            memoryGraphDao.upsertFact(
                MemoryFactRecord(
                    id = buildStableId(
                        "fact",
                        "$entityId|${fact.factType.normalizeMemoryKey()}|${fact.value.normalizeMemoryKey()}"
                    ),
                    entityId = entityId,
                    factType = fact.factType.trim(),
                    value = fact.value.trim(),
                    confidence = fact.confidence,
                    createdAt = now,
                    sourceMessageId = sourceMessageId
                )
            )
            savedFacts += 1
        }

        return SaveResult(
            entityCount = savedEntities,
            relationCount = savedRelations,
            factCount = savedFacts
        )
    }

    suspend fun searchMemory(
        query: String,
        limit: Int = 5
    ): List<SearchMatch> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        val entitiesByName = memoryGraphDao.searchEntities(
            query = normalizedQuery,
            limit = limit
        )

        val entitiesByFacts = memoryGraphDao.searchEntityIdsByFacts(
            query = normalizedQuery,
            limit = limit
        ).mapNotNull { entityId ->
            memoryGraphDao.getEntityById(entityId)
        }

        val entities = buildList {
            val seenIds = mutableSetOf<String>()
            (entitiesByName + entitiesByFacts).forEach { entity ->
                if (seenIds.add(entity.id)) {
                    add(entity)
                }
            }
        }.take(limit)

        return entities.map { entity ->
            val facts = memoryGraphDao.getFactsForEntity(entity.id).map { fact ->
                FactMatch(
                    factType = fact.factType,
                    value = fact.value,
                    confidence = fact.confidence
                )
            }

            val relations = memoryGraphDao.getRelationsForEntity(entity.id).map { relation ->
                val otherEntityId = if (relation.fromEntityId == entity.id) {
                    relation.toEntityId
                } else {
                    relation.fromEntityId
                }

                val otherEntity = memoryGraphDao.getEntityById(otherEntityId)

                RelationMatch(
                    relationType = relation.relationType,
                    otherEntityName = otherEntity?.name ?: otherEntityId,
                    direction = if (relation.fromEntityId == entity.id) "outgoing" else "incoming",
                    confidence = relation.confidence
                )
            }

            SearchMatch(
                entityId = entity.id,
                name = entity.name,
                type = entity.type,
                canonicalName = entity.canonicalName,
                summary = entity.summary,
                facts = facts,
                relations = relations
            )
        }
    }

    private fun String?.normalizeMemoryKey(): String {
        return this
            ?.trim()
            ?.lowercase()
            .orEmpty()
    }

    private fun buildStableId(prefix: String, rawKey: String): String {
        val uuid = UUID.nameUUIDFromBytes(rawKey.toByteArray(Charsets.UTF_8))
        return "${prefix}_$uuid"
    }
}

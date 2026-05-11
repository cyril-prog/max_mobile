package com.max.aiassistant.data.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.max.aiassistant.data.api.StreamingRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StreamingReleasesCacheSnapshot(
    val cacheKey: String,
    val releases: List<StreamingRelease>,
    val fetchedAt: Long
) {
    fun isExpired(now: Long, ttlMillis: Long): Boolean {
        return now - fetchedAt > ttlMillis
    }
}

class StreamingReleasesCache(
    context: Context,
    private val gson: Gson = Gson()
) {
    private val cacheFile = File(context.filesDir, "streaming_releases_cache.json")

    suspend fun get(cacheKey: String): StreamingReleasesCacheSnapshot? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null

        runCatching {
            val cached = gson.fromJson<StreamingReleasesCachePayload>(
                cacheFile.readText(),
                cachePayloadType
            )
            if (cached.cacheKey == cacheKey) {
                StreamingReleasesCacheSnapshot(
                    cacheKey = cached.cacheKey,
                    releases = cached.releases,
                    fetchedAt = cached.fetchedAt
                )
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun save(snapshot: StreamingReleasesCacheSnapshot) = withContext(Dispatchers.IO) {
        val payload = StreamingReleasesCachePayload(
            cacheKey = snapshot.cacheKey,
            releases = snapshot.releases,
            fetchedAt = snapshot.fetchedAt
        )
        cacheFile.writeText(gson.toJson(payload))
    }

    private data class StreamingReleasesCachePayload(
        val cacheKey: String,
        val releases: List<StreamingRelease>,
        val fetchedAt: Long
    )

    private companion object {
        val cachePayloadType = object : TypeToken<StreamingReleasesCachePayload>() {}.type
    }
}

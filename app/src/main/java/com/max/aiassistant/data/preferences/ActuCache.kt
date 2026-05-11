package com.max.aiassistant.data.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.max.aiassistant.data.api.ActuArticle
import com.max.aiassistant.data.api.RechercheArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ActuCacheSnapshot(
    val actuArticles: List<ActuArticle>,
    val rechercheArticles: List<RechercheArticle>,
    val fetchedAt: Long
)

class ActuCache(
    context: Context,
    private val gson: Gson = Gson()
) {
    private val cacheFile = File(context.filesDir, "actu_cache.json")

    suspend fun get(): ActuCacheSnapshot? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null

        runCatching {
            val cached = gson.fromJson<ActuCachePayload>(
                cacheFile.readText(),
                cachePayloadType
            )
            ActuCacheSnapshot(
                actuArticles = cached.actuArticles,
                rechercheArticles = cached.rechercheArticles,
                fetchedAt = cached.fetchedAt
            )
        }.getOrNull()
    }

    suspend fun save(snapshot: ActuCacheSnapshot) = withContext(Dispatchers.IO) {
        val payload = ActuCachePayload(
            actuArticles = snapshot.actuArticles,
            rechercheArticles = snapshot.rechercheArticles,
            fetchedAt = snapshot.fetchedAt
        )
        cacheFile.writeText(gson.toJson(payload))
    }

    private data class ActuCachePayload(
        val actuArticles: List<ActuArticle>,
        val rechercheArticles: List<RechercheArticle>,
        val fetchedAt: Long
    )

    private companion object {
        val cachePayloadType = object : TypeToken<ActuCachePayload>() {}.type
    }
}

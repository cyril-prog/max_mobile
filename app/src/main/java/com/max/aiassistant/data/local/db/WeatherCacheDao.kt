package com.max.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherCacheDao {

    @Query("SELECT * FROM weather_cache WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: WeatherCacheEntity)
}

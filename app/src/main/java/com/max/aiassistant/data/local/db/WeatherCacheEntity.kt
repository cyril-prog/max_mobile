package com.max.aiassistant.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weather_cache",
    indices = [
        Index(value = ["city_name"]),
        Index(value = ["updated_at"])
    ]
)
data class WeatherCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,
    @ColumnInfo(name = "city_name")
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "current_temperature")
    val currentTemperature: Double,
    @ColumnInfo(name = "current_humidity")
    val currentHumidity: Int,
    @ColumnInfo(name = "current_wind_speed")
    val currentWindSpeed: Double,
    @ColumnInfo(name = "weather_code")
    val weatherCode: Int,
    @ColumnInfo(name = "hourly_forecasts_json")
    val hourlyForecastsJson: String,
    @ColumnInfo(name = "daily_forecasts_json")
    val dailyForecastsJson: String,
    @ColumnInfo(name = "grass_pollen")
    val grassPollen: Double?,
    @ColumnInfo(name = "birch_pollen")
    val birchPollen: Double?,
    @ColumnInfo(name = "alder_pollen")
    val alderPollen: Double?,
    @ColumnInfo(name = "olive_pollen")
    val olivePollen: Double?,
    @ColumnInfo(name = "mugwort_pollen")
    val mugwortPollen: Double?,
    @ColumnInfo(name = "ragweed_pollen")
    val ragweedPollen: Double?,
    @ColumnInfo(name = "pollen_types_json")
    val pollenTypesJson: String,
    @ColumnInfo(name = "pollen_plants_json")
    val pollenPlantsJson: String,
    @ColumnInfo(name = "pollen_source")
    val pollenSource: String?,
    @ColumnInfo(name = "weather_fetched_at")
    val weatherFetchedAt: Long,
    @ColumnInfo(name = "pollen_fetched_at")
    val pollenFetchedAt: Long?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

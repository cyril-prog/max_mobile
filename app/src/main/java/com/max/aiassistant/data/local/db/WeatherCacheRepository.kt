package com.max.aiassistant.data.local.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.max.aiassistant.data.api.CurrentPollenData
import com.max.aiassistant.data.api.DailyForecast
import com.max.aiassistant.data.api.HourlyForecast
import com.max.aiassistant.data.api.WeatherData
import java.util.Locale

data class WeatherCacheSnapshot(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val weatherData: WeatherData,
    val weatherFetchedAt: Long,
    val pollenFetchedAt: Long?
) {
    fun isWeatherExpired(now: Long, ttlMillis: Long): Boolean {
        return now - weatherFetchedAt > ttlMillis
    }

    fun isPollenExpired(now: Long, ttlMillis: Long): Boolean {
        val fetchedAt = pollenFetchedAt ?: return true
        return now - fetchedAt > ttlMillis
    }
}

class WeatherCacheRepository(
    private val weatherCacheDao: WeatherCacheDao,
    private val gson: Gson = Gson()
) {

    suspend fun get(latitude: Double, longitude: Double): WeatherCacheSnapshot? {
        return weatherCacheDao.getByKey(buildCacheKey(latitude, longitude))?.toSnapshot()
    }

    suspend fun save(snapshot: WeatherCacheSnapshot) {
        weatherCacheDao.upsert(snapshot.toEntity())
    }

    fun buildCacheKey(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.6f:%.6f", latitude, longitude)
    }

    private fun WeatherCacheEntity.toSnapshot(): WeatherCacheSnapshot {
        return WeatherCacheSnapshot(
            cityName = cityName,
            latitude = latitude,
            longitude = longitude,
            weatherData = WeatherData(
                currentTemperature = currentTemperature,
                currentHumidity = currentHumidity,
                currentWindSpeed = currentWindSpeed,
                weatherCode = weatherCode,
                hourlyForecasts = fromJson(hourlyForecastsJson, hourlyForecastListType),
                dailyForecasts = fromJson(dailyForecastsJson, dailyForecastListType),
                grassPollen = grassPollen,
                birchPollen = birchPollen,
                alderPollen = alderPollen,
                olivePollen = olivePollen,
                mugwortPollen = mugwortPollen,
                ragweedPollen = ragweedPollen
            ),
            weatherFetchedAt = weatherFetchedAt,
            pollenFetchedAt = pollenFetchedAt
        )
    }

    private fun WeatherCacheSnapshot.toEntity(): WeatherCacheEntity {
        return WeatherCacheEntity(
            cacheKey = buildCacheKey(latitude, longitude),
            cityName = cityName,
            latitude = latitude,
            longitude = longitude,
            currentTemperature = weatherData.currentTemperature,
            currentHumidity = weatherData.currentHumidity,
            currentWindSpeed = weatherData.currentWindSpeed,
            weatherCode = weatherData.weatherCode,
            hourlyForecastsJson = gson.toJson(weatherData.hourlyForecasts),
            dailyForecastsJson = gson.toJson(weatherData.dailyForecasts),
            grassPollen = weatherData.grassPollen,
            birchPollen = weatherData.birchPollen,
            alderPollen = weatherData.alderPollen,
            olivePollen = weatherData.olivePollen,
            mugwortPollen = weatherData.mugwortPollen,
            ragweedPollen = weatherData.ragweedPollen,
            weatherFetchedAt = weatherFetchedAt,
            pollenFetchedAt = pollenFetchedAt,
            updatedAt = maxOf(weatherFetchedAt, pollenFetchedAt ?: 0L)
        )
    }

    private fun WeatherData.withPollen(pollen: CurrentPollenData?): WeatherData {
        if (pollen == null) return this
        return copy(
            grassPollen = pollen.grassPollen,
            birchPollen = pollen.birchPollen,
            alderPollen = pollen.alderPollen,
            olivePollen = pollen.olivePollen,
            mugwortPollen = pollen.mugwortPollen,
            ragweedPollen = pollen.ragweedPollen
        )
    }

    fun mergePollen(weatherData: WeatherData, pollen: CurrentPollenData?): WeatherData {
        return weatherData.withPollen(pollen)
    }

    private fun <T> fromJson(json: String, type: java.lang.reflect.Type): T {
        return gson.fromJson(json, type)
    }

    private companion object {
        val hourlyForecastListType = object : TypeToken<List<HourlyForecast>>() {}.type
        val dailyForecastListType = object : TypeToken<List<DailyForecast>>() {}.type
    }
}

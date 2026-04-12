package com.max.aiassistant.data.local.db

import com.max.aiassistant.data.api.CurrentPollenData
import com.max.aiassistant.data.api.DailyForecast
import com.max.aiassistant.data.api.HourlyForecast
import com.max.aiassistant.data.api.WeatherData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCacheRepositoryTest {

    @Test
    fun `repository stores and restores weather snapshot`() = runBlocking {
        val dao = FakeWeatherCacheDao()
        val repository = WeatherCacheRepository(dao)
        val snapshot = WeatherCacheSnapshot(
            cityName = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            weatherData = sampleWeatherData(),
            weatherFetchedAt = 1_000L,
            pollenFetchedAt = 2_000L
        )

        repository.save(snapshot)

        val restored = repository.get(48.8566, 2.3522)

        assertNotNull(restored)
        assertEquals("Paris", restored?.cityName)
        assertEquals(21.5, restored?.weatherData?.currentTemperature ?: 0.0, 0.0)
        assertEquals(2, restored?.weatherData?.hourlyForecasts?.size)
        assertEquals(1, restored?.weatherData?.dailyForecasts?.size)
        assertEquals(12.0, restored?.weatherData?.grassPollen ?: 0.0, 0.0)
        assertEquals(2_000L, restored?.pollenFetchedAt)
    }

    @Test
    fun `snapshot expiration uses separate weather and pollen timestamps`() {
        val snapshot = WeatherCacheSnapshot(
            cityName = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            weatherData = sampleWeatherData(),
            weatherFetchedAt = 3_600_000L,
            pollenFetchedAt = 0L
        )

        assertFalse(snapshot.isWeatherExpired(now = 7_000_000L, ttlMillis = 3_600_000L))
        assertTrue(snapshot.isPollenExpired(now = 7_000_000L, ttlMillis = 3_600_000L))
    }

    @Test
    fun `mergePollen updates only allergy fields`() {
        val repository = WeatherCacheRepository(FakeWeatherCacheDao())
        val merged = repository.mergePollen(
            sampleWeatherData(),
            CurrentPollenData(
                grassPollen = 99.0,
                birchPollen = 12.0,
                alderPollen = 8.0,
                olivePollen = 4.0,
                mugwortPollen = 2.0,
                ragweedPollen = 1.0
            )
        )

        assertEquals(21.5, merged.currentTemperature, 0.0)
        assertEquals(99.0, merged.grassPollen ?: 0.0, 0.0)
        assertEquals(1.0, merged.ragweedPollen ?: 0.0, 0.0)
    }

    private fun sampleWeatherData(): WeatherData {
        return WeatherData(
            currentTemperature = 21.5,
            currentHumidity = 58,
            currentWindSpeed = 12.0,
            weatherCode = 3,
            hourlyForecasts = listOf(
                HourlyForecast("09:00", 20.0, 60, 10, 3),
                HourlyForecast("10:00", 21.0, 58, 5, 2)
            ),
            dailyForecasts = listOf(
                DailyForecast("2026-04-12", "Aujourd'hui", 23.0, 14.0, 20, 3)
            ),
            grassPollen = 12.0,
            birchPollen = 3.0,
            alderPollen = 1.0,
            olivePollen = 0.0,
            mugwortPollen = 0.0,
            ragweedPollen = 0.0
        )
    }

    private class FakeWeatherCacheDao : WeatherCacheDao {
        private var cache: WeatherCacheEntity? = null

        override suspend fun getByKey(cacheKey: String): WeatherCacheEntity? {
            return cache?.takeIf { it.cacheKey == cacheKey }
        }

        override suspend fun upsert(cache: WeatherCacheEntity) {
            this.cache = cache
        }
    }
}

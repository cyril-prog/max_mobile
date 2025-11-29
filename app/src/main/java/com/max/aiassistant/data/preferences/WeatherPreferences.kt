package com.max.aiassistant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherDataStore by preferencesDataStore(name = "weather_prefs")

class WeatherPreferences(private val context: Context) {

    private val CITY_NAME_KEY = stringPreferencesKey("city_name")
    private val CITY_LAT_KEY = doublePreferencesKey("city_latitude")
    private val CITY_LON_KEY = doublePreferencesKey("city_longitude")
    private val SHOW_ALLERGIES_KEY = booleanPreferencesKey("show_allergies")

    val cityPreferences: Flow<WeatherCityPreferences> = context.weatherDataStore.data
        .map { prefs ->
            WeatherCityPreferences(
                cityName = prefs[CITY_NAME_KEY] ?: DEFAULT_CITY_NAME,
                latitude = prefs[CITY_LAT_KEY] ?: DEFAULT_LATITUDE,
                longitude = prefs[CITY_LON_KEY] ?: DEFAULT_LONGITUDE,
                showAllergies = prefs[SHOW_ALLERGIES_KEY] ?: DEFAULT_SHOW_ALLERGIES
            )
        }

    suspend fun saveCity(name: String, latitude: Double, longitude: Double) {
        context.weatherDataStore.edit { prefs ->
            prefs[CITY_NAME_KEY] = name
            prefs[CITY_LAT_KEY] = latitude
            prefs[CITY_LON_KEY] = longitude
        }
    }

    suspend fun setShowAllergies(show: Boolean) {
        context.weatherDataStore.edit { prefs ->
            prefs[SHOW_ALLERGIES_KEY] = show
        }
    }

    companion object {
        const val DEFAULT_CITY_NAME = "Nantes"
        const val DEFAULT_LATITUDE = 47.2184
        const val DEFAULT_LONGITUDE = -1.5536
        const val DEFAULT_SHOW_ALLERGIES = true
    }
}

data class WeatherCityPreferences(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val showAllergies: Boolean
)


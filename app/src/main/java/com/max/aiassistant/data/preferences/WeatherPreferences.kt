package com.max.aiassistant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherDataStore by preferencesDataStore(name = "weather_prefs")

class WeatherPreferences(private val context: Context) {

    private val cityNameKey = stringPreferencesKey("city_name")
    private val cityLatKey = doublePreferencesKey("city_latitude")
    private val cityLonKey = doublePreferencesKey("city_longitude")
    private val cityCountryCodeKey = stringPreferencesKey("city_country_code")
    private val showAllergiesKey = booleanPreferencesKey("show_allergies")

    val cityPreferences: Flow<WeatherCityPreferences> = context.weatherDataStore.data
        .map { prefs ->
            WeatherCityPreferences(
                cityName = prefs[cityNameKey] ?: DEFAULT_CITY_NAME,
                latitude = prefs[cityLatKey] ?: DEFAULT_LATITUDE,
                longitude = prefs[cityLonKey] ?: DEFAULT_LONGITUDE,
                countryCode = prefs[cityCountryCodeKey] ?: DEFAULT_COUNTRY_CODE,
                showAllergies = prefs[showAllergiesKey] ?: DEFAULT_SHOW_ALLERGIES
            )
        }

    suspend fun saveCity(
        name: String,
        latitude: Double,
        longitude: Double,
        countryCode: String
    ) {
        context.weatherDataStore.edit { prefs ->
            prefs[cityNameKey] = name
            prefs[cityLatKey] = latitude
            prefs[cityLonKey] = longitude
            prefs[cityCountryCodeKey] = countryCode.uppercase()
        }
    }

    suspend fun setShowAllergies(show: Boolean) {
        context.weatherDataStore.edit { prefs ->
            prefs[showAllergiesKey] = show
        }
    }

    companion object {
        const val DEFAULT_CITY_NAME = "Nantes"
        const val DEFAULT_LATITUDE = 47.2184
        const val DEFAULT_LONGITUDE = -1.5536
        const val DEFAULT_COUNTRY_CODE = "FR"
        const val DEFAULT_SHOW_ALLERGIES = true
    }
}

data class WeatherCityPreferences(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val showAllergies: Boolean
)

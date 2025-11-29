package com.max.aiassistant.data.api

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName

/**
 * Modèles de données pour l'API Open-Meteo
 * Documentation: https://open-meteo.com/en/docs
 */

/**
 * Réponse complète de l'API météo Open-Meteo
 */
data class WeatherApiResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerializedName("timezone_abbreviation")
    val timezoneAbbreviation: String,
    val elevation: Double,
    @SerializedName("current_units")
    val currentUnits: CurrentUnits,
    val current: CurrentWeather,
    @SerializedName("hourly_units")
    val hourlyUnits: HourlyUnits,
    val hourly: HourlyWeather,
    @SerializedName("daily_units")
    val dailyUnits: DailyUnits? = null,
    val daily: DailyWeather? = null
)

/**
 * Unités de mesure pour les données actuelles
 */
data class CurrentUnits(
    val time: String,
    val interval: String,
    @SerializedName("temperature_2m")
    val temperature2m: String,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: String,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: String,
    @SerializedName("weather_code")
    val weatherCode: String,
    @SerializedName("grass_pollen")
    val grassPollen: String? = null,
    @SerializedName("birch_pollen")
    val birchPollen: String? = null,
    @SerializedName("mugwort_pollen")
    val mugwortPollen: String? = null,
    @SerializedName("ragweed_pollen")
    val ragweedPollen: String? = null
)

/**
 * Données météo actuelles
 */
data class CurrentWeather(
    val time: String,
    val interval: Int,
    @SerializedName("temperature_2m")
    val temperature2m: Double,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: Int,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: Double,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("grass_pollen")
    val grassPollen: Double? = null,
    @SerializedName("birch_pollen")
    val birchPollen: Double? = null,
    @SerializedName("mugwort_pollen")
    val mugwortPollen: Double? = null,
    @SerializedName("ragweed_pollen")
    val ragweedPollen: Double? = null
)

/**
 * Unités de mesure pour les données horaires
 */
data class HourlyUnits(
    val time: String,
    @SerializedName("temperature_2m")
    val temperature2m: String,
    @SerializedName("precipitation_probability")
    val precipitationProbability: String
)

/**
 * Données météo horaires (prévisions heure par heure)
 */
data class HourlyWeather(
    val time: List<String>,
    @SerializedName("temperature_2m")
    val temperature2m: List<Double>,
    @SerializedName("precipitation_probability")
    val precipitationProbability: List<Int>
)

/**
 * Unités de mesure pour les données quotidiennes
 */
data class DailyUnits(
    val time: String,
    @SerializedName("temperature_2m_max")
    val temperature2mMax: String,
    @SerializedName("temperature_2m_min")
    val temperature2mMin: String,
    @SerializedName("precipitation_probability_max")
    val precipitationProbabilityMax: String,
    @SerializedName("weather_code")
    val weatherCode: String
)

/**
 * Données météo quotidiennes (prévisions jour par jour)
 */
data class DailyWeather(
    val time: List<String>,
    @SerializedName("temperature_2m_max")
    val temperature2mMax: List<Double>,
    @SerializedName("temperature_2m_min")
    val temperature2mMin: List<Double>,
    @SerializedName("precipitation_probability_max")
    val precipitationProbabilityMax: List<Int>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>
)

/**
 * Modèle simplifié pour l'affichage dans l'app
 */
data class WeatherData(
    val currentTemperature: Double,
    val currentHumidity: Int,
    val currentWindSpeed: Double,
    val weatherCode: Int,
    val hourlyForecasts: List<HourlyForecast>,
    val dailyForecasts: List<DailyForecast>,
    val grassPollen: Double?,
    val birchPollen: Double?,
    val weedPollen: Double? // Combinaison de mugwort et ragweed
)

/**
 * Prévision horaire simplifiée
 */
data class HourlyForecast(
    val hour: String,          // Format: "14:00"
    val temperature: Double,   // Température en °C
    val precipitationProb: Int // Probabilité de précipitation (0-100%)
)

/**
 * Prévision quotidienne simplifiée
 */
data class DailyForecast(
    val date: String,           // Format: "2025-11-30"
    val dayName: String,        // Format: "Lundi"
    val temperatureMax: Double, // Température max en °C
    val temperatureMin: Double, // Température min en °C
    val precipitationProb: Int, // Probabilité max de précipitation (0-100%)
    val weatherCode: Int        // Code météo WMO
)

/**
 * Convertit la réponse API en modèle simplifié pour l'app
 */
fun WeatherApiResponse.toWeatherData(): WeatherData {
    // Récupère uniquement les 24 prochaines heures
    val next24Hours = hourly.time.take(24).mapIndexed { index, timeString ->
        HourlyForecast(
            hour = formatHour(timeString),
            temperature = hourly.temperature2m.getOrNull(index) ?: 0.0,
            precipitationProb = hourly.precipitationProbability.getOrNull(index) ?: 0
        )
    }

    // Récupère les prévisions quotidiennes (si disponibles)
    val dailyForecastsList = if (daily != null) {
        daily.time.mapIndexed { index, dateString ->
            DailyForecast(
                date = dateString,
                dayName = formatDayName(dateString),
                temperatureMax = daily.temperature2mMax.getOrNull(index) ?: 0.0,
                temperatureMin = daily.temperature2mMin.getOrNull(index) ?: 0.0,
                precipitationProb = daily.precipitationProbabilityMax.getOrNull(index) ?: 0,
                weatherCode = daily.weatherCode.getOrNull(index) ?: 0
            )
        }
    } else {
        emptyList()
    }

    return WeatherData(
        currentTemperature = current.temperature2m,
        currentHumidity = current.relativeHumidity2m,
        currentWindSpeed = current.windSpeed10m,
        weatherCode = current.weatherCode,
        hourlyForecasts = next24Hours,
        dailyForecasts = dailyForecastsList,
        grassPollen = current.grassPollen,
        birchPollen = current.birchPollen,
        weedPollen = maxOf(current.mugwortPollen ?: 0.0, current.ragweedPollen ?: 0.0)
    )
}

/**
 * Formate une chaîne ISO 8601 en heure (HH:00)
 * Ex: "2025-11-29T14:00" -> "14:00"
 */
private fun formatHour(isoDateTime: String): String {
    return try {
        val timePart = isoDateTime.split("T")[1]
        timePart.substring(0, 5) // Prend les 5 premiers caractères (HH:MM)
    } catch (e: Exception) {
        "00:00"
    }
}

/**
 * Formate une date ISO en nom de jour
 * Ex: "2025-11-30" -> "Dimanche"
 */
private fun formatDayName(isoDate: String): String {
    return try {
        val parts = isoDate.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Month is 0-indexed in Calendar
        val day = parts[2].toInt()

        val calendar = java.util.Calendar.getInstance()
        val today = java.util.Calendar.getInstance()
        calendar.set(year, month, day)

        // Si c'est aujourd'hui
        if (calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "Aujourd'hui"
        }

        // Si c'est demain
        today.add(java.util.Calendar.DAY_OF_YEAR, 1)
        if (calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "Demain"
        }

        // Sinon, renvoie le nom du jour
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        when (dayOfWeek) {
            java.util.Calendar.MONDAY -> "Lundi"
            java.util.Calendar.TUESDAY -> "Mardi"
            java.util.Calendar.WEDNESDAY -> "Mercredi"
            java.util.Calendar.THURSDAY -> "Jeudi"
            java.util.Calendar.FRIDAY -> "Vendredi"
            java.util.Calendar.SATURDAY -> "Samedi"
            java.util.Calendar.SUNDAY -> "Dimanche"
            else -> isoDate
        }
    } catch (e: Exception) {
        isoDate
    }
}

/**
 * Convertit un code météo WMO en description textuelle
 * Documentation: https://open-meteo.com/en/docs
 */
fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Ciel dégagé"
        1 -> "Principalement dégagé"
        2 -> "Partiellement nuageux"
        3 -> "Couvert"
        45, 48 -> "Brouillard"
        51, 53, 55 -> "Bruine"
        56, 57 -> "Bruine verglaçante"
        61, 63, 65 -> "Pluie"
        66, 67 -> "Pluie verglaçante"
        71, 73, 75 -> "Neige"
        77 -> "Grains de neige"
        80, 81, 82 -> "Averses de pluie"
        85, 86 -> "Averses de neige"
        95 -> "Orage"
        96, 99 -> "Orage avec grêle"
        else -> "Inconnu"
    }
}

/**
 * Convertit un code météo WMO en icône Material
 * Documentation: https://open-meteo.com/en/docs
 */
fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Filled.WbSunny              // Ciel dégagé
        1 -> Icons.Filled.WbSunny              // Principalement dégagé
        2 -> Icons.Filled.CloudQueue           // Partiellement nuageux
        3 -> Icons.Filled.Cloud                // Couvert
        45, 48 -> Icons.Filled.Cloud           // Brouillard
        51, 53, 55 -> Icons.Filled.WaterDrop   // Bruine
        56, 57 -> Icons.Filled.AcUnit          // Bruine verglaçante
        61, 63, 65 -> Icons.Filled.WaterDrop   // Pluie
        66, 67 -> Icons.Filled.AcUnit          // Pluie verglaçante
        71, 73, 75 -> Icons.Filled.AcUnit      // Neige
        77 -> Icons.Filled.AcUnit              // Grains de neige
        80, 81, 82 -> Icons.Filled.WaterDrop   // Averses de pluie
        85, 86 -> Icons.Filled.AcUnit          // Averses de neige
        95 -> Icons.Filled.FlashOn             // Orage
        96, 99 -> Icons.Filled.FlashOn         // Orage avec grêle
        else -> Icons.AutoMirrored.Filled.Help              // Inconnu
    }
}

/**
 * Détermine si le code météo indique de la pluie
 */
fun isRaining(code: Int): Boolean {
    return code in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
}

/**
 * Interprète le niveau de pollen (grains/m³)
 * @param value Niveau de pollen en grains/m³
 * @return Niveau textuel : "Faible", "Modéré", "Élevé", "Très élevé", ou "Aucun" si null
 */
fun getPollenLevel(value: Double?): String {
    return when {
        value == null -> "Aucun"
        value < 20 -> "Faible"
        value < 50 -> "Modéré"
        value < 100 -> "Élevé"
        else -> "Très élevé"
    }
}

/**
 * Obtient la couleur pour un niveau de pollen
 * @param value Niveau de pollen en grains/m³
 * @return Code couleur hexadécimal
 */
fun getPollenColor(value: Double?): Long {
    return when {
        value == null -> 0xFF6B7280 // Gris pour N/D
        value < 20 -> 0xFF10B981 // Vert pour faible
        value < 50 -> 0xFFFBBF24 // Jaune pour modéré
        value < 100 -> 0xFFF59E0B // Orange pour élevé
        else -> 0xFFEF4444 // Rouge pour très élevé
    }
}

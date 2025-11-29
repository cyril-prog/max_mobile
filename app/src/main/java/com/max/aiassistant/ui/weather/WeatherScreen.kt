package com.max.aiassistant.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.data.api.CityResult
import com.max.aiassistant.data.api.HourlyForecast
import com.max.aiassistant.data.api.WeatherData
import com.max.aiassistant.data.api.getWeatherDescription
import com.max.aiassistant.data.api.getWeatherIcon
import com.max.aiassistant.data.api.getPollenLevel
import com.max.aiassistant.data.api.getPollenColor
import com.max.aiassistant.ui.theme.DarkBackground
import com.max.aiassistant.ui.theme.DarkSurface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

/**
 * Écran météo avec prévisions heure par heure
 * Affiche la température actuelle et les prévisions pour les 24 prochaines heures
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    weatherData: WeatherData?,
    cityName: String,
    citySearchResults: List<CityResult>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (CityResult) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // État pour afficher/masquer le dialog de sélection de ville
    var showCityDialog by remember { mutableStateOf(false) }

    if (showCityDialog) {
        CitySelectionDialog(
            currentCity = cityName,
            citySearchResults = citySearchResults,
            onDismiss = { showCityDialog = false },
            onSearchCity = onSearchCity,
            onSelectCity = { city ->
                onSelectCity(city)
                showCityDialog = false
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(onNavigateBack) {
                        var cumulativeDrag = 0f
                        var swipeTriggered = false
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0f) {
                                    cumulativeDrag += dragAmount
                                    if (!swipeTriggered && cumulativeDrag >= 80f) {
                                        swipeTriggered = true
                                        onNavigateBack()
                                    }
                                }
                            },
                            onDragEnd = {
                                cumulativeDrag = 0f
                                swipeTriggered = false
                            },
                            onDragCancel = {
                                cumulativeDrag = 0f
                                swipeTriggered = false
                            }
                        )
                    }
            ) {
                if (weatherData == null) {
                    // État de chargement initial
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF0A84FF))
                    }
                } else {
                    // Affichage des données météo
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Carte de température actuelle
                        CurrentWeatherCard(
                            weatherData = weatherData,
                            cityName = cityName,
                            onClick = { showCityDialog = true }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Carte des allergies (pollens)
                        AllergyCard(weatherData = weatherData)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Onglets pour choisir entre heure par heure et jour par jour
                        var selectedTabIndex by remember { mutableStateOf(0) }
                        val tabs = listOf("Heure par heure", "Jour par jour")

                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = DarkBackground,
                            contentColor = Color(0xFF0A84FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selectedTabIndex == index) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Affichage selon l'onglet sélectionné
                        when (selectedTabIndex) {
                            0 -> {
                                // Liste verticale des prévisions horaires (uniquement heures futures)
                                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                val futureForecasts = weatherData.hourlyForecasts.filter { forecast ->
                                    val forecastHour = forecast.hour.split(":")[0].toIntOrNull() ?: 0
                                    // Ne garder que les heures strictement supérieures à l'heure actuelle
                                    forecastHour > currentHour
                                }

                                // Prévisions affichées verticalement
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    futureForecasts.forEach { forecast ->
                                        HourlyForecastCardHorizontal(forecast = forecast)
                                    }
                                }
                            }
                            1 -> {
                                // Liste verticale des prévisions quotidiennes
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    weatherData.dailyForecasts.forEach { forecast ->
                                        DailyForecastCard(forecast = forecast)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Carte affichant la température et les conditions actuelles
 */
@Composable
fun CurrentWeatherCard(
    weatherData: WeatherData,
    cityName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 176.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A5F),
                            Color(0xFF0F1C33)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = cityName,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${weatherData.currentTemperature.toInt()}°C",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = getWeatherIcon(weatherData.weatherCode),
                            contentDescription = getWeatherDescription(weatherData.weatherCode),
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    val precipitationProb = weatherData.hourlyForecasts.firstOrNull()?.precipitationProb ?: 0
                    val metrics = listOf(
                        WeatherMetric(
                            icon = Icons.Filled.WaterDrop,
                            value = "${weatherData.currentHumidity}%",
                            label = "Humidité",
                            alignment = Alignment.Start
                        ),
                        WeatherMetric(
                            icon = Icons.Filled.Air,
                            value = "${weatherData.currentWindSpeed.toInt()} km/h",
                            label = "Vent",
                            alignment = Alignment.CenterHorizontally,
                            weight = 1.4f
                        ),
                        WeatherMetric(
                            icon = Icons.Filled.Umbrella,
                            value = if (precipitationProb > 0) "${precipitationProb}%" else "Non",
                            label = "Pluie",
                            alignment = Alignment.End
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        metrics.forEach { metric ->
                            WeatherMetricColumn(
                                metric = metric,
                                modifier = Modifier.weight(metric.weight)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class WeatherMetric(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val alignment: Alignment.Horizontal,
    val weight: Float = 1f
)

@Composable
private fun WeatherMetricColumn(
    metric: WeatherMetric,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = metric.alignment,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val textAlign = when (metric.alignment) {
            Alignment.CenterHorizontally -> TextAlign.Center
            Alignment.End -> TextAlign.End
            else -> TextAlign.Start
        }

        Text(
            text = metric.label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = when (metric.alignment) {
                Alignment.CenterHorizontally -> Arrangement.Center
                Alignment.End -> Arrangement.End
                else -> Arrangement.Start
            }
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = metric.label,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = metric.value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CitySelectionDialog(
    currentCity: String,
    citySearchResults: List<CityResult>,
    onDismiss: () -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (CityResult) -> Unit
) {
    var cityQuery by rememberSaveable(currentCity) { mutableStateOf(currentCity) }
    var hasSearched by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une ville") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ville") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            onSearchCity(cityQuery.trim())
                            hasSearched = true
                        },
                        enabled = cityQuery.trim().length >= 2
                    ) {
                        Text("Rechercher")
                    }
                }

                if (citySearchResults.isEmpty()) {
                    val helperText = if (hasSearched) {
                        "Aucun résultat, essayez un autre nom."
                    } else {
                        "Saisissez au moins 2 lettres puis lancez une recherche."
                    }
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(citySearchResults) { city ->
                            CityResultRow(
                                city = city,
                                onSelect = { onSelectCity(city) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun CityResultRow(
    city: CityResult,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface.copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = city.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = city.getFullName(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Carte affichant les informations sur les allergies (pollens)
 */
@Composable
fun AllergyCard(weatherData: WeatherData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A5F),
                            Color(0xFF0F1C33)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Titre
                Text(
                    text = "Allergies",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )

                // Row pour les 3 types de pollens
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Graminées
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Grass,
                            contentDescription = "Graminées",
                            tint = Color(getPollenColor(weatherData.grassPollen)),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getPollenLevel(weatherData.grassPollen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Graminées",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Arbres (Bouleau)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Park,
                            contentDescription = "Arbres",
                            tint = Color(getPollenColor(weatherData.birchPollen)),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getPollenLevel(weatherData.birchPollen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Arbres",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Herbacées (Armoise, Ambroisie)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFlorist,
                            contentDescription = "Herbacées",
                            tint = Color(getPollenColor(weatherData.weedPollen)),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getPollenLevel(weatherData.weedPollen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Herbacées",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carte pour une prévision horaire (ancienne version - non utilisée)
 */
@Composable
fun HourlyForecastCard(forecast: HourlyForecast) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Heure
            Text(
                text = forecast.hour,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )

            // Température
            Text(
                text = "${forecast.temperature.toInt()}°C",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Indicateur de pluie
            if (forecast.precipitationProb > 30) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Pluie",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${forecast.precipitationProb}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF60A5FA)
                    )
                }
            } else {
                // Espace vide pour garder la même hauteur
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Carte horizontale pour une prévision horaire
 * Affiche l'heure à gauche, température et pluie à droite
 */
@Composable
fun HourlyForecastCardHorizontal(forecast: HourlyForecast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heure
            Text(
                text = forecast.hour,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            // Température et pluie
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Température
                Text(
                    text = "${forecast.temperature.toInt()}°C",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Indicateur de pluie
                if (forecast.precipitationProb > 30) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.width(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Pluie",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${forecast.precipitationProb}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF60A5FA),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Texte "Pas de pluie" pour garder l'alignement
                    Text(
                        text = "Sec",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

/**
 * Carte pour une prévision quotidienne
 * Affiche le jour, température min/max et pluie
 */
@Composable
fun DailyForecastCard(forecast: com.max.aiassistant.data.api.DailyForecast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Jour de la semaine - prend tout l'espace disponible
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = forecast.dayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = forecast.date.split("-").let { "${it[2]}/${it[1]}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Icône météo - largeur fixe
            Icon(
                imageVector = com.max.aiassistant.data.api.getWeatherIcon(forecast.weatherCode),
                contentDescription = com.max.aiassistant.data.api.getWeatherDescription(forecast.weatherCode),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )

            // Températures min/max - largeur fixe
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(50.dp)
            ) {
                Text(
                    text = "${forecast.temperatureMax.toInt()}°",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${forecast.temperatureMin.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Indicateur de pluie - largeur fixe
            if (forecast.precipitationProb > 30) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Pluie",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${forecast.precipitationProb}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "Sec",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

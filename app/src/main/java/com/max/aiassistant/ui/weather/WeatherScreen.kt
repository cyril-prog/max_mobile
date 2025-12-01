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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Settings
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
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.geometry.Offset
import java.util.Calendar

/**
 * Écran météo avec prévisions heure par heure
 * Affiche la température actuelle et les prévisions pour les 24 prochaines heures
 * - Sidebar de navigation accessible par swipe vers la gauche depuis le bord droit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    weatherData: WeatherData?,
    cityName: String,
    citySearchResults: List<CityResult>,
    showAllergies: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (CityResult) -> Unit,
    onSetShowAllergies: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onRadarClick: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToPlanning: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()
    
    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.WEATHER,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.VOICE -> onNavigateBack()
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> { /* Déjà sur cet écran */ }
                NavigationScreen.NOTES -> onNavigateToNotes()
            }
        },
        sidebarState = sidebarState
    ) {
        WeatherScreenContent(
            weatherData = weatherData,
            cityName = cityName,
            citySearchResults = citySearchResults,
            showAllergies = showAllergies,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onSearchCity = onSearchCity,
            onSelectCity = onSelectCity,
            onSetShowAllergies = onSetShowAllergies,
            onNavigateBack = onNavigateBack,
            onRadarClick = onRadarClick,
            modifier = modifier
        )
    }
}

/**
 * Contenu de l'écran Météo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherScreenContent(
    weatherData: WeatherData?,
    cityName: String,
    citySearchResults: List<CityResult>,
    showAllergies: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (CityResult) -> Unit,
    onSetShowAllergies: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onRadarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // État pour afficher/masquer le dialog des paramètres météo
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        WeatherSettingsDialog(
            currentCity = cityName,
            citySearchResults = citySearchResults,
            showAllergies = showAllergies,
            onDismiss = { showSettingsDialog = false },
            onSearchCity = onSearchCity,
            onSelectCity = { city ->
                onSelectCity(city)
                showSettingsDialog = false
            },
            onSetShowAllergies = onSetShowAllergies
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Carte de température actuelle - FIXE
                        CurrentWeatherCard(
                            weatherData = weatherData,
                            cityName = cityName,
                            onOpenSettings = { showSettingsDialog = true },
                            onRadarClick = onRadarClick
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Carte des allergies (pollens) - FIXE - affichée selon les préférences
                        if (showAllergies) {
                            AllergyCard(weatherData = weatherData)
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Spacer(modifier = Modifier.height(0.dp))
                        }

                        // Onglets pour choisir entre heure par heure et jour par jour - FIXE
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

                        // Affichage selon l'onglet sélectionné - SCROLLABLE
                        when (selectedTabIndex) {
                            0 -> {
                                // Liste verticale des prévisions horaires (24 prochaines heures)
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(weatherData.hourlyForecasts) { forecast ->
                                        HourlyForecastCardHorizontal(forecast = forecast)
                                    }
                                }
                            }
                            1 -> {
                                // Liste verticale des prévisions quotidiennes
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(weatherData.dailyForecasts) { forecast ->
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
 * Retourne les couleurs de dégradé selon les conditions météorologiques
 * @param weatherCode Code météo WMO
 * @return Paire de couleurs (début, fin) pour le dégradé
 */
private fun getWeatherGradient(weatherCode: Int): Pair<Color, Color> {
    // Déterminer si c'est la nuit (entre 20h et 6h)
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = currentHour >= 20 || currentHour < 6

    return when {
        // Ciel dégagé (codes 0, 1)
        weatherCode in listOf(0, 1) -> {
            if (isNight) {
                // Nuit claire : #283D5B → #364F6B
                Pair(Color(0xFF283D5B), Color(0xFF364F6B))
            } else {
                // Ensoleillé jour : #5B9BD5 → #7CB3E0
                Pair(Color(0xFF5B9BD5), Color(0xFF7CB3E0))
            }
        }
        // Brouillard (codes 45, 48)
        weatherCode in listOf(45, 48) -> {
            // Brumeux : #6B7F8C → #7A8D9A
            Pair(Color(0xFF6B7F8C), Color(0xFF7A8D9A))
        }
        // Pluie et bruine (codes 51-67, 80-82)
        weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) -> {
            // Pluvieux : #455A64 → #546E7A
            Pair(Color(0xFF455A64), Color(0xFF546E7A))
        }
        // Neige (codes 71-77, 85-86)
        weatherCode in listOf(71, 73, 75, 77, 85, 86) -> {
            // Neigeux : #8FA3AD → #A5B8C2
            Pair(Color(0xFF8FA3AD), Color(0xFFA5B8C2))
        }
        // Orage (codes 95, 96, 99)
        weatherCode in listOf(95, 96, 99) -> {
            // Orageux : #3D4E5C → #4A5F6F
            Pair(Color(0xFF3D4E5C), Color(0xFF4A5F6F))
        }
        // Nuageux (codes 2, 3) et défaut
        else -> {
            // Nuageux : #637A87 → #758D9A
            Pair(Color(0xFF637A87), Color(0xFF758D9A))
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
    onOpenSettings: () -> Unit,
    onRadarClick: () -> Unit
) {
    // Récupérer le dégradé adaptatif selon les conditions météo
    val (startColor, endColor) = getWeatherGradient(weatherData.weatherCode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clickable { onRadarClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(startColor, endColor),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Icône de paramètres en haut à droite
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-12).dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Paramètres météo",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

/**
 * Dialog de paramètres météo
 * Permet de choisir la ville et de configurer l'affichage des allergies
 */
@Composable
private fun WeatherSettingsDialog(
    currentCity: String,
    citySearchResults: List<CityResult>,
    showAllergies: Boolean,
    onDismiss: () -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (CityResult) -> Unit,
    onSetShowAllergies: (Boolean) -> Unit
) {
    var cityQuery by rememberSaveable(currentCity) { mutableStateOf(currentCity) }
    var hasSearched by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Paramètres météo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Choix de la ville
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🌍 Ville",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )

                        OutlinedTextField(
                            value = cityQuery,
                            onValueChange = { cityQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Rechercher une ville") },
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
                                enabled = cityQuery.trim().length >= 2,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0A84FF)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Rechercher",
                                    fontWeight = FontWeight.Bold
                                )
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
                                    .heightIn(max = 200.dp)
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
                }

                // Section 2: Options d'affichage
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚙️ Affichage",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )

                        // Switch pour afficher/masquer les allergies
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Afficher les allergies",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Informations sur les pollens",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = showAllergies,
                                onCheckedChange = onSetShowAllergies
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A84FF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Fermer",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    )
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
    var showDetailDialog by remember { mutableStateOf(false) }

    if (showDetailDialog) {
        AllergyDetailDialog(
            weatherData = weatherData,
            onDismiss = { showDetailDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { showDetailDialog = true },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF002C0F),
                            Color(0xFF0D3C1F)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Titre
                Text(
                    text = "Allergies",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
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
                            tint = Color(0xFFF5F5F5),
                            modifier = Modifier.size(27.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Graminées",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = getPollenLevel(weatherData.grassPollen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Arbres (Bouleau, Aulne, Olivier)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Park,
                            contentDescription = "Arbres",
                            tint = Color(0xFFF5F5F5),
                            modifier = Modifier.size(27.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Arbres",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = getPollenLevel(
                                maxOf(
                                    weatherData.birchPollen ?: 0.0,
                                    weatherData.alderPollen ?: 0.0,
                                    weatherData.olivePollen ?: 0.0
                                )
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
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
                            tint = Color(0xFFF5F5F5),
                            modifier = Modifier.size(27.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Herbacées",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = getPollenLevel(maxOf(weatherData.mugwortPollen ?: 0.0, weatherData.ragweedPollen ?: 0.0)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog détaillé affichant toutes les informations sur les allergies par catégorie
 */
@Composable
private fun AllergyDetailDialog(
    weatherData: WeatherData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Détail des allergies",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Section Graminées
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF002C0F).copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Grass,
                                contentDescription = "Graminées",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Graminées",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }

                        PollenDetailRow(
                            name = "Graminées",
                            value = weatherData.grassPollen
                        )
                    }
                }

                // Section Arbres
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E3A28).copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Park,
                                contentDescription = "Arbres",
                                tint = Color(0xFF86EFAC),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Arbres",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC)
                            )
                        }

                        PollenDetailRow(
                            name = "Bouleau",
                            value = weatherData.birchPollen
                        )
                        PollenDetailRow(
                            name = "Aulne",
                            value = weatherData.alderPollen
                        )
                        PollenDetailRow(
                            name = "Olivier",
                            value = weatherData.olivePollen
                        )
                    }
                }

                // Section Herbacées
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D1B0E).copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFlorist,
                                contentDescription = "Herbacées",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Herbacées",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        PollenDetailRow(
                            name = "Armoise",
                            value = weatherData.mugwortPollen
                        )
                        PollenDetailRow(
                            name = "Ambroisie",
                            value = weatherData.ragweedPollen
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0A84FF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Fermer",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    )
}

/**
 * Ligne affichant le détail d'un type de pollen
 */
@Composable
private fun PollenDetailRow(
    name: String,
    value: Double?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Valeur numérique
            Text(
                text = "${(value ?: 0.0).toInt()} grains/m³",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            // Badge de niveau
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(getPollenColor(value ?: 0.0))
            ) {
                Text(
                    text = getPollenLevel(value ?: 0.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
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
 * Affiche l'heure à gauche, icône météo, température et pluie à droite
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heure - prend l'espace disponible
            Text(
                text = forecast.hour,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Indicateur de pluie - largeur fixe (seulement si > 30%)
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
            }

            // Icône météo - largeur fixe
            Icon(
                imageVector = getWeatherIcon(forecast.weatherCode),
                contentDescription = getWeatherDescription(forecast.weatherCode),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )

            // Température - toujours alignée à droite
            Text(
                text = "${forecast.temperature.toInt()}°C",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(55.dp),
                textAlign = TextAlign.End
            )
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

            // Indicateur de pluie - largeur fixe (seulement si > 30%)
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
            }

            // Icône météo - largeur fixe
            Icon(
                imageVector = com.max.aiassistant.data.api.getWeatherIcon(forecast.weatherCode),
                contentDescription = com.max.aiassistant.data.api.getWeatherDescription(forecast.weatherCode),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )

            // Températures min/max - toujours alignées à droite
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
        }
    }
}

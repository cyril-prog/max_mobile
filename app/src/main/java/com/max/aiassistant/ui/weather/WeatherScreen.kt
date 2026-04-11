package com.max.aiassistant.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.max.aiassistant.data.api.*
import com.max.aiassistant.ui.common.*
import com.max.aiassistant.ui.theme.*

private val WeatherBackdrop = Brush.verticalGradient(
    listOf(
        Color(0xFF09131E),
        Color(0xFF0E1826),
        Color(0xFF0C1420),
        Color(0xFF081019)
    )
)
private val WeatherCardSurface = Color(0xFF162234)
private val WeatherCardSurfaceAlt = Color(0xFF1A2940)
private val PollenGradient = Brush.linearGradient(
    listOf(
        Color(0xFF143126),
        Color(0xFF1D4A38),
        Color(0xFF2B6A4E)
    )
)
private val PollenDialogBackground = Color(0xFF202734)
private val PollenBadgeText = Color(0xFFF4FFF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(weatherData: WeatherData?, cityName: String, citySearchResults: List<CityResult>, showAllergies: Boolean, isRefreshing: Boolean, errorMessage: String? = null, isOffline: Boolean = false, onRefresh: () -> Unit, onSearchCity: (String) -> Unit, onSelectCity: (CityResult) -> Unit, onSetShowAllergies: (Boolean) -> Unit, onNavigateBack: () -> Unit, onRadarClick: () -> Unit, onNavigateToChat: () -> Unit = {}, onNavigateToTasks: () -> Unit = {}, onNavigateToPlanning: () -> Unit = {}, onNavigateToNotes: () -> Unit = {}, onNavigateToActu: () -> Unit = {}, showChrome: Boolean = true, modifier: Modifier = Modifier) {
    val body: @Composable () -> Unit = {
        WeatherBody(weatherData, cityName, citySearchResults, showAllergies, isRefreshing, errorMessage, isOffline, onRefresh, onSearchCity, onSelectCity, onSetShowAllergies, onRadarClick, showChrome, modifier)
    }
    if (!showChrome) body() else {
        val sidebarState = rememberNavigationSidebarState()
        NavigationSidebarScaffold(currentScreen = NavigationScreen.WEATHER, onNavigateToScreen = {
            when (it) {
                NavigationScreen.HOME -> onNavigateBack(); NavigationScreen.VOICE -> onNavigateBack(); NavigationScreen.CHAT -> onNavigateToChat(); NavigationScreen.TASKS -> onNavigateToTasks(); NavigationScreen.PLANNING -> onNavigateToPlanning(); NavigationScreen.WEATHER -> Unit; NavigationScreen.NOTES -> onNavigateToNotes(); NavigationScreen.ACTU -> onNavigateToActu()
            }
        }, sidebarState = sidebarState) { body() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherBody(weatherData: WeatherData?, cityName: String, citySearchResults: List<CityResult>, showAllergies: Boolean, isRefreshing: Boolean, errorMessage: String?, isOffline: Boolean, onRefresh: () -> Unit, onSearchCity: (String) -> Unit, onSelectCity: (CityResult) -> Unit, onSetShowAllergies: (Boolean) -> Unit, onRadarClick: () -> Unit, showChrome: Boolean, modifier: Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showPollenDetails by remember { mutableStateOf(false) }
    if (showSettings) WeatherSettingsDialog(cityName, citySearchResults, showAllergies, { showSettings = false }, onSearchCity, { onSelectCity(it); showSettings = false }, onSetShowAllergies)
    if (showPollenDetails && weatherData != null) {
        PollenDetailsDialog(
            weatherData = weatherData,
            onDismiss = { showPollenDetails = false }
        )
    }
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        if (showChrome) Row(Modifier.fillMaxWidth().background(WeatherBackdrop).statusBarsPadding().padding(20.dp, 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Weather board", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold); Text(cityName, color = TextSecondary) }
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Tune, null, tint = TextPrimary) }
        }
    }) { pad ->
        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize().background(WeatherBackdrop).padding(pad)) {
            when {
                weatherData == null && isRefreshing -> LoadingStateView(title = "Chargement meteo", subtitle = "Preparation du tableau du jour.", modifier = Modifier.fillMaxSize().padding(24.dp))
                weatherData == null && errorMessage != null -> ErrorStateView(title = "Meteo indisponible", subtitle = errorMessage, onRetry = onRefresh, modifier = Modifier.fillMaxSize().padding(24.dp))
                weatherData == null && isOffline -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { InlineStatusBanner(title = "Connexion indisponible", subtitle = "La meteo a besoin du reseau pour se rafraichir.", tone = BannerTone.Offline) }
                weatherData == null -> ErrorStateView(title = "Aucune donnee meteo", subtitle = "Choisissez une ville ou relancez le chargement.", onRetry = onRefresh, modifier = Modifier.fillMaxSize().padding(24.dp))
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WeatherBackdrop),
                    contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item { WeatherHero(cityName, weatherData, onRadarClick, { showSettings = true }) }
                    if (showAllergies) {
                        item { PollenCard(weatherData, onClick = { showPollenDetails = true }) }
                    }
                    if (errorMessage != null) item { InlineStatusBanner(title = "Rafraichissement incomplet", subtitle = errorMessage, tone = BannerTone.Warning) } else if (isOffline) item { InlineStatusBanner(title = "Mode hors ligne", subtitle = "Affichage des dernieres donnees connues.", tone = BannerTone.Offline) }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Heures") }, colors = chipColors()); FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Semaine") }, colors = chipColors()) } }
                    if (tab == 0) {
                        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(weatherData.hourlyForecasts.take(8)) { HourCard(it) } } }
                    } else {
                        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(weatherData.dailyForecasts.take(7)) { DayCard(it) } } }
                    }
                }
            }
        }
    }
}

@Composable private fun WeatherHero(cityName: String, weatherData: WeatherData, onRadarClick: () -> Unit, onSettings: () -> Unit) {
    val colors = when (weatherData.weatherCode) { 0,1 -> listOf(Color(0xFF27517E), Color(0xFF5FA4E5)); 45,48 -> listOf(Color(0xFF2D3B4A), Color(0xFF5E7387)); 95,96,99 -> listOf(Color(0xFF1D2238), Color(0xFF435274)); else -> listOf(GradientWeather.first(), GradientWeather.last()) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.background(Brush.verticalGradient(colors)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(cityName.ifBlank { "Ville" }) }, leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.14f), labelColor = Color.White, leadingIconContentColor = Color.White))
                    Text(fmt(weatherData.currentTemperature), style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) { IconButton(onClick = onRadarClick) { Icon(Icons.Default.SatelliteAlt, "Vue satellite", tint = Color.White) } }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Reglages", tint = Color.White) } }
                    }
                    Icon(getWeatherIcon(weatherData.weatherCode), getWeatherDescription(weatherData.weatherCode), tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard("Humidite", "${weatherData.currentHumidity}%", Icons.Default.WaterDrop, Modifier.weight(1f)); StatCard("Vent", "${weatherData.currentWindSpeed.toInt()} km/h", Icons.Default.Air, Modifier.weight(1f)) }
        }
    }
}

@Composable private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) { Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))) { Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Column { Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f)); Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold) } } } }
@Composable private fun HourCard(forecast: HourlyForecast) { Card(Modifier.width(158.dp).height(128.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WeatherCardSurfaceAlt)) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(forecast.hour, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold); Icon(getWeatherIcon(forecast.weatherCode), null, tint = AccentBlue, modifier = Modifier.size(22.dp)) }; Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(fmt(forecast.temperature), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold); Text(getWeatherDescription(forecast.weatherCode), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1); Text("Humidité ${forecast.humidity}%", style = MaterialTheme.typography.labelMedium, color = TextSecondary) } } } }
@Composable private fun DayCard(forecast: DailyForecast) { Card(Modifier.width(178.dp).height(128.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WeatherCardSurfaceAlt)) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Text(forecast.dayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold); Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(AccentBlue.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(getWeatherIcon(forecast.weatherCode), null, tint = AccentBlue, modifier = Modifier.size(22.dp)) } }; Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${fmt(forecast.temperatureMin)} / ${fmt(forecast.temperatureMax)}", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold); Text(getWeatherDescription(forecast.weatherCode), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1) } } } }
@Composable
private fun PollenCard(
    weatherData: WeatherData,
    onClick: () -> Unit
) {
    val items = listOf(
        "Graminées" to (weatherData.grassPollen ?: 0.0),
        "Arbres" to maxOf(
            weatherData.birchPollen ?: 0.0,
            weatherData.alderPollen ?: 0.0,
            weatherData.olivePollen ?: 0.0
        ),
        "Herbacées" to maxOf(
            weatherData.mugwortPollen ?: 0.0,
            weatherData.ragweedPollen ?: 0.0
        )
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(PollenGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "RESPIRATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA9F5C8)
                    )
                    Text(
                        "Niveaux pollens",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Touchez pour ouvrir le detail pollen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.74f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color.White.copy(alpha = 0.84f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { (label, value) ->
                    val chipWeight = when (label) {
                        "Graminées" -> 1.2f
                        "Herbacées" -> 1.15f
                        else -> 0.825f
                    }
                    Surface(
                        modifier = Modifier.weight(chipWeight),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                getPollenLevel(value),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun PollenDetailsDialog(
    weatherData: WeatherData,
    onDismiss: () -> Unit
) {
    val sections = listOf(
        PollenSection(
            title = "Graminées",
            icon = Icons.Default.Spa,
            accentColor = Color(0xFF59E297),
            containerColor = Color(0xFF103721),
            rows = listOf(
                "Graminées" to weatherData.grassPollen
            )
        ),
        PollenSection(
            title = "Arbres",
            icon = Icons.Default.Park,
            accentColor = Color(0xFF8EE7A7),
            containerColor = Color(0xFF213A2F),
            rows = listOf(
                "Bouleau" to weatherData.birchPollen,
                "Aulne" to weatherData.alderPollen,
                "Olivier" to weatherData.olivePollen
            )
        ),
        PollenSection(
            title = "Herbacées",
            icon = Icons.Default.LocalFlorist,
            accentColor = Color(0xFFF4C24D),
            containerColor = Color(0xFF3A2B20),
            rows = listOf(
                "Armoise" to weatherData.mugwortPollen,
                "Ambroisie" to weatherData.ragweedPollen
            )
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PollenDialogBackground,
        title = {
            Text(
                "Détail des allergies",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sections.forEach { section ->
                    PollenDetailSection(section)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Fermer")
            }
        }
    )
}
private data class PollenSection(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val containerColor: Color,
    val rows: List<Pair<String, Double?>>
)

@Composable
private fun PollenDetailSection(section: PollenSection) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = section.containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = section.title,
                    tint = section.accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    section.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = section.accentColor,
                    fontWeight = FontWeight.Medium
                )
            }
            section.rows.forEach { (label, value) ->
                PollenDetailRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun PollenDetailRow(
    label: String,
    value: Double?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                formatPollenValue(value),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f)
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(getPollenColor(value))
        ) {
            Text(
                text = getPollenLevel(value),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = PollenBadgeText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable private fun WeatherSettingsDialog(currentCity: String, citySearchResults: List<CityResult>, showAllergies: Boolean, onDismiss: () -> Unit, onSearchCity: (String) -> Unit, onSelectCity: (CityResult) -> Unit, onSetShowAllergies: (Boolean) -> Unit) { var query by remember(currentCity) { mutableStateOf(currentCity) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Personnaliser la meteo", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { OutlinedTextField(value = query, onValueChange = { query = it; onSearchCity(it) }, singleLine = true, label = { Text("Ville") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth()); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Afficher les pollens", fontWeight = FontWeight.SemiBold); Text("Ajoute une synthese respiration dans le board.", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }; Switch(checked = showAllergies, onCheckedChange = onSetShowAllergies) }; if (citySearchResults.isNotEmpty()) { HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f)); citySearchResults.take(5).forEach { city -> Surface(onClick = { onSelectCity(city) }, shape = RoundedCornerShape(16.dp), color = DarkSurface) { Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MyLocation, null, tint = AccentBlue, modifier = Modifier.size(18.dp)); Column { Text(city.name, fontWeight = FontWeight.SemiBold); Text(listOfNotNull(city.admin1, city.country).joinToString(", "), style = MaterialTheme.typography.bodySmall, color = TextSecondary) } } } } } } }, confirmButton = { Button(onClick = onDismiss) { Text("Fermer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }) }
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentBlue.copy(alpha = 0.18f), selectedLabelColor = AccentBlue, containerColor = DarkSurface, labelColor = TextSecondary)
private fun fmt(v: Double) = "${v.toInt()}°"
private fun formatPollenValue(value: Double?): String = "${(value ?: 0.0).toInt()} grains/m³"

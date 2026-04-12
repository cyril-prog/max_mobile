package com.max.aiassistant.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.data.api.WeatherData
import com.max.aiassistant.data.api.getWeatherDescription
import com.max.aiassistant.data.api.getWeatherIcon
import com.max.aiassistant.model.Event
import com.max.aiassistant.model.Task
import com.max.aiassistant.model.TaskStatus
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.theme.AccentBlue
import com.max.aiassistant.ui.theme.CompletedGreen
import com.max.aiassistant.ui.theme.GradientWeather
import com.max.aiassistant.ui.theme.HighOrange
import com.max.aiassistant.ui.theme.TextPrimary
import com.max.aiassistant.ui.theme.TextSecondary
import java.util.Calendar

private val HomeBackdrop = Brush.verticalGradient(
    listOf(
        Color(0xFF0B1420),
        Color(0xFF101A29),
        Color(0xFF0F1724),
        Color(0xFF0A1119)
    )
)
@Composable
fun HomeScreen(
    tasks: List<Task>,
    events: List<Event>,
    weatherData: WeatherData?,
    cityName: String,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToActu: () -> Unit,
    showChrome: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (showChrome) {
        val sidebarState = rememberNavigationSidebarState()

        NavigationSidebarScaffold(
            currentScreen = NavigationScreen.HOME,
            onNavigateToScreen = { screen ->
                when (screen) {
                    NavigationScreen.HOME -> {}
                    NavigationScreen.VOICE -> onNavigateToVoice()
                    NavigationScreen.CHAT -> onNavigateToChat()
                    NavigationScreen.TASKS -> onNavigateToTasks()
                    NavigationScreen.PLANNING -> onNavigateToPlanning()
                    NavigationScreen.WEATHER -> onNavigateToWeather()
                    NavigationScreen.NOTES -> onNavigateToNotes()
                    NavigationScreen.ACTU -> onNavigateToActu()
                }
            },
            sidebarState = sidebarState
        ) {
            HomeScreenContent(
                tasks = tasks,
                events = events,
                weatherData = weatherData,
                cityName = cityName,
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToPlanning = onNavigateToPlanning,
                onNavigateToWeather = onNavigateToWeather,
                onNavigateToRadar = onNavigateToRadar,
                modifier = modifier
            )
        }
    } else {
        HomeScreenContent(
            tasks = tasks,
            events = events,
            weatherData = weatherData,
            cityName = cityName,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToPlanning = onNavigateToPlanning,
            onNavigateToWeather = onNavigateToWeather,
            onNavigateToRadar = onNavigateToRadar,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeScreenContent(
    tasks: List<Task>,
    events: List<Event>,
    weatherData: WeatherData?,
    cityName: String,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(brush = HomeBackdrop),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeWeatherPanel(
                weatherData = weatherData,
                cityName = cityName,
                onClick = onNavigateToWeather,
                onRadarClick = onNavigateToRadar
            )
        }
        item {
            CompactOverviewPanel(
                tasks = tasks,
                events = events,
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToPlanning = onNavigateToPlanning
            )
        }
    }
}

@Composable
private fun HomeWeatherPanel(
    weatherData: WeatherData?,
    cityName: String,
    onClick: () -> Unit,
    onRadarClick: () -> Unit
) {
    val weatherLabel = weatherData?.let { getWeatherDescription(it.weatherCode) } ?: "Meteo indisponible"
    val temperature = weatherData?.let { "${it.currentTemperature.toInt()}\u00B0" } ?: "--"
    val humidityValue = weatherData?.currentHumidity?.let { "$it%" } ?: "--"
    val windValue = weatherData?.currentWindSpeed?.toInt()?.let { "$it km/h" } ?: "--"
    val backgroundColors = homeWeatherHeroColors(weatherData?.weatherCode)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(backgroundColors))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(cityName.ifBlank { "Ville" }) },
                        leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.14f),
                            labelColor = Color.White,
                            leadingIconContentColor = Color.White
                        )
                    )
                    Text(
                        text = temperature,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            IconButton(onClick = onRadarClick) {
                                Icon(
                                    imageVector = Icons.Default.SatelliteAlt,
                                    contentDescription = "Vue satellite",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = weatherData?.let { getWeatherIcon(it.weatherCode) } ?: Icons.Default.CloudQueue,
                        contentDescription = weatherLabel,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeWeatherStatCard(Icons.Default.WaterDrop, "Humidite", humidityValue, Modifier.weight(1f))
                HomeWeatherStatCard(Icons.Default.Air, "Vent", windValue, Modifier.weight(1f))
            }
        }
    }
}

private fun homeWeatherHeroColors(weatherCode: Int?): List<Color> {
    return when (weatherCode) {
        0, 1 -> listOf(Color(0xFF27517E), Color(0xFF5FA4E5))
        45, 48 -> listOf(Color(0xFF2D3B4A), Color(0xFF5E7387))
        95, 96, 99 -> listOf(Color(0xFF1D2238), Color(0xFF435274))
        else -> listOf(GradientWeather.first(), GradientWeather.last())
    }
}

@Composable
private fun HomeWeatherStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompactOverviewPanel(
    tasks: List<Task>,
    events: List<Event>,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit
) {
    val pendingTasks = remember(tasks) { tasks.filter { it.status != TaskStatus.COMPLETED } }
    val todayEvents = remember(events) { events.filter { isEventToday(it) } }
    val nextEvent = remember(todayEvents) { todayEvents.firstOrNull() }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF121A26), Color(0xFF1A2232), Color(0xFF1B1E2C))
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DigestRow(
                accent = if (pendingTasks.isEmpty()) CompletedGreen else HighOrange,
                icon = Icons.Default.CheckCircle,
                title = if (pendingTasks.isEmpty()) "Taches sous controle" else "${pendingTasks.size} tache${if (pendingTasks.size > 1) "s" else ""} ouvertes",
                detail = pendingTasks.firstOrNull()?.title ?: "Aucune urgence en attente",
                cta = "Ouvrir",
                onClick = onNavigateToTasks
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            DigestRow(
                accent = AccentBlue,
                icon = Icons.Default.Schedule,
                title = nextEvent?.title ?: "Aucun evenement prevu",
                detail = nextEvent?.let { eventTiming(it) } ?: "Journee libre pour le moment",
                cta = "Agenda",
                onClick = onNavigateToPlanning
            )
        }
    }
}

@Composable
private fun DigestRow(
    accent: Color,
    icon: ImageVector,
    title: String,
    detail: String,
    cta: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.04f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = accent.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = cta,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun weatherAccent(code: Int?): Color {
    return when (code) {
        0 -> Color(0xFFFFC857)
        in 1..3 -> Color(0xFF8FD3FF)
        in 51..67, in 80..82 -> Color(0xFF64B5F6)
        else -> AccentBlue
    }
}

private fun isEventToday(event: Event): Boolean {
    return try {
        if (event.startDateTime.isNotEmpty()) {
            val eventCal = parseIsoDate(event.startDateTime)
            eventCal != null && isSameDay(eventCal, Calendar.getInstance())
        } else {
            event.date.isNotEmpty()
        }
    } catch (_: Exception) {
        false
    }
}

private fun eventTiming(event: Event): String {
    if (event.startTime == "Toute la journee") return "Toute la journee"
    return try {
        val startCal = parseIsoDate(event.startDateTime)
        if (startCal != null) {
            val hour = "%02d".format(startCal.get(Calendar.HOUR_OF_DAY))
            val minute = "%02d".format(startCal.get(Calendar.MINUTE))
            "$hour:$minute"
        } else {
            event.startTime.ifBlank { "Horaire a confirmer" }
        }
    } catch (_: Exception) {
        event.startTime.ifBlank { "Horaire a confirmer" }
    }
}

private fun parseIsoDate(isoDate: String): Calendar? {
    return try {
        val cal = Calendar.getInstance()
        val parts = isoDate.split("T")
        val datePart = parts[0]
        val dateParts = datePart.split("-")
        if (dateParts.size == 3) {
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()
            if (parts.size > 1) {
                val timePart = parts[1].split("+", "-", "Z")[0]
                val timeParts = timePart.split(":")
                if (timeParts.size >= 2) {
                    cal.set(year, month, day, timeParts[0].toInt(), timeParts[1].toInt(), 0)
                } else {
                    cal.set(year, month, day, 0, 0, 0)
                }
            } else {
                cal.set(year, month, day, 0, 0, 0)
            }
            cal.set(Calendar.MILLISECOND, 0)
            cal
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

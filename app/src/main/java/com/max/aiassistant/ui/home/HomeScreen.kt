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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
                onClick = onNavigateToWeather
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
    onClick: () -> Unit
) {
    val weatherLabel = weatherData?.let { weatherSummary(it.weatherCode) } ?: "Meteo indisponible"
    val temperature = weatherData?.let { "${it.currentTemperature.toInt()}\u00B0" } ?: "--"
    val rainValue = weatherData?.hourlyForecasts?.firstOrNull()?.precipitationProb?.let { "$it%" } ?: "--"
    val humidityValue = weatherData?.currentHumidity?.let { "$it%" } ?: "--"
    val windValue = weatherData?.currentWindSpeed?.toInt()?.let { "$it km/h" } ?: "--"
    val accent = weatherAccent(weatherData?.weatherCode)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = weatherLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = accent.copy(alpha = 0.18f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = weatherIcon(weatherData?.weatherCode),
                            contentDescription = weatherLabel,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = temperature,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Previsions",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeatherStatPill(Icons.Default.WaterDrop, "Humidite", humidityValue, Modifier.weight(1f))
                WeatherStatPill(Icons.Default.WindPower, "Vent", windValue, Modifier.weight(1f))
                WeatherStatPill(Icons.Default.CloudQueue, "Pluie", rainValue, Modifier.weight(1f))
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
private fun WeatherStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
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

private fun weatherIcon(code: Int?): ImageVector {
    return when (code) {
        in 0..2 -> Icons.Default.WbSunny
        else -> Icons.Default.CloudQueue
    }
}

private fun weatherSummary(code: Int): String {
    return when (code) {
        0 -> "Ciel degage"
        1, 2 -> "Eclaircies"
        3 -> "Nuageux"
        in 45..48 -> "Brume"
        in 51..67 -> "Pluie"
        in 71..77 -> "Neige"
        in 80..82 -> "Averses"
        in 95..99 -> "Orage"
        else -> "Conditions du moment"
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

package com.max.aiassistant.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.max.aiassistant.data.api.ActuArticle
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
    headlineArticle: ActuArticle?,
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
                headlineArticle = headlineArticle,
                cityName = cityName,
                onNavigateToVoice = onNavigateToVoice,
                onNavigateToChat = onNavigateToChat,
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToPlanning = onNavigateToPlanning,
                onNavigateToWeather = onNavigateToWeather,
                onNavigateToRadar = onNavigateToRadar,
                onNavigateToActu = onNavigateToActu,
                modifier = modifier
            )
        }
    } else {
        HomeScreenContent(
            tasks = tasks,
            events = events,
            weatherData = weatherData,
            headlineArticle = headlineArticle,
            cityName = cityName,
            onNavigateToVoice = onNavigateToVoice,
            onNavigateToChat = onNavigateToChat,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToPlanning = onNavigateToPlanning,
            onNavigateToWeather = onNavigateToWeather,
            onNavigateToRadar = onNavigateToRadar,
            onNavigateToActu = onNavigateToActu,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeScreenContent(
    tasks: List<Task>,
    events: List<Event>,
    weatherData: WeatherData?,
    headlineArticle: ActuArticle?,
    cityName: String,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToActu: () -> Unit,
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
            HomePromptBar(
                onChatClick = onNavigateToChat,
                onVoiceClick = onNavigateToVoice
            )
        }
        item {
            HomeWeatherPanel(
                weatherData = weatherData,
                cityName = cityName,
                onClick = onNavigateToWeather,
                onRadarClick = onNavigateToRadar
            )
        }
        item {
            HomeHeadlinePanel(
                headlineArticle = headlineArticle,
                onClick = onNavigateToActu
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
private fun HomePromptBar(
    onChatClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    val borderColor = AccentBlue.copy(alpha = 0.55f)
    val glowColor = AccentBlue.copy(alpha = 0.18f)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF12243B),
                            Color(0xFF17304F),
                            Color(0xFF102743)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onChatClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Pose moi une question...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onVoiceClick,
                shape = CircleShape,
                color = glowColor.copy(alpha = 0.16f)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Ouvrir le vocal",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeadlinePanel(
    headlineArticle: ActuArticle?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF131D2C), Color(0xFF182437), Color(0xFF122031))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = AccentBlue.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = "Actualites",
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "A la une",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = headlineArticle?.titre ?: "Ouvrir les actualites du jour",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
private fun CompactOverviewPanel(
    tasks: List<Task>,
    events: List<Event>,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit
) {
    val openTaskCount = remember(tasks) { tasks.count { it.status != TaskStatus.COMPLETED } }
    val todayEventCount = remember(events) { events.count { isEventToday(it) } }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF111925), Color(0xFF192333), Color(0xFF141E2C))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactMetricCard(
                accent = if (openTaskCount == 0) CompletedGreen else HighOrange,
                icon = Icons.Default.CheckCircle,
                value = formatCompactCount(openTaskCount),
                label = "Taches",
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            CompactMetricCard(
                accent = AccentBlue,
                icon = Icons.Default.CalendarMonth,
                value = formatCompactCount(todayEventCount),
                label = "Aujourd'hui",
                onClick = onNavigateToPlanning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactMetricCard(
    accent: Color,
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCompactCount(count: Int): String = if (count > 99) "99+" else count.toString()

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

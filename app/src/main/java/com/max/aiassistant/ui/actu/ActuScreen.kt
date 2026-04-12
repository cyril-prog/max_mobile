package com.max.aiassistant.ui.actu

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.data.api.ActuArticle
import com.max.aiassistant.data.api.RechercheArticle
import com.max.aiassistant.ui.common.*
import com.max.aiassistant.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ActuBackdrop = Brush.verticalGradient(
    listOf(
        Color(0xFF0B1420),
        Color(0xFF101927),
        Color(0xFF0D1622),
        Color(0xFF09111A)
    )
)
private val ActuCardSurface = Color(0xFF172335)
private val ActuCardSurfaceAlt = Color(0xFF1A2840)

@Composable
fun ActuScreen(actuArticles: List<ActuArticle>, rechercheArticles: List<RechercheArticle>, isRefreshing: Boolean, errorMessage: String? = null, isOffline: Boolean = false, onRefresh: () -> Unit, onNavigateToHome: () -> Unit, onNavigateToVoice: () -> Unit, onNavigateToChat: () -> Unit, onNavigateToTasks: () -> Unit, onNavigateToPlanning: () -> Unit, onNavigateToWeather: () -> Unit, onNavigateToNotes: () -> Unit, showChrome: Boolean = true, modifier: Modifier = Modifier) {
    val body: @Composable () -> Unit = { ActuBody(actuArticles, rechercheArticles, isRefreshing, errorMessage, isOffline, onRefresh, showChrome, modifier) }
    if (!showChrome) body() else {
        val sidebarState = rememberNavigationSidebarState()
        NavigationSidebarScaffold(currentScreen = NavigationScreen.ACTU, onNavigateToScreen = {
            when (it) {
                NavigationScreen.HOME -> onNavigateToHome(); NavigationScreen.VOICE -> onNavigateToVoice(); NavigationScreen.CHAT -> onNavigateToChat(); NavigationScreen.TASKS -> onNavigateToTasks(); NavigationScreen.PLANNING -> onNavigateToPlanning(); NavigationScreen.WEATHER -> onNavigateToWeather(); NavigationScreen.NOTES -> onNavigateToNotes(); NavigationScreen.ACTU -> Unit
            }
        }, sidebarState = sidebarState) { body() }
    }
}

@Composable
private fun ActuBody(actuArticles: List<ActuArticle>, rechercheArticles: List<RechercheArticle>, isRefreshing: Boolean, errorMessage: String?, isOffline: Boolean, onRefresh: () -> Unit, showChrome: Boolean, modifier: Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(actuArticles.size, rechercheArticles.size) {
        when {
            tab == 0 && actuArticles.isEmpty() && rechercheArticles.isNotEmpty() -> tab = 1
            tab == 1 && rechercheArticles.isEmpty() && actuArticles.isNotEmpty() -> tab = 0
        }
    }
    val activeItems = if (tab == 0) actuArticles else rechercheArticles
    Column(modifier.fillMaxSize().background(ActuBackdrop)) {
        if (showChrome) Row(Modifier.fillMaxWidth().background(ActuBackdrop).statusBarsPadding().padding(20.dp, 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("Newsroom", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold); Text("Une lecture plus editoriale et plus rapide", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            FilledTonalButton(onClick = onRefresh, enabled = !isRefreshing, colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentBlue.copy(alpha = 0.16f), contentColor = AccentBlue)) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Refresh") }
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Le journal") }, colors = chipColors()); FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Recherche IA") }, colors = chipColors()) }
        when {
            isRefreshing && activeItems.isEmpty() -> LoadingStateView(title = "Chargement actualites", subtitle = "Mise en place de la selection editoriale.", modifier = Modifier.fillMaxSize().padding(24.dp))
            activeItems.isEmpty() && errorMessage != null -> ErrorStateView(title = "Actualites indisponibles", subtitle = errorMessage, onRetry = onRefresh, modifier = Modifier.fillMaxSize().padding(24.dp))
            activeItems.isEmpty() && isOffline -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { InlineStatusBanner(title = "Mode hors ligne", subtitle = "Les flux ont besoin d'une connexion pour etre actualises.", tone = BannerTone.Offline) }
            activeItems.isEmpty() -> EmptyActuState(onRefresh)
            else -> if (tab == 0) JournalFeed(actuArticles, errorMessage, isOffline) else ResearchFeed(rechercheArticles, errorMessage, isOffline)
        }
    }
}

@Composable
private fun JournalFeed(actuArticles: List<ActuArticle>, errorMessage: String?, isOffline: Boolean) {
    val lead = actuArticles.first()
    val others = actuArticles.drop(1)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (errorMessage != null) item { InlineStatusBanner(title = "Flux partiel", subtitle = errorMessage, tone = BannerTone.Warning) } else if (isOffline) item { InlineStatusBanner(title = "Lecture hors ligne", subtitle = "Affichage des derniers contenus charges.", tone = BannerTone.Offline) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { InsightCard("Sources", actuArticles.map { it.origine }.distinct().size.toString(), Modifier.weight(1f)); InsightCard("Articles", actuArticles.size.toString(), Modifier.weight(1f)) } }
        item { SectionHeader("A la une", "L'article principal, dans un format plus compact et lisible.") }
        item { LeadArticleCard(lead) }
        if (others.isNotEmpty()) {
            item { SectionHeader("Selection rapide", "Les titres secondaires les plus utiles pour un scan express.") }
        }
        items(others, key = { it.id }) { NewsCard(it) }
    }
}

@Composable
private fun ResearchFeed(rechercheArticles: List<RechercheArticle>, errorMessage: String?, isOffline: Boolean) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF11192B)) { Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Research digest", style = MaterialTheme.typography.labelMedium, color = AccentBlue); Text("Syntheses IA", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold); Text("Un format note de lecture, plus proche d'un memo produit que d'un flux RSS.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) } } }
        if (errorMessage != null) item { InlineStatusBanner(title = "Flux partiel", subtitle = errorMessage, tone = BannerTone.Warning) } else if (isOffline) item { InlineStatusBanner(title = "Lecture hors ligne", subtitle = "Affichage des derniers contenus charges.", tone = BannerTone.Offline) }
        items(rechercheArticles, key = { it.id }) { ResearchCard(it) }
    }
}

@Composable
private fun LeadArticleCard(article: ActuArticle) {
    val context = LocalContext.current
    var expanded by rememberSaveable(article.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF14213D), Color(0xFF23395D)))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SourcePill(article.origine); Text(formatRelativeDate(article.dateActualite), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f)) }
            Text("A la une", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.SemiBold)
            Text(article.titre, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
            if (article.description.isNotBlank()) Text(article.description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f), lineHeight = 20.sp, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScoreBadge(article.score)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpandButton(expanded = expanded, onToggle = { expanded = !expanded })
                    article.url.takeIf { it.isNotBlank() }?.let { OpenArticleButton(it, context) }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(article: ActuArticle) {
    val context = LocalContext.current
    var expanded by rememberSaveable(article.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ActuCardSurface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SourcePill(article.origine); Text(formatRelativeDate(article.dateActualite), style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
            Text(article.titre, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            if (article.description.isNotBlank()) Text(article.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScoreBadge(article.score)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpandButton(expanded = expanded, onToggle = { expanded = !expanded })
                    article.url.takeIf { it.isNotBlank() }?.let { OpenArticleButton(it, context) }
                }
            }
        }
    }
}

@Composable
private fun ResearchCard(article: RechercheArticle) {
    val context = LocalContext.current
    var expanded by rememberSaveable(article.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ActuCardSurfaceAlt)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(formatRelativeDate(article.publication), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            if (article.titre.isNotBlank()) Text(article.titre, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            if (article.resume.isNotBlank()) Text(article.resume, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpandButton(expanded = expanded, onToggle = { expanded = !expanded })
                    article.url.takeIf { it.isNotBlank() }?.let { OpenArticleButton(it, context) }
                }
            }
        }
    }
}

@Composable private fun SectionHeader(title: String, text: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold); Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary) } }
@Composable private fun InsightCard(label: String, value: String, modifier: Modifier = Modifier) { Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = ActuCardSurface)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary); Text(value, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold) } } }
@Composable private fun SourcePill(source: String) { val color = when { source.contains("Bloomberg", true) -> Color(0xFF1DA1F2); source.contains("Verge", true) -> Color(0xFFFF6A3D); source.contains("TechCrunch", true) -> Color(0xFF2EC27E); else -> AccentBlue }; Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.16f)) { Text(source.split(" / ").firstOrNull()?.trim().orEmpty(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold) } }
@Composable private fun ScoreBadge(score: Int) { if (score <= 0) return; val fg = when { score >= 80 -> Color(0xFF34C759); score >= 60 -> AccentBlue; score >= 40 -> Color(0xFFFF9F0A); else -> TextSecondary }; Surface(shape = RoundedCornerShape(999.dp), color = fg.copy(alpha = 0.16f)) { Text("Score $score", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold) } }
@Composable private fun OpenArticleButton(url: String, context: android.content.Context) { Surface(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, shape = RoundedCornerShape(999.dp), color = AccentBlue.copy(alpha = 0.16f)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Text("Ouvrir", color = AccentBlue, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium); Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = AccentBlue, modifier = Modifier.size(12.dp)) } } }
@Composable private fun ExpandButton(expanded: Boolean, onToggle: () -> Unit) { Surface(onClick = onToggle, shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.06f)) { Text(if (expanded) "Masquer" else "Afficher", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = TextSecondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium) } }
@Composable private fun EmptyActuState(onRefresh: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { Box(Modifier.size(72.dp).clip(CircleShape).background(DarkSurface), contentAlignment = Alignment.Center) { Icon(Icons.Default.Newspaper, null, tint = TextSecondary, modifier = Modifier.size(34.dp)) }; Text("Aucun contenu disponible", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold); Text("Le board reviendra des qu'une nouvelle selection sera disponible.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary); OutlinedButton(onClick = onRefresh) { Text("Reessayer") } } } }
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentBlue.copy(alpha = 0.18f), selectedLabelColor = AccentBlue, containerColor = DarkSurface, labelColor = TextSecondary)
private fun formatRelativeDate(isoDate: String): String { if (isoDate.isBlank()) return ""; return try { val date = ZonedDateTime.parse(isoDate); val now = ZonedDateTime.now(); val minutes = ChronoUnit.MINUTES.between(date, now); val hours = ChronoUnit.HOURS.between(date, now); val days = ChronoUnit.DAYS.between(date, now); when { minutes < 5 -> "A l'instant"; minutes < 60 -> "il y a ${minutes} min"; hours < 24 -> "il y a ${hours} h"; days == 1L -> "hier"; days < 7 -> "il y a ${days} j"; else -> date.format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRENCH)) } } catch (_: Exception) { isoDate.take(10) } }

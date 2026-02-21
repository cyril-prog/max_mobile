package com.max.aiassistant.ui.actu

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.max.aiassistant.data.api.ActuArticle
import com.max.aiassistant.data.api.RechercheArticle
import com.max.aiassistant.ui.common.NavigationScreen
import com.max.aiassistant.ui.common.NavigationSidebarScaffold
import com.max.aiassistant.ui.common.rememberNavigationSidebarState
import com.max.aiassistant.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * ÉCRAN ACTUALITÉS — Deux onglets : "Actualité du jour" et "Recherche IA"
 *
 * Affiche les articles récupérés depuis POST /webhook/get_actu
 * avec gestion des cas vides et bouton de rafraîchissement.
 */
@Composable
fun ActuScreen(
    actuArticles: List<ActuArticle>,
    rechercheArticles: List<RechercheArticle>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPlanning: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarState = rememberNavigationSidebarState()

    NavigationSidebarScaffold(
        currentScreen = NavigationScreen.ACTU,
        onNavigateToScreen = { screen ->
            when (screen) {
                NavigationScreen.HOME -> onNavigateToHome()
                NavigationScreen.VOICE -> onNavigateToVoice()
                NavigationScreen.CHAT -> onNavigateToChat()
                NavigationScreen.TASKS -> onNavigateToTasks()
                NavigationScreen.PLANNING -> onNavigateToPlanning()
                NavigationScreen.WEATHER -> onNavigateToWeather()
                NavigationScreen.NOTES -> onNavigateToNotes()
                NavigationScreen.ACTU -> { /* Déjà ici */ }
            }
        },
        sidebarState = sidebarState
    ) {
        ActuScreenContent(
            actuArticles = actuArticles,
            rechercheArticles = rechercheArticles,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActuScreenContent(
    actuArticles: List<ActuArticle>,
    rechercheArticles: List<RechercheArticle>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // ── TopBar ──────────────────────────────────────────────────────
        ActuTopBar(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        )

        // ── TabRow ──────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = AccentBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentBlue,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Actualité du jour",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == 0) AccentBlue else TextSecondary
                        )
                        if (actuArticles.isNotEmpty()) {
                            Badge(
                                containerColor = if (selectedTab == 0) AccentBlue else TextTertiary
                            ) {
                                Text(
                                    text = "${actuArticles.size}",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Recherche IA",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == 1) AccentBlue else TextSecondary
                        )
                        if (rechercheArticles.isNotEmpty()) {
                            Badge(
                                containerColor = if (selectedTab == 1) AccentBlue else TextTertiary
                            ) {
                                Text(
                                    text = "${rechercheArticles.size}",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            )
        }

        // ── Contenu de l'onglet sélectionné ─────────────────────────────
        val isEmpty = if (selectedTab == 0) actuArticles.isEmpty() else rechercheArticles.isEmpty()
        val emptyMessage = if (selectedTab == 0)
            "Aucune actualité disponible aujourd'hui"
        else
            "Aucune recherche IA disponible aujourd'hui"

        if (isRefreshing && isEmpty) {
            // Indicateur de chargement initial
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (isEmpty) {
            EmptyActuState(message = emptyMessage, onRefresh = onRefresh)
        } else if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.md.dp,
                    end = Spacing.md.dp,
                    top = Spacing.sm.dp,
                    bottom = Spacing.xl.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
            ) {
                items(actuArticles, key = { it.id }) { article ->
                    ActuArticleCard(article = article)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.md.dp,
                    end = Spacing.md.dp,
                    top = Spacing.sm.dp,
                    bottom = Spacing.xl.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
            ) {
                items(rechercheArticles, key = { it.id }) { article ->
                    RechercheArticleCard(article = article)
                }
            }
        }
    }
}

/**
 * Barre de titre avec bouton de rafraîchissement
 */
@Composable
private fun ActuTopBar(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    // Animation de rotation pour l'icône de refresh
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spinAngle"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = Spacing.md.dp, vertical = Spacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            // Icône gradient
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientActu)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Newspaper,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Actualités",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Bouton refresh
        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Rafraîchir",
                tint = if (isRefreshing) AccentBlue else TextSecondary,
                modifier = if (isRefreshing)
                    Modifier.size(24.dp).graphicsLayerRotation(rotation)
                else
                    Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Carte d'affichage d'un article d'actualité
 * Affiche : titre, origine (chip), description, date relative, score, lien URL
 */
@Composable
private fun ActuArticleCard(article: ActuArticle) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = article.url.isNotEmpty()) {
                if (article.url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            // ── Ligne 1 : origine + date ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OrigineChip(origine = article.origine)
                Text(
                    text = formatRelativeDate(article.dateActualite),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            // ── Titre ───────────────────────────────────────────────────
            Text(
                text = article.titre,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )

            // ── Description (si non vide) ───────────────────────────────
            if (article.description.isNotEmpty()) {
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                Text(
                    text = if (expanded) "Voir moins" else "Voir plus",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }

            // ── Ligne bas : score + lien ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreBadge(score = article.score)

                if (article.url.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Lire l'article",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carte dédiée à l'onglet "Recherche IA".
 * Affiche le titre + résumé complet en français (dépliable) + lien source en bas.
 */
@Composable
private fun RechercheArticleCard(article: RechercheArticle) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm.dp)
        ) {
            // ── Date de publication ─────────────────────────────────────
            if (article.publication.isNotEmpty()) {
                Text(
                    text = formatRelativeDate(article.publication),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            // ── Titre ───────────────────────────────────────────────────
            if (article.titre.isNotEmpty()) {
                Text(
                    text = article.titre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
            }

            // ── Résumé complet, dépliable ───────────────────────────────
            if (article.resume.isNotEmpty()) {
                Text(
                    text = article.resume,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = if (expanded) Int.MAX_VALUE else 5,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    lineHeight = 19.sp,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                Text(
                    text = if (expanded) "Voir moins" else "Voir plus",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }

            // ── Lien vers l'article source ──────────────────────────────
            if (article.url.isNotEmpty()) {
                HorizontalDivider(
                    color = TextTertiary.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Voir l'article source",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 */
@Composable
private fun OrigineChip(origine: String) {
    // Couleur déterministe basée sur la première lettre de l'origine
    val chipColor = when {
        origine.contains("Bloomberg", ignoreCase = true) -> Color(0xFF1DA1F2)
        origine.contains("Verge", ignoreCase = true) -> Color(0xFFFF3B30)
        origine.contains("TechCrunch", ignoreCase = true) -> Color(0xFF34C759)
        origine.contains("Google", ignoreCase = true) -> Color(0xFFFF9500)
        origine.contains("Marktechpost", ignoreCase = true) -> Color(0xFF7C3AED)
        origine.contains("Science", ignoreCase = true) -> Color(0xFF0D9488)
        else -> AccentBlueMuted
    }

    // N'afficher que le premier nom si la chaîne contient " / "
    val shortName = origine.split(" / ").firstOrNull()?.trim() ?: origine

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipColor.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = shortName,
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Badge de score (0-100) avec couleur selon la valeur
 */
@Composable
private fun ScoreBadge(score: Int) {
    if (score <= 0) return

    val (bgColor, label) = when {
        score >= 80 -> Pair(Color(0xFF34C759).copy(alpha = 0.18f) to Color(0xFF34C759), "Top")
        score >= 60 -> Pair(AccentBlue.copy(alpha = 0.15f) to AccentBlue, "Bon")
        score >= 40 -> Pair(Color(0xFFFF9500).copy(alpha = 0.15f) to Color(0xFFFF9500), "Moyen")
        else -> Pair(TextTertiary.copy(alpha = 0.15f) to TextTertiary, "Faible")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor.first)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Score : $score",
                style = MaterialTheme.typography.labelSmall,
                color = bgColor.second,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * État vide avec message et bouton de relance
 */
@Composable
private fun EmptyActuState(
    message: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Newspaper,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(Spacing.sm.dp))
        Text(
            text = "Les actualités sont récupérées chaque jour",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(Spacing.lg.dp))
        OutlinedButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Réessayer")
        }
    }
}

// ─── Extension pour la rotation graphique ─────────────────────────────────────

private fun Modifier.graphicsLayerRotation(degrees: Float): Modifier =
    this.graphicsLayer { rotationZ = degrees }

// ─── Utilitaire de formatage de date ──────────────────────────────────────────

/**
 * Formate une date ISO 8601 en date relative ("il y a 2h", "hier", "il y a 3j")
 */
private fun formatRelativeDate(isoDate: String): String {
    if (isoDate.isEmpty()) return ""
    return try {
        val date = ZonedDateTime.parse(isoDate)
        val now = ZonedDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(date, now)
        val hours = ChronoUnit.HOURS.between(date, now)
        val days = ChronoUnit.DAYS.between(date, now)
        when {
            minutes < 5 -> "À l'instant"
            minutes < 60 -> "il y a ${minutes}min"
            hours < 24 -> "il y a ${hours}h"
            days == 1L -> "hier"
            days < 7 -> "il y a ${days}j"
            else -> {
                val fmt = DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRENCH)
                date.format(fmt)
            }
        }
    } catch (e: Exception) {
        isoDate.take(10) // Fallback : affiche la date brute
    }
}

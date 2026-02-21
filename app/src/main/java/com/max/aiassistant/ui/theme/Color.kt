package com.max.aiassistant.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs pour le mode sombre de Max
 *
 * Design minimaliste avec :
 * - Fond très sombre (quasi noir)
 * - Texte clair pour contraste élevé
 * - Accent bleu pour les éléments interactifs
 * - Couleurs de statut pour les tâches
 * - Système de spacing cohérent
 */

// ─── Fond et surfaces ───────────────────────────────────────────────
val DarkBackground = Color(0xFF000000)           // Fond principal noir pur
val DarkSurface = Color(0xFF1C1C1E)              // Surface légèrement plus claire
val DarkSurfaceVariant = Color(0xFF2C2C2E)       // Variante pour différencier les sections
val DarkSurfaceElevated = Color(0xFF3A3A3C)      // Surface élevée (cartes prominentes)

// ─── Texte ──────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFFFFFFF)              // Texte principal blanc
val TextSecondary = Color(0xFFAAAAAA)            // Texte secondaire gris clair
val TextTertiary = Color(0xFF6E6E73)             // Texte tertiaire (hints, placeholders)
val TextOnAccent = Color(0xFFFFFFFF)             // Texte sur fond coloré

// ─── Accent et éléments interactifs ─────────────────────────────────
val AccentBlue = Color(0xFF0A84FF)               // Bleu principal pour les accents
val AccentBlueDark = Color(0xFF0066CC)           // Version plus foncée
val AccentBlueMuted = Color(0xFF1E3A5F)          // Bleu atténué (bulles IA, fonds)
val AccentBlueLight = Color(0xFF60A5FA)          // Bleu clair (highlights)

// ─── Messages chat ──────────────────────────────────────────────────
val UserMessageBg = Color(0xFF0A84FF)            // Message utilisateur : bleu accent (droite)
val MaxMessageBg = Color(0xFF2C2C2E)             // Message Max : surface variante (gauche)

// ─── Statuts de tâches ──────────────────────────────────────────────
val UrgentRed = Color(0xFFFF3B30)                // Rouge vif pour urgent (P1)
val HighOrange = Color(0xFFFF6B35)               // Orange pour haute priorité (P2)
val NormalOrange = Color(0xFFFF9500)             // Orange pour normal (P3)
val LowYellow = Color(0xFFFFCC00)                // Jaune pour basse priorité (P4)
val CompletedGreen = Color(0xFF34C759)           // Vert pour complété
val InfoBlue = Color(0xFF0A84FF)                 // Bleu pour information (P5)

// ─── Bouton vocal ────────────────────────────────────────────────────
val VoiceButtonRed = Color(0xFFFF3B30)           // Rouge vif pour le bouton micro

// ─── État de connexion ───────────────────────────────────────────────
val ConnectionActiveColor = Color(0xFF34C759)    // Vert : connecté/actif
val ConnectionIdleColor = Color(0xFF6E6E73)      // Gris : inactif/déconnecté

// ─── Bordures et séparateurs ─────────────────────────────────────────
val BorderColor = Color(0xFF3A3A3C)              // Bordure subtile
val BorderColorLight = Color(0xFF48484A)         // Bordure plus visible

// ─── Gradients prédéfinis ────────────────────────────────────────────
// Voice : violet chaud → rose magenta (vivant, expressif)
val GradientVoice = listOf(Color(0xFF7C3AED), Color(0xFFDB2777))
// Chat : bleu électrique → cyan (technologie, fluidité)
val GradientChat = listOf(Color(0xFF0A84FF), Color(0xFF00C6FB))
// Tasks : orange brûlé → rouge corail (énergie, action)
val GradientTasks = listOf(Color(0xFFFF6B35), Color(0xFFEA4C1D))
// Planning : teal profond → émeraude (organisation, clarté)
val GradientPlanning = listOf(Color(0xFF0D9488), Color(0xFF059669))
// Weather : bleu nuit → cyan ciel (atmosphère, horizon)
val GradientWeather = listOf(Color(0xFF1E40AF), Color(0xFF0891B2))
// Notes : indigo → violet moyen (créativité, réflexion)
val GradientNotes = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
// Actualités : ambre doré → rouge corail (presse, urgence, info)
val GradientActu = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))

// ─── Système de Spacing ──────────────────────────────────────────────
// Utiliser ces constantes plutôt que des valeurs hardcodées
// Exemple d'usage : Modifier.padding(Spacing.md.dp)
object Spacing {
    const val xs = 4    // Extra small  : 4dp
    const val sm = 8    // Small        : 8dp
    const val md = 16   // Medium       : 16dp
    const val lg = 24   // Large        : 24dp
    const val xl = 32   // Extra large  : 32dp
    const val xxl = 48  // Extra extra  : 48dp
}

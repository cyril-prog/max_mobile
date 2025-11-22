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
 */

// Couleurs principales
val DarkBackground = Color(0xFF000000)           // Fond principal noir pur
val DarkSurface = Color(0xFF1C1C1E)              // Surface légèrement plus claire
val DarkSurfaceVariant = Color(0xFF2C2C2E)       // Variante pour différencier les sections

// Texte
val TextPrimary = Color(0xFFFFFFFF)              // Texte principal blanc
val TextSecondary = Color(0xFFAAAAAA)            // Texte secondaire gris clair

// Accent et éléments interactifs
val AccentBlue = Color(0xFF0A84FF)               // Bleu iOS-like pour les accents
val AccentBlueDark = Color(0xFF0066CC)           // Version plus foncée

// Couleurs pour les messages (contrastées et élégantes sur fond noir)
val UserMessageBg = Color(0xFF3D3D3D)            // Fond message utilisateur (gris moyen clair)
val MaxMessageBg = Color(0xFF1E3A5F)             // Fond message Max (bleu profond élégant)

// Couleurs de statut des tâches
val UrgentRed = Color(0xFFBDBDBD)                // Rouge vif pour urgent
val NormalOrange = Color(0xFFFF9500)             // Orange pour normal
val CompletedGreen = Color(0xFF34C759)           // Vert pour complété

// Couleur pour le bouton voice
val VoiceButtonRed = Color(0xFFFF3B30)           // Rouge vif pour le bouton micro

// Bordures et séparateurs
val BorderColor = Color(0xFF3A3A3C)              // Bordure subtile

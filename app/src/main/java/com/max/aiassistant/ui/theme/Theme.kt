package com.max.aiassistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Schéma de couleurs pour le mode sombre de Max
 *
 * Applique la palette définie dans Color.kt au thème Material 3
 */
private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    secondary = AccentBlueDark,
    onSecondary = TextPrimary,
    tertiary = VoiceButtonRed,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = UrgentRed,
    onError = TextPrimary,
    outline = BorderColor
)

/**
 * Thème principal de l'application Max
 *
 * Force le mode sombre par défaut pour un look moderne et professionnel
 *
 * @param darkTheme Force le mode sombre (true par défaut)
 * @param content Le contenu de l'application à thématiser
 */
@Composable
fun MaxTheme(
    darkTheme: Boolean = true, // Toujours en mode sombre
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Configure la barre de statut pour le mode sombre
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaxTypography,
        content = content
    )
}

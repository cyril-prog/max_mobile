package com.max.aiassistant.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current
    val componentActivity = context.findComponentActivity()
    val transparentScrim = Color.Transparent.toArgb()

    DisposableEffect(componentActivity, darkTheme) {
        componentActivity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = transparentScrim,
                darkScrim = transparentScrim
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = transparentScrim,
                darkScrim = transparentScrim
            )
        )
        onDispose { }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaxTypography,
        content = content
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

package com.max.aiassistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.max.aiassistant.R

/**
 * Police Inter (variable font) — moderne, très lisible sur mobile.
 * Fichiers TTF inclus statiquement dans res/font/ (pas de dépendance réseau).
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.Thin),
    Font(R.font.inter_variable, weight = FontWeight.ExtraLight),
    Font(R.font.inter_variable, weight = FontWeight.Light),
    Font(R.font.inter_variable, weight = FontWeight.Normal),
    Font(R.font.inter_variable, weight = FontWeight.Medium),
    Font(R.font.inter_variable, weight = FontWeight.SemiBold),
    Font(R.font.inter_variable, weight = FontWeight.Bold),
    Font(R.font.inter_variable, weight = FontWeight.ExtraBold),
    Font(R.font.inter_variable, weight = FontWeight.Black),
    Font(R.font.inter_variable_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.inter_variable_italic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(R.font.inter_variable_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(R.font.inter_variable_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
)

/**
 * Typographie de l'application Max
 *
 * Utilise Inter (variable font) pour une identité visuelle moderne et cohérente.
 * Letter-spacing négatif pour un rendu plus dense et professionnel aux grandes tailles.
 */
val MaxTypography = Typography(
    // Titre principal (ex: "Max" dans la barre de titre)
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),

    // Titres de sections (ex: "TO DO", "SCHEDULE")
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),

    // Titre de tâches et événements
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),

    // Corps de texte principal (messages, descriptions)
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // Texte secondaire (deadlines, heures)
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    // Petits labels (badges de statut)
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),

    // Très petits textes (si nécessaire)
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp
    )
)

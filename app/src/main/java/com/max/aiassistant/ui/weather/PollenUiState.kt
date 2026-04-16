package com.max.aiassistant.ui.weather

import com.max.aiassistant.data.api.PollenPlantData
import com.max.aiassistant.data.api.WeatherData
import com.max.aiassistant.data.api.getPollenLevel

internal enum class PollenCategory {
    GRASSES,
    TREES,
    HERBACEOUS
}

internal data class PollenReading(
    val label: String,
    val category: PollenCategory,
    val severity: Int?,
    val levelText: String,
    val valueText: String,
    val inSeason: Boolean?,
    val defaultVisible: Boolean
)

internal data class PollenSectionUiState(
    val category: PollenCategory,
    val rows: List<PollenReading>,
    val summarySeverity: Int?,
    val summaryLabel: String
)

private data class LegacyPollenDescriptor(
    val label: String,
    val category: PollenCategory,
    val defaultVisible: Boolean,
    val valueSelector: (WeatherData) -> Double?
)

private val legacyDescriptors = listOf(
    LegacyPollenDescriptor(
        label = "Graminées",
        category = PollenCategory.GRASSES,
        defaultVisible = true,
        valueSelector = { it.grassPollen }
    ),
    LegacyPollenDescriptor(
        label = "Bouleau",
        category = PollenCategory.TREES,
        defaultVisible = true,
        valueSelector = { it.birchPollen }
    ),
    LegacyPollenDescriptor(
        label = "Aulne",
        category = PollenCategory.TREES,
        defaultVisible = true,
        valueSelector = { it.alderPollen }
    ),
    LegacyPollenDescriptor(
        label = "Olivier",
        category = PollenCategory.TREES,
        defaultVisible = true,
        valueSelector = { it.olivePollen }
    ),
    LegacyPollenDescriptor(
        label = "Armoise",
        category = PollenCategory.HERBACEOUS,
        defaultVisible = true,
        valueSelector = { it.mugwortPollen }
    ),
    LegacyPollenDescriptor(
        label = "Ambroisie",
        category = PollenCategory.HERBACEOUS,
        defaultVisible = true,
        valueSelector = { it.ragweedPollen }
    )
)

internal fun buildPollenSections(weatherData: WeatherData): List<PollenSectionUiState> {
    return if (weatherData.pollenPlants.isNotEmpty()) {
        buildDynamicSections(weatherData)
    } else {
        buildLegacySections(weatherData)
    }
}

private fun buildDynamicSections(weatherData: WeatherData): List<PollenSectionUiState> {
    val readings = weatherData.pollenPlants.mapNotNull { plant ->
        val category = plant.plantType.toPollenCategory() ?: return@mapNotNull null
        PollenReading(
            label = plant.toDisplayLabel(),
            category = category,
            severity = plant.indexValue,
            levelText = plant.category ?: pollenLevelFromIndex(plant.indexValue),
            valueText = plant.indexValue?.let { "$it/5" }
                ?: if (plant.inSeason == false) "Hors saison" else "Donnée indisponible",
            inSeason = plant.inSeason,
            defaultVisible = false
        )
    }

    val pollenTypeByCategory = weatherData.pollenTypes
        .mapNotNull { type ->
            val category = type.code.toPollenCategory() ?: return@mapNotNull null
            category to type
        }
        .toMap()

    return PollenCategory.entries.map { category ->
        val categoryReadings = readings.filter { it.category == category }
        val rowsWithSeverity = categoryReadings
            .filter { (it.severity ?: 0) > 0 }
            .sortedWith(compareByDescending<PollenReading> { it.severity ?: 0 }.thenBy { it.label })

        val rowsInSeason = categoryReadings
            .filter { it.inSeason == true }
            .sortedWith(compareByDescending<PollenReading> { it.severity ?: -1 }.thenBy { it.label })

        val orderedFallbackRows = categoryReadings
            .sortedWith(compareByDescending<PollenReading> { it.severity ?: -1 }.thenBy { it.label })
            .take(defaultFallbackCount(category))

        val typeSummary = pollenTypeByCategory[category]
        val summarySeverity = typeSummary?.indexValue ?: categoryReadings.mapNotNull { it.severity }.maxOrNull()

        PollenSectionUiState(
            category = category,
            rows = when {
                rowsWithSeverity.isNotEmpty() -> rowsWithSeverity
                rowsInSeason.isNotEmpty() -> rowsInSeason
                else -> orderedFallbackRows
            },
            summarySeverity = summarySeverity,
            summaryLabel = typeSummary?.category ?: pollenLevelFromIndex(summarySeverity)
        )
    }
}

private fun buildLegacySections(weatherData: WeatherData): List<PollenSectionUiState> {
    val readings = legacyDescriptors.map { descriptor ->
        val value = descriptor.valueSelector(weatherData)
        PollenReading(
            label = descriptor.label,
            category = descriptor.category,
            severity = severityFromGrains(value),
            levelText = value?.let(::getPollenLevel) ?: "Indisponible",
            valueText = value?.let { "${it.toInt()} grains/m3" } ?: "Donnée indisponible",
            inSeason = null,
            defaultVisible = descriptor.defaultVisible
        )
    }

    return PollenCategory.entries.map { category ->
        val categoryReadings = readings.filter { it.category == category }
        val rowsWithSeverity = categoryReadings
            .filter { (it.severity ?: 0) > 0 }
            .sortedWith(compareByDescending<PollenReading> { it.severity ?: 0 }.thenBy { it.label })
        val summaryValue = legacyDescriptors
            .filter { it.category == category }
            .mapNotNull { it.valueSelector(weatherData) }
            .maxOrNull()

        PollenSectionUiState(
            category = category,
            rows = if (rowsWithSeverity.isNotEmpty()) {
                rowsWithSeverity
            } else {
                categoryReadings.filter { it.defaultVisible }
            },
            summarySeverity = severityFromGrains(summaryValue),
            summaryLabel = summaryValue?.let(::getPollenLevel) ?: "Indisponible"
        )
    }
}

private fun PollenPlantData.toDisplayLabel(): String {
    return when (code) {
        "ALDER" -> "Aulne"
        "ASH" -> "Frêne"
        "BIRCH" -> "Bouleau"
        "COTTONWOOD" -> "Peuplier"
        "CYPRESS_PINE" -> "Cyprès"
        "ELM" -> "Orme"
        "GRAMINALES" -> "Graminées"
        "HAZEL" -> "Noisetier"
        "JAPANESE_CEDAR" -> "Cèdre du Japon"
        "JAPANESE_CYPRESS" -> "Cyprès du Japon"
        "JUNIPER" -> "Genévrier"
        "MAPLE" -> "Érable"
        "MUGWORT" -> "Armoise"
        "OAK" -> "Chêne"
        "OLIVE" -> "Olivier"
        "PINE" -> "Pin"
        "RAGWEED" -> "Ambroisie"
        else -> displayName
    }
}

private fun String.toPollenCategory(): PollenCategory? {
    return when (this) {
        "GRASS", "GRAMINALES" -> PollenCategory.GRASSES
        "TREE" -> PollenCategory.TREES
        "WEED" -> PollenCategory.HERBACEOUS
        "ALDER",
        "ASH",
        "BIRCH",
        "COTTONWOOD",
        "CYPRESS_PINE",
        "ELM",
        "HAZEL",
        "JAPANESE_CEDAR",
        "JAPANESE_CYPRESS",
        "JUNIPER",
        "MAPLE",
        "OAK",
        "OLIVE",
        "PINE" -> PollenCategory.TREES
        "MUGWORT", "RAGWEED" -> PollenCategory.HERBACEOUS
        else -> null
    }
}

private fun severityFromGrains(value: Double?): Int? {
    return when {
        value == null -> null
        value <= 0.0 -> 0
        value < 20 -> 1
        value < 50 -> 2
        value < 100 -> 3
        value < 200 -> 4
        else -> 5
    }
}

internal fun pollenLevelFromIndex(value: Int?): String {
    return when (value) {
        null -> "Indisponible"
        0 -> "Aucun"
        1 -> "Très faible"
        2 -> "Faible"
        3 -> "Modéré"
        4 -> "Fort"
        else -> "Très fort"
    }
}

private fun defaultFallbackCount(category: PollenCategory): Int {
    return when (category) {
        PollenCategory.GRASSES -> 3
        PollenCategory.TREES -> 3
        PollenCategory.HERBACEOUS -> 3
    }
}

package com.max.aiassistant.ui.weather

import com.max.aiassistant.data.api.PollenPlantData
import com.max.aiassistant.data.api.PollenTypeData
import com.max.aiassistant.data.api.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PollenUiStateTest {

    @Test
    fun `legacy fallback shows default trees when source has only fixed taxons`() {
        val sections = buildPollenSections(
            weatherData(
                grassPollen = 4.0,
                birchPollen = 22.0,
                alderPollen = 8.0,
                olivePollen = 0.0,
                mugwortPollen = 0.0,
                ragweedPollen = 12.0
            )
        )

        val trees = sections.first { it.category == PollenCategory.TREES }
        val herbaceous = sections.first { it.category == PollenCategory.HERBACEOUS }

        assertEquals(listOf("Bouleau", "Aulne"), trees.rows.map { it.label })
        assertEquals("Modéré", trees.summaryLabel)
        assertEquals(listOf("Ambroisie"), herbaceous.rows.map { it.label })
    }

    @Test
    fun `dynamic source shows every species with index above zero for selected city`() {
        val sections = buildPollenSections(
            weatherData(
                pollenTypes = listOf(
                    PollenTypeData("TREE", "Arbres", 4, "Fort", true),
                    PollenTypeData("GRASS", "Graminées", 2, "Faible", true),
                    PollenTypeData("WEED", "Herbacées", 3, "Modéré", true)
                ),
                pollenPlants = listOf(
                    PollenPlantData("OAK", "Oak", "TREE", 4, "Fort", true),
                    PollenPlantData("HAZEL", "Hazel", "TREE", 2, "Faible", true),
                    PollenPlantData("ASH", "Ash", "TREE", 1, "Très faible", true),
                    PollenPlantData("JUNIPER", "Juniper", "TREE", 1, "Très faible", true),
                    PollenPlantData("MAPLE", "Maple", "TREE", 1, "Très faible", true),
                    PollenPlantData("GRAMINALES", "Grass", "GRASS", 2, "Faible", true),
                    PollenPlantData("MUGWORT", "Mugwort", "WEED", 0, "Aucun", false),
                    PollenPlantData("RAGWEED", "Ragweed", "WEED", 3, "Modéré", true)
                )
            )
        )

        val trees = sections.first { it.category == PollenCategory.TREES }
        val herbaceous = sections.first { it.category == PollenCategory.HERBACEOUS }

        assertEquals(
            listOf("Chêne", "Noisetier", "Frêne", "Genévrier", "Érable"),
            trees.rows.map { it.label }
        )
        assertEquals(4, trees.summarySeverity)
        assertEquals("Fort", trees.summaryLabel)
        assertEquals(listOf("Ambroisie"), herbaceous.rows.map { it.label })
    }

    @Test
    fun `dynamic source shows all in season species when individual indexes are unavailable`() {
        val sections = buildPollenSections(
            weatherData(
                pollenPlants = listOf(
                    PollenPlantData("OAK", "Oak", "TREE", null, null, true),
                    PollenPlantData("HAZEL", "Hazel", "TREE", null, null, true),
                    PollenPlantData("ASH", "Ash", "TREE", null, null, true),
                    PollenPlantData("JUNIPER", "Juniper", "TREE", null, null, true),
                    PollenPlantData("MAPLE", "Maple", "TREE", null, null, true)
                )
            )
        )

        val trees = sections.first { it.category == PollenCategory.TREES }

        assertEquals(
            listOf("Chêne", "Frêne", "Genévrier", "Noisetier", "Érable"),
            trees.rows.map { it.label }
        )
    }

    @Test
    fun `dynamic source falls back to best known species when everything is zero`() {
        val sections = buildPollenSections(
            weatherData(
                pollenPlants = listOf(
                    PollenPlantData("OAK", "Oak", "TREE", 0, "Aucun", false),
                    PollenPlantData("HAZEL", "Hazel", "TREE", 0, "Aucun", false),
                    PollenPlantData("ASH", "Ash", "TREE", null, null, false)
                )
            )
        )

        val trees = sections.first { it.category == PollenCategory.TREES }

        assertEquals(listOf("Chêne", "Noisetier", "Frêne"), trees.rows.map { it.label })
        assertNull(sections.first { it.category == PollenCategory.GRASSES }.summarySeverity)
    }

    private fun weatherData(
        grassPollen: Double? = null,
        birchPollen: Double? = null,
        alderPollen: Double? = null,
        olivePollen: Double? = null,
        mugwortPollen: Double? = null,
        ragweedPollen: Double? = null,
        pollenTypes: List<PollenTypeData> = emptyList(),
        pollenPlants: List<PollenPlantData> = emptyList()
    ) = WeatherData(
        currentTemperature = 18.0,
        currentHumidity = 55,
        currentWindSpeed = 14.0,
        weatherCode = 1,
        hourlyForecasts = emptyList(),
        dailyForecasts = emptyList(),
        grassPollen = grassPollen,
        birchPollen = birchPollen,
        alderPollen = alderPollen,
        olivePollen = olivePollen,
        mugwortPollen = mugwortPollen,
        ragweedPollen = ragweedPollen,
        pollenTypes = pollenTypes,
        pollenPlants = pollenPlants
    )
}

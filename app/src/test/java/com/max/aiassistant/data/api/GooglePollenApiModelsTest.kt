package com.max.aiassistant.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GooglePollenApiModelsTest {

    @Test
    fun `toCurrentPollenData maps dynamic species and summaries`() {
        val pollen = GooglePollenForecastResponse(
            regionCode = "FR",
            dailyInfo = listOf(
                GooglePollenDayInfo(
                    pollenTypeInfo = listOf(
                        GooglePollenTypeInfo(
                            code = "TREE",
                            displayName = "Arbres",
                            inSeason = true,
                            indexInfo = GooglePollenIndexInfo(value = 4, category = "Fort")
                        )
                    ),
                    plantInfo = listOf(
                        GooglePollenPlantInfo(
                            code = "OAK",
                            displayName = "Oak",
                            inSeason = true,
                            indexInfo = GooglePollenIndexInfo(value = 4, category = "Fort"),
                            plantDescription = GooglePollenPlantDescription("TREE")
                        ),
                        GooglePollenPlantInfo(
                            code = "GRAMINALES",
                            displayName = "Grass",
                            inSeason = true,
                            indexInfo = GooglePollenIndexInfo(value = 2, category = "Faible"),
                            plantDescription = GooglePollenPlantDescription("GRASS")
                        )
                    )
                )
            )
        ).toCurrentPollenData()

        assertNotNull(pollen)
        assertEquals(2.0, pollen?.grassPollen ?: -1.0, 0.0)
        assertEquals(4.0, pollen?.pollenPlants?.first { it.code == "OAK" }?.indexValue?.toDouble() ?: -1.0, 0.0)
        assertEquals("google_pollen", pollen?.pollenSource)
        assertEquals("TREE", pollen?.pollenTypes?.first()?.code)
    }
}

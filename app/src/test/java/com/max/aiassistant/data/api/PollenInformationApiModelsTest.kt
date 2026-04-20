package com.max.aiassistant.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PollenInformationApiModelsTest {

    @Test
    fun `toCurrentPollenData maps official polleninformation forecast`() {
        val pollen = PollenInformationForecastResponse(
            contamination = listOf(
                PollenInformationContamination(
                    pollId = 5,
                    pollTitle = "Graminées (Poaceae)",
                    contaminationToday = 1,
                    contaminationTomorrow = 1,
                    contaminationDay3 = 1,
                    contaminationDay4 = 1
                ),
                PollenInformationContamination(
                    pollId = 2,
                    pollTitle = "Bouleau (Betula)",
                    contaminationToday = 0,
                    contaminationTomorrow = 1,
                    contaminationDay3 = 1,
                    contaminationDay4 = 1
                ),
                PollenInformationContamination(
                    pollId = 1,
                    pollTitle = "Aulne (Alnus)",
                    contaminationToday = 0,
                    contaminationTomorrow = 1,
                    contaminationDay3 = 0,
                    contaminationDay4 = 0
                )
            )
        ).toCurrentPollenData()

        assertNotNull(pollen)
        assertEquals("polleninformation", pollen?.pollenSource)
        assertEquals(2.0, pollen?.grassPollen ?: -1.0, 0.0)
        assertEquals(0, pollen?.pollenPlants?.first { it.code == "BIRCH" }?.indexValue)
        assertTrue(pollen?.pollenPlants?.first { it.code == "BIRCH" }?.inSeason == true)
        assertEquals("TREE", pollen?.pollenTypes?.first { it.code == "TREE" }?.code)
    }
}

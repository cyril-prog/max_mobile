package com.max.aiassistant.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class PollenApiModelsTest {

    @Test
    fun `toCurrentPollen uses exact hourly slot when present`() {
        val response = PollenApiResponse(
            timezone = "Europe/Paris",
            hourlyUnits = PollenHourlyUnits(time = "iso8601"),
            hourly = PollenHourlyData(
                time = listOf("2026-04-09T20:00", "2026-04-09T21:00", "2026-04-09T22:00"),
                alderPollen = listOf(0.1, 0.2, 0.3),
                birchPollen = listOf(10.0, 22.0, 30.0),
                grassPollen = listOf(1.0, 2.5, 3.0),
                mugwortPollen = listOf(0.0, 0.0, 0.1),
                olivePollen = listOf(0.0, 0.0, 0.0),
                ragweedPollen = listOf(0.0, 0.0, 0.0)
            )
        )

        val pollen = response.toCurrentPollen(LocalDateTime.parse("2026-04-09T21:15"))

        assertEquals(2.5, pollen.grassPollen!!, 0.0)
        assertEquals(22.0, pollen.birchPollen!!, 0.0)
        assertEquals(0.2, pollen.alderPollen!!, 0.0)
    }

    @Test
    fun `toCurrentPollen falls back to previous hourly slot when current hour missing`() {
        val response = PollenApiResponse(
            timezone = "Europe/Paris",
            hourlyUnits = PollenHourlyUnits(time = "iso8601"),
            hourly = PollenHourlyData(
                time = listOf("2026-04-09T18:00", "2026-04-09T20:00"),
                alderPollen = listOf(0.1, 0.2),
                birchPollen = listOf(8.0, 12.0),
                grassPollen = listOf(1.1, 1.9),
                mugwortPollen = listOf(null, null),
                olivePollen = listOf(null, null),
                ragweedPollen = listOf(null, null)
            )
        )

        val pollen = response.toCurrentPollen(LocalDateTime.parse("2026-04-09T21:05"))

        assertEquals(1.9, pollen.grassPollen!!, 0.0)
        assertEquals(12.0, pollen.birchPollen!!, 0.0)
        assertNull(pollen.mugwortPollen)
    }
}

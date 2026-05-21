package com.example.naikapa

import com.example.naikapa.domain.routing.GtfsTimeParser
import org.junit.Assert.assertEquals
import org.junit.Test

class GtfsTimeParserTest {
    @Test
    fun parseToSecondsHandlesNormalGtfsTime() {
        assertEquals(6 * 3600 + 15 * 60 + 30, GtfsTimeParser.parseToSeconds("06:15:30"))
    }

    @Test
    fun parseToSecondsHandlesMoreThan24HourGtfsTime() {
        assertEquals(25 * 3600 + 30 * 60, GtfsTimeParser.parseToSeconds("25:30:00"))
    }

    @Test
    fun durationSecondsHandlesSameDayTrip() {
        assertEquals(300, GtfsTimeParser.durationSeconds("08:00:00", "08:05:00"))
    }

    @Test
    fun durationSecondsHandlesMidnightRollover() {
        assertEquals(900, GtfsTimeParser.durationSeconds("23:55:00", "00:10:00"))
    }
}

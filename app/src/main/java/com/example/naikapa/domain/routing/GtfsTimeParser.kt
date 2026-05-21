package com.example.naikapa.domain.routing

object GtfsTimeParser {
    private const val SECONDS_PER_DAY = 24 * 60 * 60

    fun parseToSeconds(time: String): Int {
        val parts = time.trim().split(":")
        require(parts.size == 3) { "Invalid GTFS time: $time" }

        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        val seconds = parts[2].toInt()

        require(hours >= 0) { "Invalid GTFS hour: $time" }
        require(minutes in 0..59) { "Invalid GTFS minute: $time" }
        require(seconds in 0..59) { "Invalid GTFS second: $time" }

        return hours * 3600 + minutes * 60 + seconds
    }

    fun durationSeconds(departureTime: String, arrivalTime: String): Int {
        val departure = parseToSeconds(departureTime)
        var arrival = parseToSeconds(arrivalTime)
        if (arrival < departure) arrival += SECONDS_PER_DAY
        return arrival - departure
    }
}

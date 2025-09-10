package com.simulatedtez.gochat.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


fun formatTimestamp(isoString: String): String {
    return try {
        val instant = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        DateTimeFormatter.ofPattern("hh:mm a")
            .format(instant)
    } catch (e: Exception) {
        ""
    }
}

fun LocalDateTime.toISOString(): String {
    val zoneDateTime = ZonedDateTime.of(this, ZoneOffset.systemDefault())
    return DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    ).format(zoneDateTime)
}
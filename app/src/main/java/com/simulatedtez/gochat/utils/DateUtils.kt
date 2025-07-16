package com.simulatedtez.gochat.utils

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date


fun formatTimestamp(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneOffset.UTC)
            .format(instant)
    } catch (e: Exception) {
        ""
    }
}

fun Date.toISOString(): String {
    return DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    ).withZone(ZoneOffset.UTC).format(this.toInstant())
}
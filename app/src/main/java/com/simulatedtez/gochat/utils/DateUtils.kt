package com.simulatedtez.gochat.utils

import android.icu.util.TimeZone
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date


fun formatTimestamp(isoString: String): String {
    return try {
        val instant = LocalDateTime.parse(isoString)
        DateTimeFormatter.ofPattern("hh:mm a")
            .format(instant)
    } catch (e: Exception) {
        ""
    }
}

fun LocalDateTime.toISOString(): String {
    return DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss"
    ).format(this)
}
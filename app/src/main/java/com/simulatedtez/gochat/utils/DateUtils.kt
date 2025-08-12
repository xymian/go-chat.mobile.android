package com.simulatedtez.gochat.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


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
package com.simulatedtez.gochat.utils

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun formatTimestamp(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        SimpleDateFormat(
            "HH:mm", Locale.getDefault()
        ).format(instant)
    } catch (e: Exception) {
        ""
    }
}
package com.kobe.blogpress_api.exception

import java.time.Duration
import java.time.Instant

class ContentNotYetPublishedException(
    val publishAt: Instant,
    contentType: String = "Content"
) : RuntimeException("$contentType will be published at $publishAt") {

    fun getTimeRemaining(): String {
        val now = Instant.now()
        val duration = Duration.between(now, publishAt)

        return when {
            duration.toDays() > 0 -> {
                val days = duration.toDays()
                val hours = duration.toHours() % 24
                "$days jour${if (days > 1) "s" else ""} et $hours heure${if (hours > 1) "s" else ""}"
            }
            duration.toHours() > 0 -> {
                val hours = duration.toHours()
                val minutes = duration.toMinutes() % 60
                "$hours heure${if (hours > 1) "s" else ""} et $minutes minute${if (minutes > 1) "s" else ""}"
            }
            duration.toMinutes() > 0 -> {
                val minutes = duration.toMinutes()
                "$minutes minute${if (minutes > 1) "s" else ""}"
            }
            else -> "moins d'une minute"
        }
    }
}
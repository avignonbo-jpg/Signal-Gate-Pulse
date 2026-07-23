package com.signalgate.multipoint.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formats an epoch-millis timestamp for display in source health rows, sync
 * history, etc.
 *
 * - 0L (never synced) renders as "Never".
 * - Recent timestamps render as relative time ("5m ago", "3h ago", "2d ago").
 * - Anything older than a week — or any timestamp in the future, which should
 *   never happen but is handled defensively — falls back to an absolute date.
 */
fun Long.humanReadable(): String {
    if (this <= 0L) return "Never"

    val now = System.currentTimeMillis()
    val diffMs = now - this
    if (diffMs < 0) return formatAbsoluteDate(this)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> formatAbsoluteDate(this)
    }
}

private fun formatAbsoluteDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))

app/src/main/java/com/signalgate/sources/PublicSourceParser.kt
package com.signalgate.sources

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * Public source parsing utilities - used by ReliableSourceManager
 * Security: Strict sanitization, memory-safe chunking.
 */
object PublicSourceParser {

    suspend fun parseFTCSource(context: Context): List<String> {
        // Placeholder - now delegated to ReliableSourceManager for better management
        return ReliableSourceManager.fetchReliableBlocklist(context).take(10000) // limit for safety
    }

    fun sanitizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "").take(15)
    }

    // Additional parsers for CSV/JSON if needed
    fun parseLineForNumber(line: String): String? {
        val sanitized = sanitizeNumber(line)
        return if (sanitized.length >= 10) sanitized else null
    }
}

package com.signalgate.sources

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

object PublicSourceParser {
    suspend fun parseFTCSource(context: Context): List<String> {
        // Example: Download + chunked parse (replace with actual FTC URL)
        val url = URL("https://raw.githubusercontent.com/.../ftc-spam.csv")
        val numbers = mutableListOf<String>()
        url.openStream().bufferedReader().useLines { lines ->
            lines.chunked(1000).forEach { chunk -> // Memory-safe
                chunk.forEach { line ->
                    val sanitized = line.trim().replace(Regex("[^0-9+]"), "")
                    if (sanitized.isNotEmpty()) numbers.add(sanitized)
                }
            }
        }
        return numbers
    }

    fun sanitizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "").take(15)
    }
}

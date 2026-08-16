package com.signalgate.pulse.data.security

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class SecureCsvParser(
    private val bloomFilter: BloomFilterEngine
) {
    companion object {
        const val MAX_ROWS = 2_000_000
    }

    /**
     * Streams incoming large target datasets line-by-line without overloading the JVM heap or risking zip bombs.
     * Integrates the sanitization module and populates the Bloom Filter simultaneously.
     *
     * Reaching the row limit is a hard security failure. The callback may have
     * received earlier rows, but callers must discard the entire candidate and
     * must not activate or persist that partial result.
     */
    fun streamAndPopulate(inputStream: InputStream, onRowParsed: (String) -> Unit) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        try {
            var line: String? = reader.readLine()
            var rowCount = 0
            while (line != null) {
                if (line.isNotBlank()) {
                    // Extract the first column representing the raw number vector
                    val rawNumber = line.split(",").firstOrNull()
                    val cleanNumber = SanitizationEngine.sanitizePhoneNumber(rawNumber)

                    if (cleanNumber.isNotEmpty()) {
                        if (rowCount >= MAX_ROWS) {
                            throw CsvResourceLimitExceededException(
                                "CSV source exceeded $MAX_ROWS valid-row limit"
                            )
                        }
                        bloomFilter.insert(cleanNumber)
                        onRowParsed(cleanNumber)
                        rowCount++
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
        }
    }
}

class CsvResourceLimitExceededException(message: String) : Exception(message)

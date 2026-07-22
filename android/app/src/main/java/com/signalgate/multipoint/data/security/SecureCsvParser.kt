package com.signalgate.multipoint.data.security

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class SecureCsvParser(
    private val bloomFilter: BloomFilterEngine
) {
    /**
     * Streams incoming large target datasets line-by-line without overloading the JVM heap or risking zip bombs.
     * Integrates the sanitization module and populates the Bloom Filter simultaneously.
     */
    fun streamAndPopulate(inputStream: InputStream, onRowParsed: (String) -> Unit) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        try {
            var line: String? = reader.readLine()
            var rowCount = 0
            // Enforce a hard safety limit of 2 million entries per file stream to prevent system denial-of-service
            while (line != null && rowCount < 2000000) {
                if (line.isNotBlank()) {
                    // Extract the first column representing the raw number vector
                    val rawNumber = line.split(",").firstOrNull()
                    val cleanNumber = SanitizationEngine.sanitizePhoneNumber(rawNumber)
                    
                    if (cleanNumber.isNotEmpty()) {
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

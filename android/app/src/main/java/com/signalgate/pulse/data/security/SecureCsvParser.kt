package com.signalgate.pulse.data.security

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class SecureCsvParser {
    companion object {
        const val MAX_ROWS = 2_000_000
    }

    /**
     * Streams incoming large target datasets line-by-line without overloading the JVM heap.
     * This default path preserves the historical first-column contract for single-column
     * datasets. Multi-column sources must use [streamColumnByHeader] explicitly.
     */
    fun streamRows(inputStream: InputStream, onRowParsed: (String) -> Unit) {
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            streamRecords(reader, columnIndex = 0, onRowParsed = onRowParsed)
        }
    }

    /**
     * Streams one explicitly named column from a CSV candidate. The header is consumed and
     * never emitted as data. A missing header is a hard failure: silently selecting another
     * column could transform an untrusted identifier field into a security rule.
     */
    fun streamColumnByHeader(
        inputStream: InputStream,
        requiredHeader: String,
        onRowParsed: (String) -> Unit
    ) {
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            val headerLine = reader.readLine()
                ?: throw IllegalArgumentException("CSV source is missing required header: $requiredHeader")
            val columnIndex = parseRecord(headerLine)
                .indexOfFirst { it.trim().equals(requiredHeader, ignoreCase = true) }
            require(columnIndex >= 0) { "CSV source is missing required header: $requiredHeader" }
            streamRecords(reader, columnIndex, onRowParsed)
        }
    }

    private fun streamRecords(
        reader: BufferedReader,
        columnIndex: Int,
        onRowParsed: (String) -> Unit
    ) {
        var rowCount = 0
        var line: String? = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                val rawValue = parseRecord(line).getOrNull(columnIndex)?.trim()
                if (!rawValue.isNullOrEmpty()) {
                    if (rowCount >= MAX_ROWS) {
                        throw CsvResourceLimitExceededException(
                            "CSV source exceeded $MAX_ROWS valid-row limit"
                        )
                    }
                    onRowParsed(rawValue)
                    rowCount++
                }
            }
            line = reader.readLine()
        }
    }

    /**
     * Parses one RFC 4180-style record sufficiently for quoted commas and escaped quotes.
     * Records are consumed line-by-line, so this parser never materializes a whole snapshot.
     */
    private fun parseRecord(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            when (val character = line[index]) {
                '"' -> {
                    if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
                        current.append('"')
                        index++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> if (inQuotes) current.append(character) else {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(character)
            }
            index++
        }
        fields += current.toString()
        return fields
    }

    /**
     * Suspendable counterpart used when a downstream consumer must flush each
     * bounded batch before parsing continues. It preserves the historical first-column
     * raw-record contract; named-column users must remain on the non-suspending
     * bounded source-sync path until a suspending named-column consumer is needed.
     */
    suspend fun streamRowsSuspend(
        inputStream: InputStream,
        onRowParsed: suspend (String) -> Unit
    ) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        try {
            var line: String? = reader.readLine()
            var rowCount = 0
            while (line != null) {
                if (line.isNotBlank()) {
                    val rawNumber = parseRecord(line).firstOrNull()?.trim()
                    if (!rawNumber.isNullOrEmpty()) {
                        if (rowCount >= MAX_ROWS) {
                            throw CsvResourceLimitExceededException(
                                "CSV source exceeded $MAX_ROWS valid-row limit"
                            )
                        }
                        onRowParsed(rawNumber)
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

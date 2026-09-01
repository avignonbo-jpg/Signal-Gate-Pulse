package com.signalgate.pulse.data.security

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class SecureCsvParser {
    companion object {
        const val MAX_ROWS = 2_000_000
    }

    /**
     * Streams incoming large target datasets line-by-line without overloading the JVM heap or risking zip bombs.
     * This parser owns bounded raw record extraction only. Canonicalization,
     * validation, Bloom population, and snapshot activation belong to downstream
     * boundaries and are intentionally not performed here.
     *
     * Reaching the row limit is a hard security failure. The callback may have
     * received earlier rows, but callers must discard the entire candidate and
     * must not activate or persist that partial result.
     */
    fun streamRows(inputStream: InputStream, onRowParsed: (String) -> Unit) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        try {
            var line: String? = reader.readLine()
            var rowCount = 0
            while (line != null) {
                if (line.isNotBlank()) {
                    // Extract the first raw column. Validation and canonicalization
                    // are downstream responsibilities of SourceRecordValidator.
                    val rawNumber = line.split(",").firstOrNull()?.trim()

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

    /**
     * Suspendable counterpart used when a downstream consumer must flush each
     * bounded batch before parsing continues. It preserves the same hard row
     * limit and raw-record contract as [streamRows].
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
                    val rawNumber = line.split(",").firstOrNull()?.trim()
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

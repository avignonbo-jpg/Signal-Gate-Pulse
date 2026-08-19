package com.signalgate.pulse.data.security

import kotlin.math.max

/**
 * Validates a fully parsed source candidate before application-level snapshot
 * activation. Parsing remains responsible for bounded extraction; this class
 * owns candidate sanity policy and returns explicit rejection reasons.
 */
class SnapshotSanityValidator(
    private val limits: Limits = Limits()
) {
    data class Limits(
        val maxBytes: Long = 25L * 1024L * 1024L,
        val maxRecords: Int = 50_000,
        val maxFieldLength: Int = 30,
        val minRecords: Int = 1,
        val maxAgeMs: Long = 7L * 24L * 60L * 60L * 1000L,
        val minimumAcceptedCountRatio: Double = 0.50,
        val maximumAcceptedCountRatio: Double = 2.00,
        val maximumMalformedRatio: Double = 0.05
    )

    data class Candidate(
        val contentType: String?,
        val charset: String?,
        val byteCount: Long,
        val recordCount: Int,
        val acceptedRecordCount: Int,
        val duplicateRecordCount: Int,
        val maxObservedFieldLength: Int,
        val malformedRecordCount: Int,
        val fetchedAt: Long,
        val previousAcceptedRecordCount: Int? = null,
        val expectedContentTypes: Set<String>,
        val now: Long = System.currentTimeMillis()
    )

    sealed interface Result {
        data object Accepted : Result
        data class Rejected(val reason: String) : Result
    }

    fun validate(candidate: Candidate): Result {
        val normalizedContentType = candidate.contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
        if (normalizedContentType == null ||
            normalizedContentType !in candidate.expectedContentTypes.map { it.lowercase() }
        ) {
            return Result.Rejected("Unsupported or missing content type")
        }
        if (candidate.charset != null && !candidate.charset.equals("UTF-8", ignoreCase = true)) {
            return Result.Rejected("Unsupported source encoding")
        }
        if (candidate.byteCount !in 1..limits.maxBytes) {
            return Result.Rejected("Snapshot byte limit exceeded or body empty")
        }
        if (candidate.recordCount !in limits.minRecords..limits.maxRecords) {
            return Result.Rejected("Snapshot record count outside allowed range")
        }
        if (candidate.acceptedRecordCount !in limits.minRecords..limits.maxRecords) {
            return Result.Rejected("Accepted record count outside allowed range")
        }
        if (candidate.maxObservedFieldLength > limits.maxFieldLength) {
            return Result.Rejected("Snapshot field length exceeds limit")
        }
        if (candidate.duplicateRecordCount > 0 &&
            candidate.acceptedRecordCount > candidate.recordCount
        ) {
            return Result.Rejected("Snapshot duplicate accounting is inconsistent")
        }
        val malformedRatio = candidate.malformedRecordCount.toDouble() /
            max(candidate.recordCount, 1)
        if (malformedRatio > limits.maximumMalformedRatio) {
            return Result.Rejected("Malformed-record ratio exceeds limit")
        }
        if (candidate.now - candidate.fetchedAt > limits.maxAgeMs) {
            return Result.Rejected("Snapshot is stale")
        }
        candidate.previousAcceptedRecordCount?.let { previous ->
            if (previous >= limits.minRecords) {
                val ratio = candidate.acceptedRecordCount.toDouble() / previous.toDouble()
                if (ratio < limits.minimumAcceptedCountRatio ||
                    ratio > limits.maximumAcceptedCountRatio
                ) {
                    return Result.Rejected("Catastrophic snapshot count change")
                }
            }
        }
        return Result.Accepted
    }
}

package com.signalgate.pulse.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotSanityValidatorTest {

    private val validator = SnapshotSanityValidator(
        SnapshotSanityValidator.Limits(
            maxBytes = 100,
            maxRecords = 10,
            maxFieldLength = 15,
            maxAgeMs = 1_000,
            minimumAcceptedCountRatio = 0.5,
            maximumAcceptedCountRatio = 2.0,
            maximumMalformedRatio = 0.25
        )
    )

    @Test
    fun acceptsValidCandidateAndAllowsDeduplicatedRecords() {
        assertEquals(
            SnapshotSanityValidator.Result.Accepted,
            validator.validate(candidate(duplicateRecordCount = 2, acceptedRecordCount = 3))
        )
    }

    @Test
    fun rejectsUnsupportedContentTypeAndEncoding() {
        assertRejected(candidate(contentType = "application/octet-stream"))
        assertRejected(candidate(charset = "ISO-8859-1"))
    }

    @Test
    fun rejectsByteRecordAndFieldLimits() {
        assertRejected(candidate(byteCount = 101))
        assertRejected(candidate(recordCount = 11, acceptedRecordCount = 11))
        assertRejected(candidate(maxObservedFieldLength = 16))
    }

    @Test
    fun rejectsMalformedRatioAndStaleCandidate() {
        assertRejected(candidate(malformedRecordCount = 3))
        assertRejected(candidate(fetchedAt = 0, now = 2_000))
    }

    @Test
    fun rejectsCatastrophicCountChange() {
        assertRejected(candidate(acceptedRecordCount = 4, previousAcceptedRecordCount = 10))
        assertRejected(candidate(acceptedRecordCount = 10, previousAcceptedRecordCount = 4))
    }

    private fun assertRejected(candidate: SnapshotSanityValidator.Candidate) {
        assertTrue(validator.validate(candidate) is SnapshotSanityValidator.Result.Rejected)
    }

    private fun candidate(
        contentType: String? = "text/csv",
        charset: String? = "UTF-8",
        byteCount: Long = 50,
        recordCount: Int = 5,
        acceptedRecordCount: Int = 5,
        duplicateRecordCount: Int = 0,
        maxObservedFieldLength: Int = 12,
        malformedRecordCount: Int = 0,
        fetchedAt: Long = 1_000,
        previousAcceptedRecordCount: Int? = null,
        now: Long = 1_000
    ) = SnapshotSanityValidator.Candidate(
        contentType = contentType,
        charset = charset,
        byteCount = byteCount,
        recordCount = recordCount,
        acceptedRecordCount = acceptedRecordCount,
        duplicateRecordCount = duplicateRecordCount,
        maxObservedFieldLength = maxObservedFieldLength,
        malformedRecordCount = malformedRecordCount,
        fetchedAt = fetchedAt,
        previousAcceptedRecordCount = previousAcceptedRecordCount,
        expectedContentTypes = setOf("text/csv"),
        now = now
    )
}

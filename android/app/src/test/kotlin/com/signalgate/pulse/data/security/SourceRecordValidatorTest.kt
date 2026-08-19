package com.signalgate.pulse.data.security

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceRecordValidatorTest {

    @Test
    fun canonicalizePhone_sanitizesAndEnforcesLengthBoundary() {
        assertEquals(
            "+15551234567",
            SourceRecordValidator.canonicalizePhone(" +1 (555) 123-4567 ")
        )
        assertNull(SourceRecordValidator.canonicalizePhone("123456"))
        assertNull(SourceRecordValidator.canonicalizePhone(""))
    }

    @Test
    fun secureCsvParser_emitsRawRecordBeforeValidation() {
        val rawRecords = mutableListOf<String>()
        SecureCsvParser().streamRows(
            ByteArrayInputStream(" +1 (555) 123-4567 ,metadata\n".toByteArray())
        ) { rawRecords += it }

        assertEquals(listOf("+1 (555) 123-4567"), rawRecords)
        assertEquals(
            "+15551234567",
            SourceRecordValidator.canonicalizePhone(rawRecords.single())
        )
    }
}

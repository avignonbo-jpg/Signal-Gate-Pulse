package com.signalgate.pulse.data.security

import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Phase 0.5 regression coverage: a CSV candidate that exceeds the hard valid-row
 * limit must be rejected rather than returned as a truncated security dataset.
 */
class SecureCsvParserLimitTest {

    @Test
    fun rowLimitExceeded_isHardFailure() {
        var callbackCount = 0
        val parser = SecureCsvParser(BloomFilterEngine())

        val failure = assertThrows(CsvResourceLimitExceededException::class.java) {
            parser.streamAndPopulate(GeneratedCsvInputStream(SecureCsvParser.MAX_ROWS + 1)) {
                callbackCount++
            }
        }

        assertEquals(SecureCsvParser.MAX_ROWS, callbackCount)
        assertEquals(
            "CSV source exceeded ${SecureCsvParser.MAX_ROWS} valid-row limit",
            failure.message
        )
    }

    /** Generates valid CSV rows without allocating the complete candidate in memory. */
    private class GeneratedCsvInputStream(private val rowCount: Int) : InputStream() {
        private var rowsEmitted = 0
        private var current = ByteArray(0)
        private var offset = 0

        override fun read(buffer: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            var written = 0
            while (written < len) {
                if (offset >= current.size) {
                    if (rowsEmitted >= rowCount) break
                    current = "+1555${(rowsEmitted % 10000000).toString().padStart(7, '0')}\n"
                        .toByteArray(Charsets.UTF_8)
                    rowsEmitted++
                    offset = 0
                }
                val copyLength = minOf(len - written, current.size - offset)
                current.copyInto(buffer, off + written, offset, offset + copyLength)
                offset += copyLength
                written += copyLength
            }
            return if (written == 0) -1 else written
        }

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) == -1) -1 else one[0].toInt()
        }
    }
}

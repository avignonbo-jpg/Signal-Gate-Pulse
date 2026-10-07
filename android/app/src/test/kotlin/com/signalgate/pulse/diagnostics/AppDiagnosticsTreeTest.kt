package com.signalgate.pulse.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDiagnosticsTreeTest {
    @Test
    fun `buffer retains only newest entries and clear empties the flow`() {
        val tree = AppDiagnosticsTree(
            capacity = 2,
            logcatSink = { _, _, _ -> },
            clockMillis = { 123L }
        )

        tree.append(priority = 4, tag = "Test", message = "first")
        tree.append(priority = 4, tag = "Test", message = "second")
        tree.append(priority = 4, tag = "Test", message = "third")

        assertEquals(listOf("second", "third"), tree.entries.value.map { it.message })
        assertEquals(listOf(2L, 3L), tree.entries.value.map { it.id })
        assertEquals(listOf(123L, 123L), tree.entries.value.map { it.timestampMillis })

        tree.clear()
        assertTrue(tree.entries.value.isEmpty())
    }

    @Test
    fun `buffer and Logcat preserve the original message including digits`() {
        val mirrored = mutableListOf<String>()
        val tree = AppDiagnosticsTree(
            capacity = 4,
            logcatSink = { _, _, message -> mirrored += message },
            clockMillis = { 500L }
        )
        val originalMessage = "callflow-id=12345678901234567890 elapsed=987654321ms"

        tree.append(priority = 6, tag = "TestTag", message = originalMessage)

        assertEquals(originalMessage, tree.entries.value.single().message)
        assertEquals(originalMessage, mirrored.single())
    }

    @Test
    fun `long messages are truncated at the default limit with suffix`() {
        val mirrored = mutableListOf<String>()
        val tree = AppDiagnosticsTree(
            logcatSink = { _, _, message -> mirrored += message },
            clockMillis = { 500L }
        )
        val originalMessage = "x".repeat(AppDiagnosticsTree.DEFAULT_MESSAGE_CHARS + 128)

        tree.append(priority = 6, tag = "TestTag", message = originalMessage)

        val stored = tree.entries.value.single().message
        val expected = originalMessage.take(
            AppDiagnosticsTree.DEFAULT_MESSAGE_CHARS - AppDiagnosticsTree.TRUNCATION_SUFFIX.length
        ) + AppDiagnosticsTree.TRUNCATION_SUFFIX
        assertEquals(AppDiagnosticsTree.DEFAULT_MESSAGE_CHARS, stored.length)
        assertEquals(expected, stored)
        assertEquals(stored, mirrored.single())
    }

    @Test
    fun `sink failure does not discard an in-app entry`() {
        val tree = AppDiagnosticsTree(
            logcatSink = { _, _, _ -> error("Logcat unavailable") },
            clockMillis = { 1L }
        )

        tree.append(priority = 5, tag = "Test", message = "still buffered")

        assertEquals("still buffered", tree.entries.value.single().message)
    }
}

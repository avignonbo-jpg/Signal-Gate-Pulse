package com.signalgate.pulse.logic

import com.signalgate.pulse.database.repositories.DataSourceRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Phase 0.5 / 0.8 regression coverage.
 *
 * XLSX row and shared-string limits are hard security boundaries. A candidate
 * that exceeds either bound must throw rather than return a partial dataset.
 */
class DataSyncEngineXlsxLimitTest {

    @Test
    fun rowLimitExceeded_isHardFailure() = runBlocking {
        val engine = engineWithLimits(maxRows = 2)
        val xlsx = xlsxArchive(
            sheetXml = sheetWithInlineRows(
                "+15550000001",
                "+15550000002",
                "+15550000003"
            )
        )

        val failure = try {
            engine.parseXLSXFile(ByteArrayInputStream(xlsx), sourceId = 7)
            error("row-limit overflow must fail")
        } catch (expected: DataSyncEngine.RowLimitExceededException) {
            expected
        }

        assertEquals("Row limit 2 reached", failure.message)
    }

    @Test
    fun sharedStringLimitExceeded_isHardFailure() = runBlocking {
        val engine = engineWithLimits(maxSharedStrings = 2)
        val xlsx = xlsxArchive(
            sharedStringsXml = sharedStrings(
                "+15550000001",
                "+15550000002",
                "+15550000003"
            ),
            sheetXml = sheetWithSharedRows(0, 1)
        )

        val failure = try {
            engine.parseXLSXFile(ByteArrayInputStream(xlsx), sourceId = 7)
            error("shared-string-limit overflow must fail")
        } catch (expected: DataSyncEngine.SharedStringsLimitExceededException) {
            expected
        }

        assertEquals("Shared strings limit 2 reached", failure.message)
    }

    @Test
    fun expandedSharedStringByteLimitExceeded_isHardFailure() = runBlocking {
        val engine = engineWithLimits(maxExpandedSharedStringBytes = 8)
        val xlsx = xlsxArchive(
            sharedStringsXml = sharedStrings("123456789"),
            sheetXml = sheetWithSharedRows(0)
        )

        val failure = try {
            engine.parseXLSXFile(ByteArrayInputStream(xlsx), sourceId = 7)
            error("expanded shared-string byte overflow must fail")
        } catch (expected: DataSyncEngine.ExpandedSharedStringsLimitExceededException) {
            expected
        }

        assertEquals(
            "Expanded shared strings exceeded 8 byte limit",
            failure.message
        )
    }

    @Test
    fun cellLengthLimitExceeded_isHardFailure() = runBlocking {
        val engine = engineWithLimits(maxCellLength = 6)
        val xlsx = xlsxArchive(sheetWithInlineRows("+15550000001"))

        val failure = try {
            engine.parseXLSXFile(ByteArrayInputStream(xlsx), sourceId = 7)
            error("cell-length overflow must fail")
        } catch (expected: DataSyncEngine.CellLengthLimitExceededException) {
            expected
        }

        assertEquals("Cell exceeded 6 byte limit", failure.message)
    }

    @Test
    fun csvParser_emitsBoundedBatches() = runBlocking {
        val engine = DataSyncEngine(
            dataSourceRepository = mock<DataSourceRepository>(),
            csvParser = com.signalgate.pulse.data.security.SecureCsvParser()
        )
        val batches = mutableListOf<List<com.signalgate.pulse.database.entities.UnifiedEntryEntity>>()
        val csv = "+15550000001\n+15550000002\n+15550000003\n+"

        engine.streamCsvFile(
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            sourceId = 7,
            batchSize = 2,
            onBatch = { batches += it }
        )

        assertEquals(listOf(2, 1), batches.map { it.size })
        assertEquals(
            listOf("+15550000001", "+15550000002", "+15550000003"),
            batches.flatten().map { it.phoneNumber }
        )
    }

    private fun engineWithLimits(
        maxRows: Int = 2_000_000,
        maxSharedStrings: Int = 2_000_000,
        maxExpandedSharedStringBytes: Int = 64 * 1024 * 1024,
        maxCellLength: Int = 64 * 1024
    ): DataSyncEngine = DataSyncEngine(
        dataSourceRepository = mock<DataSourceRepository>(),
        csvParser = mock(),
        parserLimits = DataSyncEngine.ParserLimits(
            maxRows = maxRows,
            maxSharedStrings = maxSharedStrings,
            maxExpandedSharedStringBytes = maxExpandedSharedStringBytes,
            maxCellLength = maxCellLength
        )
    )

    private fun xlsxArchive(
        sheetXml: String,
        sharedStringsXml: String? = null
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray())
            zip.closeEntry()
            if (sharedStringsXml != null) {
                zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
                zip.write(sharedStringsXml.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun sheetWithInlineRows(vararg phoneNumbers: String): String = buildString {
        append("<worksheet><sheetData>")
        append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>phone</t></is></c></row>")
        phoneNumbers.forEachIndexed { index, phone ->
            val row = index + 2
            append("<row r=\"$row\"><c r=\"A$row\" t=\"inlineStr\"><is><t>$phone</t></is></c></row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun sheetWithSharedRows(vararg indexes: Int): String = buildString {
        append("<worksheet><sheetData>")
        append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>phone</t></is></c></row>")
        indexes.forEachIndexed { position, index ->
            val row = position + 2
            append("<row r=\"$row\"><c r=\"A$row\" t=\"s\"><v>$index</v></c></row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun sharedStrings(vararg values: String): String = buildString {
        append("<sst>")
        values.forEach { value -> append("<si><t>$value</t></si>") }
        append("</sst>")
    }
}

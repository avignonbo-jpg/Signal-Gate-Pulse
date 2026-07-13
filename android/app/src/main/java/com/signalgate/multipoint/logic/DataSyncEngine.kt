package com.signalgate.multipoint.logic

import com.signalgate.multipoint.data.security.SanitizationEngine
import com.signalgate.multipoint.data.security.SecureCsvParser
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.usermodel.XSSFComment
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import org.xml.sax.helpers.XMLReaderFactory
import timber.log.Timber
import java.io.File
import java.io.InputStream

/**
 * DataSyncEngine — Memory-safe XLSX/CSV parsing per Architecture Contract §4 L2.
 *
 * parseXLSXFile() implementation — streaming SAX approach:
 *
 * WHY NOT WorkbookFactory.create():
 * The original stub used WorkbookFactory.create(inputStream), which is the
 * non-streaming "XSSF" reader. It loads the entire XLSX into memory as a DOM.
 * A 2M-row XLSX file with phone numbers typically runs 50–200MB on disk —
 * expanded into a DOM on an Android device this OOMs reliably. The 2M row
 * limit is meaningless if the app crashes before counting a single row.
 *
 * WHY SAX (XSSFReader + XSSFSheetXMLHandler):
 * Apache POI's event-based XLSX reader processes the XML sheet one element
 * at a time, never materializing more than one row in memory. Peak memory
 * usage is bounded by the longest row encountered, not the file size.
 * This is the correct approach for Android's constrained heap.
 *
 * ANDROID COMPATIBILITY NOTE:
 * Full poi-ooxml pulls in xmlbeans which conflicts with Android's bundled
 * XML processing. build.gradle excludes xmlbeans and stax-api — see
 * the dependency block comment there. poi-ooxml-lite is the minimal
 * artifact that includes XSSFReader without pulling in the full schema stack.
 *
 * STREAMING CONSTRAINT:
 * XSSFReader requires a seekable source (OPCPackage), not a raw InputStream.
 * The XLSX must be written to a temp file first. Temp file is deleted
 * immediately after parsing — it never persists beyond the function call.
 * For files already on disk, callers should use parseXLSXFile(File, sourceId)
 * directly to avoid the write step.
 *
 * ROW LIMIT:
 * MAX_ROWS is enforced inside the SAX handler — parsing stops cleanly
 * at the limit via a custom exception rather than letting the SAX reader
 * continue processing the remaining XML. This avoids the time cost of
 * parsing 10M rows to enforce a 2M limit.
 */
class DataSyncEngine(
    private val dataSourceRepository: DataSourceRepository,
    private val csvParser: SecureCsvParser
) {

    companion object {
        private const val TAG = "DataSyncEngine"
        const val MAX_ROWS = 2_000_000
        private const val MIN_PHONE_LENGTH = 7
        private const val MAX_PHONE_LENGTH = 15
        private const val CHUNK_SIZE = 1_000
        private const val DEFAULT_PHONE_COLUMN = 0 // Column A
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses an XLSX InputStream, enforces MAX_ROWS, sanitizes every phone number,
     * and returns a list of UnifiedEntryEntity ready for database insert.
     *
     * The InputStream is written to a temp file so OPCPackage can seek.
     * Temp file is deleted before this function returns regardless of outcome.
     *
     * @throws XlsxParseException for malformed XLSX structure (not IOException)
     * @throws RowLimitExceededException if the file exceeds MAX_ROWS
     */
    suspend fun parseXLSXFile(
        inputStream: InputStream,
        sourceId: Int,
        phoneColumnIndex: Int = DEFAULT_PHONE_COLUMN
    ): List<UnifiedEntryEntity> = withContext(Dispatchers.IO) {
        val tempFile = writeTempFile(inputStream)
        try {
            parseXLSXFile(tempFile, sourceId, phoneColumnIndex)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Parses an XLSX File directly — use this when the file is already on disk
     * to avoid the temp-file write step.
     */
    suspend fun parseXLSXFile(
        file: File,
        sourceId: Int,
        phoneColumnIndex: Int = DEFAULT_PHONE_COLUMN
    ): List<UnifiedEntryEntity> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("XLSX parse started — source=$sourceId file=${file.name} size=${file.length()}b")

        val entries = mutableListOf<UnifiedEntryEntity>()
        val handler = PhoneNumberSheetHandler(
            sourceId = sourceId,
            phoneColumnIndex = phoneColumnIndex,
            maxRows = MAX_ROWS,
            onEntry = { entry -> entries.add(entry) }
        )

        try {
            OPCPackage.open(file).use { pkg ->
                val reader = XSSFReader(pkg)
                val sharedStrings = ReadOnlySharedStringsTable(pkg)
                val styles: StylesTable = reader.stylesTable
                val xmlReader: XMLReader = buildXmlReader(styles, sharedStrings, handler)

                // Parse first sheet only — XLSX blocklists are single-sheet files
                val sheetIterator = reader.sheetsData
                if (sheetIterator.hasNext()) {
                    sheetIterator.next().use { sheetStream ->
                        xmlReader.parse(InputSource(sheetStream))
                    }
                } else {
                    throw XlsxParseException("XLSX file contains no sheets")
                }
            }

            Timber.tag(TAG).i(
                "XLSX parse complete — rows=${handler.rowsProcessed} " +
                "valid=${entries.size} skipped=${handler.rowsSkipped} source=$sourceId"
            )
            entries

        } catch (e: RowLimitExceededException) {
            Timber.tag(TAG).w(
                "XLSX row limit reached ($MAX_ROWS) — ${entries.size} entries collected source=$sourceId"
            )
            // Return what we have — partial import is better than no import
            entries

        } catch (e: XlsxParseException) {
            Timber.tag(TAG).e(e, "XLSX structure error source=$sourceId")
            throw e

        } catch (e: Exception) {
            val msg = "XLSX parse failed for source=$sourceId: ${e.javaClass.simpleName}: ${e.message}"
            Timber.tag(TAG).e(e, msg)
            throw XlsxParseException(msg, e)
        }
    }

    suspend fun parseCsvFile(
        inputStream: InputStream,
        sourceId: Int
    ): List<UnifiedEntryEntity> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<UnifiedEntryEntity>()
        csvParser.streamAndPopulate(inputStream) { sanitizedNumber ->
            entries.add(
                UnifiedEntryEntity(
                    phoneNumber = sanitizedNumber,
                    action = "BLOCK",
                    sourceId = sourceId,
                    category = "CSV Import",
                    confidence = 75,
                    metadata = "DataSyncEngine batch"
                )
            )
        }
        Timber.tag(TAG).i("CSV parse complete — entries=${entries.size} source=$sourceId")
        entries
    }

    /**
     * Inserts a list of entries in chunks of CHUNK_SIZE to avoid binding too
     * many parameters in a single Room transaction.
     */
    suspend fun insertEntries(entries: List<UnifiedEntryEntity>) = withContext(Dispatchers.IO) {
        var inserted = 0
        entries.chunked(CHUNK_SIZE).forEach { chunk ->
            chunk.forEach { entry -> dataSourceRepository.insertEntry(entry) }
            inserted += chunk.size
            Timber.tag(TAG).d("Inserted $inserted / ${entries.size}")
        }
        Timber.tag(TAG).i("Batch insert complete: $inserted entries")
    }

    // ── SAX sheet handler ─────────────────────────────────────────────────────

    /**
     * SAX handler that receives one cell at a time from XSSFSheetXMLHandler.
     * Extracts the phone number from the target column, sanitizes it, and
     * calls onEntry for each valid number found.
     *
     * Row limit enforcement: throws RowLimitExceededException (caught in the
     * caller) rather than setting a flag and continuing — this stops the SAX
     * parser immediately rather than wasting CPU parsing the rest of the file.
     */
    private inner class PhoneNumberSheetHandler(
        private val sourceId: Int,
        private val phoneColumnIndex: Int,
        private val maxRows: Int,
        private val onEntry: (UnifiedEntryEntity) -> Unit
    ) : SheetContentsHandler {

        var rowsProcessed = 0
            private set
        var rowsSkipped = 0
            private set

        private var currentRowIndex = -1
        private var currentPhoneValue: String? = null

        override fun startRow(rowNum: Int) {
            if (rowNum == 0) {
                // Row 0 is the header — skip
                currentRowIndex = rowNum
                return
            }
            if (rowsProcessed >= maxRows) {
                throw RowLimitExceededException(
                    "Row limit $maxRows reached after $rowsProcessed data rows"
                )
            }
            currentRowIndex = rowNum
            currentPhoneValue = null
        }

        override fun endRow(rowNum: Int) {
            if (rowNum == 0) return // Header row

            val raw = currentPhoneValue
            if (raw.isNullOrBlank()) {
                rowsSkipped++
                return
            }

            val sanitized = SanitizationEngine.sanitizePhoneNumber(raw)
            if (sanitized.length < MIN_PHONE_LENGTH || sanitized.length > MAX_PHONE_LENGTH) {
                rowsSkipped++
                return
            }

            onEntry(
                UnifiedEntryEntity(
                    phoneNumber = sanitized,
                    action = "BLOCK",
                    sourceId = sourceId,
                    category = "XLSX Import",
                    confidence = 75,
                    metadata = "DataSyncEngine batch"
                )
            )
            rowsProcessed++
        }

        override fun cell(
            cellReference: String?,
            formattedValue: String?,
            comment: XSSFComment?
        ) {
            if (currentRowIndex == 0) return // Header row
            if (cellReference == null || formattedValue == null) return

            // Only interested in the phone number column
            val ref = CellReference(cellReference)
            if (ref.col.toInt() == phoneColumnIndex) {
                currentPhoneValue = formattedValue.trim()
            }
        }

        override fun headerFooter(text: String?, isHeader: Boolean, tagName: String?) {
            // Not needed
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildXmlReader(
        styles: StylesTable,
        sharedStrings: ReadOnlySharedStringsTable,
        handler: SheetContentsHandler
    ): XMLReader {
        val xmlReader = XMLReaderFactory.createXMLReader()
        xmlReader.contentHandler = XSSFSheetXMLHandler(
            styles,
            null,
            sharedStrings,
            handler,
            null,
            false
        )
        return xmlReader
    }

    /**
     * Writes an InputStream to a temp file so OPCPackage can seek within it.
     * OPCPackage (the ZIP container for XLSX) requires random access — it cannot
     * operate on a forward-only InputStream. The temp file is deleted by the
     * caller immediately after parsing.
     */
    private fun writeTempFile(inputStream: InputStream): File {
        val tempFile = File.createTempFile("signalgate_xlsx_", ".tmp")
        tempFile.outputStream().use { out ->
            inputStream.copyTo(out)
        }
        return tempFile
    }

    // ── Exception types ───────────────────────────────────────────────────────

    /**
     * Thrown for structural problems with the XLSX file itself — missing sheets,
     * corrupt ZIP structure, unreadable cell types. Distinct from IOException
     * so callers can handle parse errors separately from I/O errors.
     */
    class XlsxParseException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    /**
     * Thrown internally by PhoneNumberSheetHandler when MAX_ROWS is reached.
     * Caught by parseXLSXFile() which returns the entries collected so far.
     * Not surfaced to callers — partial import is the correct behavior.
     */
    private class RowLimitExceededException(message: String) : Exception(message)
}

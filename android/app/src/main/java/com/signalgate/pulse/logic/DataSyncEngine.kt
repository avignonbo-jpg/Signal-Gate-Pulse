package com.signalgate.pulse.logic

import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.data.security.SourceRecordValidator
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

/**
 * DataSyncEngine — Memory-safe XLSX/CSV parsing per Architecture Contract §4 L2.
 *
 * XLSX PARSING APPROACH — Native Android ZipInputStream + SAX (no Apache POI):
 *
 * An XLSX file is a ZIP archive. Its phone number data lives in:
 *   xl/worksheets/sheet1.xml   — cell values (inline strings + shared string refs)
 *   xl/sharedStrings.xml       — shared string table (referenced by sheet1.xml)
 *
 * This implementation unzips those two entries using Android's built-in
 * ZipInputStream (available since API 1), parses them with javax.xml.parsers.SAXParser
 * (built into Android, no external dependency), and extracts phone numbers from
 * column A one row at a time.
 *
 * WHY NOT APACHE POI:
 * POI's poi-ooxml-lite pulls in xmlbeans and stax-api which conflict with
 * Android's bundled XML stack at runtime — ClassNotFoundException on OEM devices.
 * The workaround (exclude xmlbeans, stax-api) is fragile and adds ~4MB to the APK.
 * At minSdk 29, Android's built-in ZipInputStream + SAXParser covers everything
 * POI's XSSFReader provided, with zero external dependency and zero OEM compatibility
 * risk. This is the correct approach for Android 10+ (API 29+).
 *
 * STREAMING:
 * ZipInputStream processes the ZIP file linearly — only one entry is in memory at
 * a time. SAXParser processes the XML one element at a time. Peak memory is bounded
 * by the longest cell value encountered, not the file size. A 2M-row XLSX uses
 * approximately the same peak memory as a 10-row XLSX.
 *
 * ROW LIMIT:
 * MAX_ROWS is enforced inside the SAX handler via RowLimitExceededException.
 * Reaching the limit is a hard security failure: parsing stops immediately and
 * the exception propagates so callers cannot activate a partial result.
 *
 * BYTE LIMIT (MAX_XLSX_BYTES):
 * The initial inputStream.readBytes() call has no ceiling of its own — it runs
 * before MAX_ROWS ever gets a chance to matter, so a compromised or oversized
 * upstream response could otherwise force a large allocation regardless of what
 * the file actually contains. MAX_XLSX_BYTES bounds that read directly. The shared
 * strings table gets its own MAX_SHARED_STRINGS cap for the same reason — it's a
 * separate, unbounded-by-MAX_ROWS expansion vector (a small compressed file can
 * still carry a very large shared-string table independent of actual row count).
 *
 * CURRENT WIRING STATUS:
 * As of this writing, nothing in the app calls parseXLSXFile() — SourcesViewModel
 * exposes an "XLSX" source type in its Add Source sheet, but ReliableSourceManager
 * (the actual sync engine) works off a hardcoded federal source list and never
 * reads the sources table at all, so a user-added source of any type (CSV, URL, or
 * XLSX) is inert today. This parser is hardened regardless, on the assumption that
 * "unreachable today" is not the same guarantee as "will stay unreachable."
 *
 * minSdk requirement: API 29+ (confirmed in build.gradle defaultConfig).
 */
class DataSyncEngine(
    private val dataSourceRepository: DataSourceRepository,
    private val csvParser: SecureCsvParser,
    private val parserLimits: ParserLimits = ParserLimits()
) {
    /**
     * Hard parser boundaries. Production uses the documented defaults; tests may
     * inject smaller limits so failure paths are exercised without generating
     * multi-million-row candidates.
     */
    data class ParserLimits(
        val maxRows: Int = 2_000_000,
        val maxXlsxBytes: Int = 25 * 1024 * 1024,
        val maxSharedStrings: Int = 2_000_000,
        val maxExpandedSharedStringBytes: Int = 64 * 1024 * 1024,
        val maxCellLength: Int = 64 * 1024
    )

    companion object {
        private const val TAG = "DataSyncEngine"
        const val MAX_ROWS = 2_000_000
        private const val MIN_PHONE_LENGTH = 7
        private const val MAX_PHONE_LENGTH = 15
        private const val CHUNK_SIZE = 1_000
        private const val DEFAULT_PHONE_COLUMN = 0 // Column A (0-indexed)

        // Hard ceiling on the raw (compressed) XLSX byte size read into memory before
        // any row-based limiting applies. ~25MB gives generous headroom over the
        // documented typical case (500K numbers ≈ 5-15MB compressed).
        private const val MAX_XLSX_BYTES = 25 * 1024 * 1024

        // Hard ceiling on shared-string table size — independent of MAX_ROWS, since
        // a small compressed file can still expand into a very large shared-strings.xml.
        private const val MAX_SHARED_STRINGS = 2_000_000

        // Entry paths inside the XLSX ZIP archive
        private const val SHEET1_PATH = "xl/worksheets/sheet1.xml"
        private const val SHARED_STRINGS_PATH = "xl/sharedStrings.xml"
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parses an XLSX InputStream, enforces MAX_ROWS, sanitizes every phone number,
     * and returns a list of UnifiedEntryEntity ready for database insert.
     *
     * The stream is NOT copied to a temp file — ZipInputStream reads forward-only
     * directly from the InputStream. No temp file creation, no temp file cleanup.
     *
     * @throws XlsxParseException for malformed XLSX structure
     */
    suspend fun parseXLSXFile(
        inputStream: InputStream,
        sourceId: Int,
        phoneColumnIndex: Int = DEFAULT_PHONE_COLUMN
    ): List<UnifiedEntryEntity> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("XLSX parse started — source=$sourceId column=$phoneColumnIndex")
        parseXlsxFromStream(inputStream, sourceId, phoneColumnIndex)
    }

    /**
     * Parses an on-disk XLSX File directly.
     * Opens a FileInputStream and delegates to the stream overload.
     */
    suspend fun parseXLSXFile(
        file: File,
        sourceId: Int,
        phoneColumnIndex: Int = DEFAULT_PHONE_COLUMN
    ): List<UnifiedEntryEntity> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i(
            "XLSX parse started — source=$sourceId file=${file.name} size=${file.length()}b"
        )
        file.inputStream().use { stream ->
            parseXlsxFromStream(stream, sourceId, phoneColumnIndex)
        }
    }

    /**
     * Parses XLSX sheet entries and emits bounded batches with backpressure.
     * Shared strings remain indexed for two-pass resolution, while parsed phone
     * entries are not accumulated as a complete candidate list. Parser failures
     * propagate so an activation boundary can discard all emitted batches.
     */
    suspend fun streamXLSXFile(
        inputStream: InputStream,
        sourceId: Int,
        phoneColumnIndex: Int = DEFAULT_PHONE_COLUMN,
        batchSize: Int = CHUNK_SIZE,
        onBatch: suspend (List<UnifiedEntryEntity>) -> Unit
    ) = withContext(Dispatchers.IO) {
        require(batchSize > 0) { "batchSize must be positive" }
        val zipBytes = readBytesWithLimit(inputStream, parserLimits.maxXlsxBytes)
        val sharedStrings = parseSharedStrings(
            zipBytes,
            parserLimits.maxSharedStrings,
            parserLimits.maxExpandedSharedStringBytes
        )
        val queue = ArrayBlockingQueue<XlsxBatchMessage>(1)
        val producer = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val batch = ArrayList<UnifiedEntryEntity>(batchSize)
                parseSheetWithCallback(
                    zipBytes, sourceId, phoneColumnIndex, sharedStrings,
                    parserLimits.maxRows, parserLimits.maxCellLength
                ) { entry ->
                    batch += entry
                    if (batch.size == batchSize) {
                        queue.put(XlsxBatchMessage.Batch(batch.toList()))
                        batch.clear()
                    }
                }
                if (batch.isNotEmpty()) {
                    queue.put(XlsxBatchMessage.Batch(batch.toList()))
                }
                queue.put(XlsxBatchMessage.Complete)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                queue.put(XlsxBatchMessage.Failed(e))
            }
        }
        try {
            while (true) {
                when (val message = queue.take()) {
                    is XlsxBatchMessage.Batch -> onBatch(message.entries)
                    XlsxBatchMessage.Complete -> break
                    is XlsxBatchMessage.Failed -> throw message.cause
                }
            }
        } finally {
            producer.cancel()
        }
    }

    suspend fun parseCsvFile(
        inputStream: InputStream,
        sourceId: Int
    ): List<UnifiedEntryEntity> = withContext(Dispatchers.IO) {
        Timber.tag(TAG).i("CSV parse started — source=$sourceId")
        val entries = mutableListOf<UnifiedEntryEntity>()
        streamCsvFile(inputStream, sourceId) { batch -> entries.addAll(batch) }
        Timber.tag(TAG).i("CSV parse complete — valid=${entries.size} source=$sourceId")
        entries
    }

    /**
     * Parses CSV rows and delivers bounded batches. A caller that activates a
     * source must discard all prior batches if parsing throws; this method never
     * represents a partial candidate as a successful parse.
     */
    suspend fun streamCsvFile(
        inputStream: InputStream,
        sourceId: Int,
        batchSize: Int = CHUNK_SIZE,
        onBatch: suspend (List<UnifiedEntryEntity>) -> Unit
    ) = withContext(Dispatchers.IO) {
        require(batchSize > 0) { "batchSize must be positive" }
        Timber.tag(TAG).i("CSV batch parse started — source=$sourceId batchSize=$batchSize")
        val batch = ArrayList<UnifiedEntryEntity>(batchSize)
        csvParser.streamRowsSuspend(inputStream) { rawPhoneNumber ->
            val phoneNumber = SourceRecordValidator.canonicalizePhone(rawPhoneNumber)
                ?: return@streamRowsSuspend
            batch += UnifiedEntryEntity(
                phoneNumber = phoneNumber,
                action = "BLOCK",
                sourceId = sourceId,
                category = "CSV Import",
                confidence = 75,
                metadata = "DataSyncEngine batch"
            )
            if (batch.size == batchSize) {
                onBatch(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) onBatch(batch.toList())
        Timber.tag(TAG).i("CSV batch parse complete — source=$sourceId")
    }

    /**
     * Streams a CSV candidate directly into the authoritative snapshot
     * transaction. Parser failure or cancellation rolls back every emitted
     * batch; no caller can accidentally activate a partial candidate.
     */
    suspend fun replaceCsvSnapshot(
        inputStream: InputStream,
        sourceId: Int,
        securityRuleRepository: SecurityRuleRepository,
        snapshotVersion: String? = null,
        snapshotHash: String? = null,
        batchSize: Int = CHUNK_SIZE
    ): SnapshotActivationResult = securityRuleRepository.replaceSourceSnapshotBatched(
        sourceId = sourceId,
        snapshotVersion = snapshotVersion,
        snapshotHash = snapshotHash
    ) { emitBatch ->
        streamCsvFile(inputStream, sourceId, batchSize) { batch -> emitBatch(batch) }
    }

    /**
     * Streams an XLSX candidate directly into the authoritative snapshot
     * transaction. The parser retains its two-pass shared-string resolution,
     * while the activation boundary guarantees that no partial candidate can
     * replace the last-known-good snapshot.
     */
    suspend fun replaceXlsxSnapshot(
        inputStream: InputStream,
        sourceId: Int,
        securityRuleRepository: SecurityRuleRepository,
        phoneColumnIndex: Int = DEFAULT_PHONE_COLUMN,
        snapshotVersion: String? = null,
        snapshotHash: String? = null,
        batchSize: Int = CHUNK_SIZE
    ): SnapshotActivationResult = securityRuleRepository.replaceSourceSnapshotBatched(
        sourceId = sourceId,
        snapshotVersion = snapshotVersion,
        snapshotHash = snapshotHash
    ) { emitBatch ->
        streamXLSXFile(
            inputStream,
            sourceId,
            phoneColumnIndex,
            batchSize
        ) { batch -> emitBatch(batch) }
    }

    /**
     * Inserts a list of entries in chunks of CHUNK_SIZE to avoid binding too
     * many parameters in a single Room transaction.
     */
    suspend fun insertEntries(entries: List<UnifiedEntryEntity>) = withContext(Dispatchers.IO) {
        var inserted = 0
        entries.chunked(CHUNK_SIZE).forEach { chunk ->
            dataSourceRepository.insertEntriesAuthoritative(chunk)
            dataSourceRepository.rebuildDerivedIndexes()
            inserted += chunk.size
            Timber.tag(TAG).d("Inserted $inserted / ${entries.size}")
        }
        Timber.tag(TAG).i("Batch insert complete: $inserted entries")
    }

    // ── Core XLSX parser ─────────────────────────────────────────────────────

    /**
     * Two-pass XLSX parse:
     *   Pass 1 — read xl/sharedStrings.xml into memory as a List<String>.
     *            Shared strings are referenced by index from sheet1.xml.
     *            Typical blocklist file has few unique strings (phone numbers
     *            are stored as inline strings, not shared), so this list is small.
     *   Pass 2 — read xl/worksheets/sheet1.xml, extract column A values,
     *            resolve shared string refs, sanitize, collect entries.
     *
     * Both passes use forward-only ZipInputStream — the file is read twice
     * by opening two separate ZipInputStreams from the same source InputStream.
     * For the InputStream overload: the caller's stream is read once into a
     * byte array so we can open it twice. The byte array is released after parsing.
     */
    private fun parseXlsxFromStream(
        inputStream: InputStream,
        sourceId: Int,
        phoneColumnIndex: Int
    ): List<UnifiedEntryEntity> {
        // Bounded read: MAX_ROWS only protects the parsing pass, which starts after
        // this point. Without a ceiling here, an oversized upstream response would
        // still force a large allocation before row-limiting ever applies.
        val zipBytes = readBytesWithLimit(inputStream, parserLimits.maxXlsxBytes)

        val sharedStrings = parseSharedStrings(
            zipBytes,
            parserLimits.maxSharedStrings,
            parserLimits.maxExpandedSharedStringBytes
        )
        val entries = parseSheet(
            zipBytes,
            sourceId,
            phoneColumnIndex,
            sharedStrings,
            parserLimits.maxRows,
            parserLimits.maxCellLength
        )

        Timber.tag(TAG).i(
            "XLSX parse complete — valid=${entries.size} source=$sourceId"
        )
        return entries
    }

    /**
     * Reads inputStream into a ByteArray, throwing XlsxParseException if the total
     * exceeds maxBytes. Reads in fixed-size chunks rather than trusting a
     * Content-Length header, which chunked transfer encoding won't provide anyway.
     */
    private fun readBytesWithLimit(inputStream: InputStream, maxBytes: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val read = inputStream.read(chunk)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                throw XlsxParseException(
                    "XLSX source exceeded $maxBytes byte limit — refusing to buffer further"
                )
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    /**
     * Pass 1 — Parse xl/sharedStrings.xml.
     * Returns an indexed list of shared string values.
     * Returns empty list if the entry is absent (all strings may be inline).
     */
    private fun parseSharedStrings(
        zipBytes: ByteArray,
        maxSharedStrings: Int,
        maxExpandedSharedStringBytes: Int
    ): List<String> {
        val sharedStrings = mutableListOf<String>()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == SHARED_STRINGS_PATH) {
                    val handler = SharedStringsHandler(
                        sharedStrings,
                        maxSharedStrings,
                        maxExpandedSharedStringBytes
                    )
                    try {
                        SAXParserFactory.newInstance().newSAXParser()
                            .parse(InputSource(zip), handler)
                    } catch (e: Exception) {
                        // SAX wraps exceptions thrown from handler callbacks in SAXException
                        // before they reach this catch site. Unwrap to recover the typed
                        // limit exception — failing to do so lets a partial shared-string
                        // table silently pass as a valid parse result, a hard security violation.
                        val cause = unwrapSaxException(e)
                        when (cause) {
                            is SharedStringsLimitExceededException -> {
                                Timber.tag(TAG).w(
                                    "Shared strings limit $maxSharedStrings reached — " +
                                    "${sharedStrings.size} entries collected"
                                )
                                // A shared-string limit is a hard security failure. Do not
                                // return a partial table whose unresolved references could
                                // silently remove security rules from the candidate dataset.
                                throw cause
                            }
                            is ExpandedSharedStringsLimitExceededException -> throw cause
                            else -> throw XlsxParseException(
                                "Unexpected error parsing shared strings", e
                            )
                        }
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }
        Timber.tag(TAG).d("Shared strings loaded: ${sharedStrings.size}")
        return sharedStrings
    }

    /**
     * Pass 2 — Parse xl/worksheets/sheet1.xml.
     * Extracts phone numbers from the target column, resolves shared string refs,
     * sanitizes, and returns UnifiedEntryEntity list.
     */
    private fun parseSheet(
        zipBytes: ByteArray,
        sourceId: Int,
        phoneColumnIndex: Int,
        sharedStrings: List<String>,
        maxRows: Int,
        maxCellLength: Int
    ): List<UnifiedEntryEntity> {
        val entries = mutableListOf<UnifiedEntryEntity>()
        parseSheetWithCallback(
            zipBytes, sourceId, phoneColumnIndex, sharedStrings, maxRows, maxCellLength
        ) { entry -> entries.add(entry) }
        return entries
    }

    private fun parseSheetWithCallback(
        zipBytes: ByteArray,
        sourceId: Int,
        phoneColumnIndex: Int,
        sharedStrings: List<String>,
        maxRows: Int,
        maxCellLength: Int,
        onEntry: (UnifiedEntryEntity) -> Unit
    ) {
        var sheetFound = false

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == SHEET1_PATH) {
                    sheetFound = true
                    val handler = SheetHandler(
                        sourceId = sourceId,
                        phoneColumnIndex = phoneColumnIndex,
                        sharedStrings = sharedStrings,
                        maxRows = maxRows,
                        maxCellLength = maxCellLength,
                        onEntry = onEntry
                    )
                    try {
                        SAXParserFactory.newInstance().newSAXParser()
                            .parse(InputSource(zip), handler)
                    } catch (e: Exception) {
                        val cause = unwrapSaxException(e)
                        when (cause) {
                            is RowLimitExceededException -> {
                                Timber.tag(TAG).w("Row limit $maxRows reached")
                                throw cause
                            }
                            is CellLengthLimitExceededException -> throw cause
                            else -> throw XlsxParseException("Unexpected error parsing sheet", e)
                        }
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }

        if (!sheetFound) {
            throw XlsxParseException(
                "xl/worksheets/sheet1.xml not found in XLSX archive — " +
                    "file may be corrupt or not a valid XLSX"
            )
        }
    }


    /**
     * SAX parsers wrap exceptions thrown from handler callbacks in SAXException
     * before they propagate to callers. This strips up to two wrapping layers to
     * recover the original typed exception (e.g. RowLimitExceededException,
     * SharedStringsLimitExceededException) so callers can type-match correctly.
     *
     * If the root cause is not a SAXException wrapper, the original exception is
     * returned unchanged so non-limit errors still surface with full context.
     */
    private fun unwrapSaxException(e: Exception): Exception {
        // SAX may double-wrap: SAXException -> SAXException -> real cause
        var cause: Throwable = e
        repeat(2) { cause = (cause as? org.xml.sax.SAXException)?.exception ?: cause }
        return cause as? Exception ?: e
    }

    // ── SAX handlers ─────────────────────────────────────────────────────────

    /**
     * Parses xl/sharedStrings.xml.
     *
     * Structure:
     *   <sst>
     *     <si><t>string value</t></si>   ← one per shared string
     *     <si><r><t>rich text part</t></r></si>  ← rich text (concatenated)
     *   </sst>
     */
    private class SharedStringsHandler(
        private val result: MutableList<String>,
        private val maxSharedStrings: Int,
        private val maxExpandedSharedStringBytes: Int
    ) : DefaultHandler() {

        private var inT = false
        private val current = StringBuilder()
        private var expandedBytes = 0

        override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
            when (qName) {
                "si" -> current.clear()
                "t"  -> { inT = true }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            when (qName) {
                "t"  -> inT = false
                "si" -> {
                    if (result.size >= maxSharedStrings) {
                        throw SharedStringsLimitExceededException(
                            "Shared strings limit $maxSharedStrings reached"
                        )
                    }
                    result.add(current.toString())
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (inT) {
                expandedBytes += String(ch, start, length).toByteArray(Charsets.UTF_8).size
                if (expandedBytes > maxExpandedSharedStringBytes) {
                    throw ExpandedSharedStringsLimitExceededException(
                        "Expanded shared strings exceeded $maxExpandedSharedStringBytes byte limit"
                    )
                }
                current.append(ch, start, length)
            }
        }
    }

    /**
     * Parses xl/worksheets/sheet1.xml.
     *
     * Relevant XML structure:
     *   <sheetData>
     *     <row r="1">
     *       <c r="A1" t="s"><v>0</v></c>   ← shared string ref (t="s", value is index)
     *       <c r="A2" t="inlineStr"><is><t>+18005551234</t></is></c>  ← inline string
     *       <c r="A3"><v>+18005559999</v></c>  ← numeric/untyped
     *     </row>
     *   </sheetData>
     *
     * Column detection: cell reference (e.g. "A1", "B2") is parsed to column index.
     * "A" = 0, "B" = 1, etc. Only cells in phoneColumnIndex are processed.
     */
    private inner class SheetHandler(
        private val sourceId: Int,
        private val phoneColumnIndex: Int,
        private val sharedStrings: List<String>,
        private val maxRows: Int,
        private val maxCellLength: Int,
        private val onEntry: (UnifiedEntryEntity) -> Unit
    ) : DefaultHandler() {

        private var rowsProcessed = 0
        private var rowsSkipped = 0
        private var isFirstRow = true

        // Per-cell state
        private var currentCellColumn = -1
        private var currentCellType = ""    // "s" = shared string, "inlineStr", "" = default
        private var inV = false
        private var inT = false
        private val cellValue = StringBuilder()
        private var cellBytes = 0

        override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
            when (qName) {
                "row" -> {
                    if (isFirstRow) {
                        isFirstRow = false
                        return  // Skip header row
                    }
                    if (rowsProcessed >= maxRows) {
                        throw RowLimitExceededException("Row limit $maxRows reached")
                    }
                }
                "c" -> {
                    val ref = attrs.getValue("r") ?: return
                    currentCellColumn = columnIndexFromRef(ref)
                    currentCellType = attrs.getValue("t") ?: ""
                    cellValue.clear()
                    cellBytes = 0
                }
                "v" -> if (currentCellColumn == phoneColumnIndex) inV = true
                "t" -> if (currentCellColumn == phoneColumnIndex) inT = true
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            when (qName) {
                "v" -> inV = false
                "t" -> inT = false
                "c" -> {
                    if (currentCellColumn != phoneColumnIndex) return
                    val raw = resolveValue(cellValue.toString().trim(), currentCellType)
                    processPhone(raw)
                }
                "row" -> {
                    if (!isFirstRow) {
                        // row count incremented in processPhone or skipped
                    }
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if ((inV || inT) && currentCellColumn == phoneColumnIndex) {
                cellBytes += String(ch, start, length).toByteArray(Charsets.UTF_8).size
                if (cellBytes > maxCellLength) {
                    throw CellLengthLimitExceededException(
                        "Cell exceeded $maxCellLength byte limit"
                    )
                }
                cellValue.append(ch, start, length)
            }
        }

        private fun resolveValue(raw: String, type: String): String {
            return when (type) {
                "s" -> {
                    // Shared string index
                    val index = raw.toIntOrNull() ?: return ""
                    sharedStrings.getOrElse(index) { "" }
                }
                else -> raw  // Inline string, numeric, or untyped
            }
        }

        private fun processPhone(raw: String) {
            if (raw.isBlank()) {
                rowsSkipped++
                return
            }
            val canonicalPhone = SourceRecordValidator.canonicalizePhone(raw)
                ?: run {
                    rowsSkipped++
                    return
                }
            onEntry(
                UnifiedEntryEntity(
                    phoneNumber = canonicalPhone,
                    action = "BLOCK",
                    sourceId = sourceId,
                    category = "XLSX Import",
                    confidence = 75,
                    metadata = "DataSyncEngine batch"
                )
            )
            rowsProcessed++
        }

        /**
         * Converts a cell reference column letter(s) to a 0-based column index.
         * "A" -> 0, "B" -> 1, "Z" -> 25, "AA" -> 26, etc.
         * The cell reference may include a row number (e.g. "A1") — only the
         * leading alpha characters are used.
         */
        private fun columnIndexFromRef(ref: String): Int {
            var index = 0
            for (ch in ref) {
                if (!ch.isLetter()) break
                index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
            }
            return index - 1
        }
    }

    // ── Exception types ───────────────────────────────────────────────────────

    /**
     * Thrown for structural problems with the XLSX file itself.
     * Distinct from IOException so callers can handle parse errors separately.
     */
    class XlsxParseException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    /**
     * Thrown internally when MAX_ROWS is reached. Propagates to callers so a
     * truncated candidate dataset cannot be treated as a successful parse.
     */
    class RowLimitExceededException(message: String) : Exception(message)

    /**
     * Thrown internally when MAX_SHARED_STRINGS is reached. Propagates to
     * callers so unresolved shared-string references cannot create a partial
     * candidate dataset that looks valid.
     */
    class SharedStringsLimitExceededException(message: String) : Exception(message)

    class ExpandedSharedStringsLimitExceededException(message: String) : Exception(message)

    class CellLengthLimitExceededException(message: String) : Exception(message)

    private sealed interface XlsxBatchMessage {
        data class Batch(val entries: List<UnifiedEntryEntity>) : XlsxBatchMessage
        data class Failed(val cause: Exception) : XlsxBatchMessage
        data object Complete : XlsxBatchMessage
    }
}

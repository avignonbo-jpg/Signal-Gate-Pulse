package com.signalgate.pulse.logic

import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.data.security.SnapshotSanityValidator
import com.signalgate.pulse.data.security.SourceRecordValidator
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SyncHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * ReliableSourceManager — Phase 3.1: verified production endpoints, resilience.
 *
 * FTC endpoint decision:
 *   The FTC publishes DNC complaint numbers via a REST API at api.ftc.gov,
 *   which requires an api.data.gov-issued API key. Embedding that key in the
 *   app (even via BuildConfig, encrypted resources, R8 obfuscation, or
 *   AndroidKeyStore) does not make it a secret — an attacker who controls the
 *   app's runtime can observe any credential the app itself must use. It also
 *   meant every install shared one hardcoded key's rate-limit bucket, which
 *   would have become a real scaling problem well before any secrecy concern
 *   mattered.
 *
 *   Fix: a scheduled GitHub Actions workflow in a separate, dedicated repo
 *   (signalgate-dnc-mirror) holds the real FTC_API_KEY as a repo secret,
 *   fetches and paginates the FTC API server-side, validates the result
 *   (refuses to publish if the count craters vs. the last snapshot — see that
 *   repo's sync_ftc_dnc.py), and publishes one flat JSON file to the
 *   dnc-mirror-pulse branch. This app fetches that published file over a
 *   plain, unauthenticated HTTPS GET — no credential ships in the APK, full
 *   stop, and the FTC's own rate limit is decoupled entirely from user count.
 *
 *   Field used: "phone_number" — the E.164-formatted number in each complaint
 *   record. Filtering happens server-side in the mirror, but this app routes every
 *   received value through SourceRecordValidator regardless —
 *   never trust an external source blindly, even one you operate yourself.
 *
 *   See: https://github.com/avignonbo-jpg/signalgate-dnc-mirror
 *
 * FCC endpoint decision:
 *   opendata.fcc.gov dataset vakf-fz8e is the FCC's Informal Complaints dataset,
 *   exported via the Socrata Open Data API. This contains consumer-reported
 *   phone numbers but is not a curated blocklist — treat as medium-confidence
 *   signal (confidence = 70), not a hard block source. The primary URL is the
 *   Socrata DOWNLOAD endpoint; the fallback queries the Socrata API directly
 *   with a row limit. Neither was independently verified against live FCC data
 *   this session — confirm both URLs resolve before shipping.
 *
 * Fallback strategy:
 *   Each source has a fallback URL list. On primary failure, each fallback is
 *   tried in order. If all fail, the sync for that source fails gracefully —
 *   other sources continue, and the failure is recorded in SyncHistory.
 */
class ReliableSourceManager(
    private val dataSourceRepository: DataSourceRepository,
    private val securityRuleRepository: SecurityRuleRepository,
    private val syncHistoryRepository: SyncHistoryRepository,
    private val secureCsvParser: SecureCsvParser,
    private val snapshotSanityValidator: SnapshotSanityValidator
) {

    companion object {
        private const val TAG = "ReliableSourceManager"
        private const val CONNECT_TIMEOUT_SEC = 10L
        private const val READ_TIMEOUT_SEC = 60L
        private const val MAX_ENTRIES_PER_SOURCE = 50_000
        private const val MIN_NUMBER_LENGTH = 10
        private const val MAX_NUMBER_LENGTH = 15

        private const val FTC_API_BASE  =
            "https://raw.githubusercontent.com/avignonbo-jpg/signalgate-dnc-mirror/dnc-mirror-pulse/dnc-numbers.json"

        private const val FCC_PRIMARY_URL  =
            "https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?accessType=DOWNLOAD"
        private const val FCC_FALLBACK_URL =
            "https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?\$limit=50000"

        val SOURCES = listOf(
            FederalSource(
                name         = "FTC Do Not Call Registry",
                primaryUrl   = FTC_API_BASE,
                // DIR-003 (2026-08-09): liveness of this fallback is unverified and may
                // already be dead — the FTC is known to embed a publish date into the
                // real filename, which a static path like this can't track. Left as-is
                // deliberately, not by oversight: since the mirror architecture above
                // means this app no longer talks to FTC directly at all, the real
                // single point of failure is raw.githubusercontent.com availability,
                // not ftc.gov — and this fallback protects against neither the old
                // failure mode (may be dead) nor the new one (irrelevant to a GitHub
                // outage) with any real confidence. A fallback that actually covers a
                // mirror outage needs genuine infrastructure redundancy, which is
                // disproportionate to a rare, self-limiting failure (FCC continues
                // independently; the failure is logged to SyncHistory, not silent).
                // Revisit only if a real incident shows this gap actually mattering.
                fallbackUrls = listOf(
                    "https://www.ftc.gov/system/files/ftc_gov/DNC_Complaint_Numbers.csv"
                ),
                sourceType   = "FTC",
                priority     = 90,
                strategy     = FetchStrategy.FTC_REST_API
            ),
            FederalSource(
                name         = "FCC Consumer Complaints",
                primaryUrl   = FCC_PRIMARY_URL,
                fallbackUrls = listOf(FCC_FALLBACK_URL),
                sourceType   = "FCC",
                priority     = 85,
                strategy     = FetchStrategy.CSV
            )
        )
    }

    enum class FetchStrategy { FTC_REST_API, CSV }

    data class FederalSource(
        val name: String,
        val primaryUrl: String,
        val fallbackUrls: List<String>,
        val sourceType: String,
        val priority: Int,
        val strategy: FetchStrategy
    )

    data class SyncResult(
        val sourceName: String,
        val entriesAdded: Int,
        val success: Boolean,
        val errorMessage: String? = null
    )

    private data class FetchedSnapshot(
        val numbers: List<String>,
        val contentType: String?,
        val charset: String?,
        val byteCount: Long,
        val recordCount: Int,
        val duplicateRecordCount: Int,
        val maxObservedFieldLength: Int,
        val malformedRecordCount: Int,
        val fetchedAt: Long,
        val expectedContentTypes: Set<String>
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    suspend fun syncAllFederalSources(): List<SyncResult> = withContext(Dispatchers.IO) {
        SOURCES.map { syncSource(it) }
    }

    suspend fun syncSource(sourceId: Int): SyncResult = withContext(Dispatchers.IO) {
        val source = dataSourceRepository.getSourceById(sourceId)
            ?: return@withContext SyncResult("source:$sourceId", 0, false, "Source not found")
        val federalSource = SOURCES.firstOrNull { it.sourceType == source.type && it.name == source.name }
            ?: return@withContext SyncResult(source.name, 0, false, "Source is not a managed federal source")
        syncSource(federalSource, sourceId)
    }

    private suspend fun syncSource(source: FederalSource, knownSourceId: Int? = null): SyncResult {
        val syncStartMs = System.currentTimeMillis()
        Timber.tag(TAG).i("Starting sync: ${source.name} (strategy=${source.strategy})")
        return try {
            val sourceId = knownSourceId ?: ensureSourceRow(source)
            val fetchStartMs = System.currentTimeMillis()
            val snapshot = fetchWithFallback(source)
            val fetchDurationMs = System.currentTimeMillis() - fetchStartMs
            Timber.tag(TAG).i(
                "Fetched ${source.name}: numbers=${snapshot.numbers.size}, duration_ms=$fetchDurationMs"
            )
            val previousAcceptedCount = dataSourceRepository.getEntryCountBySourceId(sourceId)
            when (val sanity = snapshotSanityValidator.validate(
                SnapshotSanityValidator.Candidate(
                    contentType = snapshot.contentType,
                    charset = snapshot.charset,
                    byteCount = snapshot.byteCount,
                    recordCount = snapshot.recordCount,
                    acceptedRecordCount = snapshot.numbers.size,
                    duplicateRecordCount = snapshot.duplicateRecordCount,
                    maxObservedFieldLength = snapshot.maxObservedFieldLength,
                    malformedRecordCount = snapshot.malformedRecordCount,
                    fetchedAt = snapshot.fetchedAt,
                    previousAcceptedRecordCount = previousAcceptedCount,
                    expectedContentTypes = snapshot.expectedContentTypes
                )
            )) {
                SnapshotSanityValidator.Result.Accepted -> Unit
                is SnapshotSanityValidator.Result.Rejected ->
                    throw IllegalArgumentException("Snapshot rejected: ${sanity.reason}")
            }

            val entries = snapshot.numbers.map { number ->
                UnifiedEntryEntity(
                    phoneNumber = number,
                    action      = "BLOCK",
                    sourceId    = sourceId,
                    category    = source.sourceType,
                    confidence  = if (source.strategy == FetchStrategy.FTC_REST_API) 85 else 70,
                    metadata    = source.name
                )
            }

            val activation = securityRuleRepository.replaceSourceSnapshot(sourceId, entries)
            val inserted = when (activation) {
                SnapshotActivationResult.Accepted -> entries.size
                is SnapshotActivationResult.Failed -> throw activation.cause
            }
            Timber.tag(TAG).i(
                "Accepted snapshot for ${source.name}: entries=$inserted"
            )

            val totalDurationMs = System.currentTimeMillis() - syncStartMs
            Timber.tag(TAG).i(
                "Sync complete: ${source.name} — $inserted entries, total_duration_ms=$totalDurationMs"
            )
            SyncResult(source.name, inserted, true)
        } catch (e: Exception) {
            val totalDurationMs = System.currentTimeMillis() - syncStartMs
            Timber.tag(TAG).e(
                e,
                "Sync failed: ${source.name}, total_duration_ms=$totalDurationMs"
            )
            SyncResult(source.name, 0, false, e.message)
        }
    }

    private fun fetchWithFallback(source: FederalSource): FetchedSnapshot {
        val urls = listOf(source.primaryUrl) + source.fallbackUrls
        var lastError: Exception? = null
        for (url in urls) {
            try {
                val snapshot = when (source.strategy) {
                    FetchStrategy.FTC_REST_API -> {
                        if (url == FTC_API_BASE) fetchFtcApiSnapshot()
                        else fetchCsvSnapshot(url, source.name)
                    }
                    FetchStrategy.CSV -> fetchCsvSnapshot(url, source.name)
                }
                if (snapshot.numbers.isNotEmpty()) {
                    Timber.tag(TAG).i("Fetched ${snapshot.numbers.size} numbers from $url")
                    return snapshot
                }
                Timber.tag(TAG).w("Empty response from $url — trying next fallback")
            } catch (e: Exception) {
                Timber.tag(TAG).w("Failed to fetch from $url: ${e.message} — trying next")
                lastError = e
            }
        }
        throw lastError ?: Exception("All URLs failed for ${source.name} with empty responses")
    }

    /**
     * Fetches the bounded FTC mirror candidate and records all sanity metadata
     * before the application boundary decides whether it may be activated.
     */
    private fun fetchFtcApiSnapshot(): FetchedSnapshot {
        val body = fetchRawBody(FTC_API_BASE, "FTC DNC mirror")
        val json = JSONObject(body.text)
        val dataArray = json.optJSONArray("phone_numbers")
            ?: throw IllegalArgumentException("FTC mirror missing phone_numbers array")
        val numbers = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        var malformed = 0
        var duplicates = 0
        var maxFieldLength = 0
        for (i in 0 until dataArray.length()) {
            val rawNumber = dataArray.optString(i, "").trim()
            maxFieldLength = maxOf(maxFieldLength, rawNumber.length)
            val canonicalNumber = SourceRecordValidator.canonicalizePhone(rawNumber)
            if (canonicalNumber == null ||
                canonicalNumber.length !in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH
            ) {
                malformed++
            } else if (!seen.add(canonicalNumber)) {
                duplicates++
            } else if (numbers.size < MAX_ENTRIES_PER_SOURCE) {
                numbers.add(canonicalNumber)
            }
        }
        return FetchedSnapshot(
            numbers = numbers,
            contentType = body.contentType,
            charset = body.charset,
            byteCount = body.byteCount,
            recordCount = dataArray.length(),
            duplicateRecordCount = duplicates,
            maxObservedFieldLength = maxFieldLength,
            malformedRecordCount = malformed,
            fetchedAt = body.fetchedAt,
            expectedContentTypes = setOf("application/json")
        )
    }

    /**
     * Streams CSV through the bounded raw parser, then validates/canonicalizes
     * each field and retains metadata for the snapshot sanity boundary.
     */
    private fun fetchCsvSnapshot(url: String, label: String): FetchedSnapshot {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SignalGate-Pulse/1.0")
            .header("Accept", "text/csv, application/csv, */*")
            .build()
        val numbers = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        var recordCount = 0
        var malformed = 0
        var duplicates = 0
        var maxFieldLength = 0
        val fetchedAt = System.currentTimeMillis()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code} fetching $label")
            val contentType = response.header("Content-Type")
            val charset = charsetFromContentType(contentType)
            if (!charset.equals("UTF-8", ignoreCase = true)) {
                throw IllegalArgumentException("Unsupported source encoding")
            }
            val bodyStream = response.body?.byteStream() ?: throw Exception("Empty body from $label")
            val stream = CountingInputStream(bodyStream)
            secureCsvParser.streamRows(stream) { rawNumber ->
                val normalizedRaw = rawNumber.trim()
                if (recordCount == 0 && normalizedRaw.lowercase() in setOf("phone", "phone_number", "number")) {
                    return@streamRows
                }
                recordCount++
                maxFieldLength = maxOf(maxFieldLength, normalizedRaw.length)
                val canonicalNumber = SourceRecordValidator.canonicalizePhone(normalizedRaw)
                if (canonicalNumber == null ||
                    canonicalNumber.length !in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH
                ) {
                    malformed++
                } else if (!seen.add(canonicalNumber)) {
                    duplicates++
                } else if (numbers.size < MAX_ENTRIES_PER_SOURCE) {
                    numbers.add(canonicalNumber)
                }
            }
            return FetchedSnapshot(
                numbers = numbers,
                contentType = contentType,
                charset = charset,
                byteCount = stream.count,
                recordCount = recordCount,
                duplicateRecordCount = duplicates,
                maxObservedFieldLength = maxFieldLength,
                malformedRecordCount = malformed,
                fetchedAt = fetchedAt,
                expectedContentTypes = setOf("text/csv", "application/csv")
            )
        }
    }

    private class CountingInputStream(
        input: java.io.InputStream
    ) : java.io.FilterInputStream(input) {
        var count: Long = 0
            private set

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) count++
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val read = super.read(buffer, offset, length)
            if (read > 0) count += read
            return read
        }
    }

    private data class RawBody(
        val text: String,
        val contentType: String?,
        val charset: String?,
        val byteCount: Long,
        val fetchedAt: Long
    )

    private fun fetchRawBody(url: String, label: String): RawBody {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SignalGate-Pulse/1.0")
            .header("Accept", "application/json, text/csv, */*")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code} fetching $label")
            val body = response.body ?: throw Exception("Empty body from $label")
            val bytes = body.byteStream().use { readBoundedBody(it, SnapshotSanityValidator.Limits().maxBytes) }
            return RawBody(
                text = bytes.toString(Charsets.UTF_8),
                contentType = response.header("Content-Type"),
                charset = charsetFromContentType(response.header("Content-Type")),
                byteCount = bytes.size.toLong(),
                fetchedAt = System.currentTimeMillis()
            )
        }
    }

    private fun readBoundedBody(input: java.io.InputStream, maxBytes: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw IllegalArgumentException("Snapshot byte limit exceeded")
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    private fun charsetFromContentType(contentType: String?): String? =
        contentType?.split(';')
            ?.drop(1)
            ?.firstOrNull { it.trim().startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim()
            ?.trim('"')
            ?: "UTF-8"

    private suspend fun ensureSourceRow(source: FederalSource): Int {
        val existing = dataSourceRepository.getSourceByName(source.name)
        if (existing != null) return existing.id
        val newId = dataSourceRepository.insertSource(
            SourceEntity(
                name      = source.name,
                type      = source.sourceType,
                pathOrUrl = source.primaryUrl,
                isEnabled = true,
                priority  = source.priority
            )
        )
        return newId.toInt()
    }

    fun getSourceInfo(): List<FederalSource> = SOURCES
}

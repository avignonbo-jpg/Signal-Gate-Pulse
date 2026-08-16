package com.signalgate.pulse.logic

import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.data.security.SecureCsvParser
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
 *   record. Filtering/sanitization already happens server-side in the mirror,
 *   but this app re-sanitizes and re-validates length on receipt regardless —
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
    private val secureCsvParser: SecureCsvParser
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
            val numbers = fetchWithFallback(source)
            val fetchDurationMs = System.currentTimeMillis() - fetchStartMs
            Timber.tag(TAG).i(
                "Fetched ${source.name}: numbers=${numbers.size}, duration_ms=$fetchDurationMs"
            )

            val entries = numbers.map { number ->
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

    private fun fetchWithFallback(source: FederalSource): List<String> {
        val urls = listOf(source.primaryUrl) + source.fallbackUrls
        var lastError: Exception? = null
        for (url in urls) {
            try {
                val numbers = when (source.strategy) {
                    FetchStrategy.FTC_REST_API -> {
                        if (url == FTC_API_BASE) fetchFtcApiNumbers()
                        else fetchCsvNumbers(url, source.name)
                    }
                    FetchStrategy.CSV -> fetchCsvNumbers(url, source.name)
                }
                if (numbers.isNotEmpty()) {
                    Timber.tag(TAG).i("Fetched ${numbers.size} numbers from $url")
                    return numbers
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
     * Fetches the pre-aggregated snapshot published by signalgate-dnc-mirror
     * (see the class doc comment above). One flat file, no pagination needed —
     * the mirror already did that server-side. Still re-sanitizes, re-validates
     * length, AND re-enforces MAX_ENTRIES_PER_SOURCE here — this app never
     * trusts an external source blindly, even a mirror it operates itself.
     * The mirror's cumulative merge only ever grows; this cap is what stops
     * that growth from silently exceeding what the rest of this class assumes
     * as a hard per-source ceiling.
     */
    private fun fetchFtcApiNumbers(): List<String> {
        val body = fetchRawBody(FTC_API_BASE, "FTC DNC mirror")
        val json = JSONObject(body)
        val dataArray = json.optJSONArray("phone_numbers") ?: return emptyList()
        val numbers = mutableListOf<String>()
        for (i in 0 until dataArray.length()) {
            if (numbers.size >= MAX_ENTRIES_PER_SOURCE) break
            val rawNumber = dataArray.optString(i, "").trim()
            val sanitized = SanitizationEngine.sanitizePhoneNumber(rawNumber)
            if (sanitized.length in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH) numbers.add(sanitized)
        }
        Timber.tag(TAG).d("FTC mirror — ${dataArray.length()} records (kept: ${numbers.size})")
        return numbers.distinct()
    }

    /**
     * Security fix (audit finding): streams the response body line-by-line through
     * SecureCsvParser instead of buffering the whole HTTP response into a JVM String
     * first (the previous parseCsvBody(fetchRawBody(...)) approach). These federal
     * endpoints are external, unauthenticated-by-us data sources — an oversized,
     * malformed, or malicious response can no longer force unbounded heap growth,
     * since SecureCsvParser reads one line at a time under a hard 2,000,000-line cap.
     *
     * MIN_NUMBER_LENGTH/MAX_NUMBER_LENGTH filtering happens here rather than inside
     * SecureCsvParser: the parser's contract is generic streaming + sanitization +
     * bloom-filter population, not this source's specific length bounds, and a stray
     * CSV header cell must not reach the DB as a bogus block entry.
     */
    private fun fetchCsvNumbers(url: String, label: String): List<String> {
        val parseStartMs = System.currentTimeMillis()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SignalGate-Pulse/1.0")
            .header("Accept", "application/json, text/csv, */*")
            .build()
        val numbers = mutableListOf<String>()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code} fetching $label")
            val stream = response.body?.byteStream() ?: throw Exception("Empty body from $label")
            secureCsvParser.streamAndPopulate(stream) { number ->
                if (numbers.size < MAX_ENTRIES_PER_SOURCE && number.length in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH) {
                    numbers.add(number)
                }
            }
        }
        val distinctNumbers = numbers.distinct()
        Timber.tag(TAG).d(
            "Parsed $label: raw_numbers=${numbers.size}, distinct_numbers=${distinctNumbers.size}, duration_ms=${System.currentTimeMillis() - parseStartMs}"
        )
        return distinctNumbers
    }

    private fun fetchRawBody(url: String, label: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SignalGate-Pulse/1.0")
            .header("Accept", "application/json, text/csv, */*")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code} fetching $label")
        return response.body?.string() ?: throw Exception("Empty body from $label")
    }

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

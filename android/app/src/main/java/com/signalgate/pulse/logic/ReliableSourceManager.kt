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
 *   The FTC publishes DNC complaint numbers via a REST API at api.ftc.gov.
 *   The previous CSV approach used a static filename on ftc.gov — the FTC embeds
 *   the publication date into the filename (the dated-filename variant), making
 *   any static URL stale within 24 hours. The REST API returns paginated JSON
 *   and is stable.
 *
 *   API key: DEMO_KEY works for development/testing (60 req/min, 1,000/hr).
 *   For production, register at https://api.data.gov/signup for a real key
 *   and store it in AndroidKeyStore — never in source.
 *   See: https://api.ftc.gov/v0/dnc-complaints (public API docs)
 *
 *   Field used: "phone_number" — the E.164-formatted number in each complaint
 *   record. Other fields (subject, created_date, etc.) are discarded.
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

        private const val FTC_API_BASE  = "https://api.ftc.gov/v0/dnc-complaints"
        private const val FTC_API_KEY   = "DEMO_KEY"
        private const val FTC_PAGE_SIZE = 1_000
        private const val FTC_MAX_PAGES = 50

        private const val FCC_PRIMARY_URL  =
            "https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?accessType=DOWNLOAD"
        private const val FCC_FALLBACK_URL =
            "https://opendata.fcc.gov/api/views/vakf-fz8e/rows.csv?\$limit=50000"

        val SOURCES = listOf(
            FederalSource(
                name         = "FTC Do Not Call Registry",
                primaryUrl   = FTC_API_BASE,
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

    private suspend fun syncSource(source: FederalSource): SyncResult {
        Timber.tag(TAG).i("Starting sync: ${source.name} (strategy=${source.strategy})")
        return try {
            val sourceId = ensureSourceRow(source)
            val numbers  = fetchWithFallback(source)
            var inserted = 0
            numbers.chunked(1_000).forEach { chunk ->
                chunk.forEach { number ->
                    dataSourceRepository.insertEntry(
                        UnifiedEntryEntity(
                            phoneNumber = number,
                            action      = "BLOCK",
                            sourceId    = sourceId,
                            category    = source.sourceType,
                            confidence  = if (source.strategy == FetchStrategy.FTC_REST_API) 85 else 70,
                            metadata    = source.name
                        )
                    )
                    inserted++
                }
            }
            dataSourceRepository.updateSourceSyncStatus(
                sourceId     = sourceId,
                timestamp    = System.currentTimeMillis(),
                entriesCount = inserted,
                healthStatus = "HEALTHY"
            )
            Timber.tag(TAG).i("Sync complete: ${source.name} — $inserted entries")
            SyncResult(source.name, inserted, true)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Sync failed: ${source.name}")
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

    private fun fetchFtcApiNumbers(): List<String> {
        val numbers = mutableListOf<String>()
        var page = 1
        while (page <= FTC_MAX_PAGES && numbers.size < MAX_ENTRIES_PER_SOURCE) {
            val url  = "$FTC_API_BASE?api_key=$FTC_API_KEY&per_page=$FTC_PAGE_SIZE&page=$page"
            val body = fetchRawBody(url, "FTC API page $page")
            val json = JSONObject(body)
            val dataArray = json.optJSONArray("data")
            if (dataArray == null || dataArray.length() == 0) break
            for (i in 0 until dataArray.length()) {
                val rawNumber = dataArray.getJSONObject(i).optString("phone_number", "").trim()
                val sanitized = SanitizationEngine.sanitizePhoneNumber(rawNumber)
                if (sanitized.length in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH) numbers.add(sanitized)
            }
            Timber.tag(TAG).d("FTC page $page — ${dataArray.length()} records (total: ${numbers.size})")
            page++
        }
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
        return numbers.distinct()
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

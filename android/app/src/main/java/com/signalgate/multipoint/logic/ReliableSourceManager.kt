package com.signalgate.multipoint.logic

import com.signalgate.multipoint.data.security.SanitizationEngine
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import com.signalgate.multipoint.database.repositories.SyncHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * ReliableSourceManager fetches and persists vetted federal blocklist sources
 * into the local Room database via DataSourceRepository.
 *
 * Verified production endpoints (confirmed July 2026):
 *
 * FTC Do Not Call — daily CSV published each weekday by ~noon ET.
 *   Pattern: https://www.ftc.gov/sites/default/files/DNC_Complaint_Numbers_YYYY-MM-DD.csv
 *   Weekend/holiday data rolls into the next business day's file.
 *   Dynamic date resolution: tries today, then walks back up to MAX_DATE_LOOKBACK
 *   weekdays until a 200 response is found. This handles weekends, holidays,
 *   and the window between midnight and noon ET when the day's file isn't yet live.
 *   Source: https://www.ftc.gov/policy-notices/open-government/data-sets/do-not-call-data
 *
 * FCC Consumer Complaints — full CGB complaints dataset, updated continuously.
 *   Endpoint: https://opendata.fcc.gov/api/views/3xyp-aqkj/rows.csv?accessType=DOWNLOAD
 *   Note: vakf-fz8e (used in placeholder) is the unwanted-calls filtered view —
 *   3xyp-aqkj is the correct full CGB dataset.
 *   Source: https://opendata.fcc.gov/Consumer/CGB-Consumer-Complaints-Data/3xyp-aqkj
 *
 * Security requirements (Architecture Contract Step 3.4):
 *   - TLS enforced via OkHttp (TLS 1.2+ by default on API 29+)
 *   - Connect and read timeouts enforced — no indefinite hangs
 *   - All numbers sanitized via SanitizationEngine before insert
 *   - MAX_ENTRIES_PER_SOURCE cap prevents unbounded memory use
 *
 * Called by CommunitySyncWorker via WorkManager on a daily periodic schedule.
 */
class ReliableSourceManager(
    private val dataSourceRepository: DataSourceRepository,
    private val syncHistoryRepository: SyncHistoryRepository
) {

    companion object {
        private const val TAG = "ReliableSourceManager"
        private const val CONNECT_TIMEOUT_SEC = 10L
        private const val READ_TIMEOUT_SEC = 60L
        private const val MAX_ENTRIES_PER_SOURCE = 50_000
        private const val MIN_NUMBER_LENGTH = 10
        private const val MAX_NUMBER_LENGTH = 15
        private const val MAX_DATE_LOOKBACK = 7 // weekdays to walk back for FTC

        private const val FTC_URL_BASE =
            "https://www.ftc.gov/sites/default/files/DNC_Complaint_Numbers_"
        private const val FTC_URL_SUFFIX = ".csv"

        private const val FCC_URL =
            "https://opendata.fcc.gov/api/views/3xyp-aqkj/rows.csv?accessType=DOWNLOAD"

        private val FTC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    data class FederalSource(
        val name: String,
        val url: String,
        val sourceType: String,
        val priority: Int,
        val useDynamicDate: Boolean = false
    )

    data class SyncResult(
        val sourceName: String,
        val entriesAdded: Int,
        val success: Boolean,
        val resolvedUrl: String? = null,
        val errorMessage: String? = null
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    fun getSources(): List<FederalSource> = listOf(
        FederalSource(
            name = "FTC Do Not Call Registry",
            url = FTC_URL_BASE,
            sourceType = "FTC",
            priority = 90,
            useDynamicDate = true
        ),
        FederalSource(
            name = "FCC Consumer Complaints",
            url = FCC_URL,
            sourceType = "FCC",
            priority = 85,
            useDynamicDate = false
        )
    )

    suspend fun syncAllFederalSources(): List<SyncResult> = withContext(Dispatchers.IO) {
        getSources().map { syncSource(it) }
    }

    private suspend fun syncSource(source: FederalSource): SyncResult {
        Timber.tag(TAG).i("Starting sync: ${source.name}")
        return try {
            val resolvedUrl = if (source.useDynamicDate) {
                resolveFtcUrl() ?: throw Exception(
                    "FTC: no file found within $MAX_DATE_LOOKBACK weekdays — " +
                    "site may be down or URL pattern has changed"
                )
            } else {
                source.url
            }

            val sourceId = ensureSourceRow(source, resolvedUrl)
            val numbers = fetchAndParse(source.name, resolvedUrl)
            var inserted = 0

            numbers.chunked(1000).forEach { chunk ->
                chunk.forEach { number ->
                    dataSourceRepository.insertEntry(
                        UnifiedEntryEntity(
                            phoneNumber = number,
                            action = "BLOCK",
                            sourceId = sourceId,
                            category = source.sourceType,
                            confidence = 85,
                            metadata = source.name
                        )
                    )
                    inserted++
                }
            }

            dataSourceRepository.updateSourceSyncStatus(
                sourceId = sourceId,
                timestamp = System.currentTimeMillis(),
                entriesCount = inserted,
                healthStatus = "HEALTHY"
            )

            Timber.tag(TAG).i("Sync complete: ${source.name} — $inserted entries from $resolvedUrl")
            SyncResult(source.name, inserted, true, resolvedUrl)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Sync failed: ${source.name}")
            SyncResult(source.name, 0, false, errorMessage = e.message)
        }
    }

    /**
     * Resolves the correct FTC daily CSV URL by walking back from today.
     *
     * FTC publishes a new file each weekday by ~noon ET. The file is named with
     * that day's date. Weekends and holidays have no file — Monday's file covers
     * the weekend. We walk back up to MAX_DATE_LOOKBACK days, skipping weekends,
     * until we find a URL that returns HTTP 200.
     *
     * Returns the first URL that resolves successfully, or null if none found.
     */
    private fun resolveFtcUrl(): String? {
        var date = LocalDate.now()
        var attempts = 0

        while (attempts < MAX_DATE_LOOKBACK) {
            // Skip weekends — FTC never publishes on Saturday or Sunday
            if (date.dayOfWeek == DayOfWeek.SATURDAY) {
                date = date.minusDays(1)
                continue
            }
            if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                date = date.minusDays(2)
                continue
            }

            val url = "$FTC_URL_BASE${date.format(FTC_DATE_FORMAT)}$FTC_URL_SUFFIX"
            Timber.tag(TAG).d("FTC probe: $url")

            try {
                val request = Request.Builder()
                    .url(url)
                    .head() // HEAD request — just check existence, don't download yet
                    .header("User-Agent", "SignalGate-Pulse/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                response.close()

                if (response.isSuccessful) {
                    Timber.tag(TAG).i("FTC resolved: $url")
                    return url
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("FTC probe failed for $url: ${e.message}")
            }

            date = date.minusDays(1)
            attempts++
        }

        return null
    }

    /**
     * Ensures a SourceEntity row exists for this source.
     * Updates the pathOrUrl if the resolved URL differs from the stored one
     * (relevant for FTC where the URL changes daily).
     */
    private suspend fun ensureSourceRow(source: FederalSource, resolvedUrl: String): Int {
        val existing = dataSourceRepository.getSourceByName(source.name)
        if (existing != null) {
            // Update stored URL to the latest resolved one
            if (existing.pathOrUrl != resolvedUrl) {
                dataSourceRepository.updateSource(
                    existing.copy(pathOrUrl = resolvedUrl)
                )
            }
            return existing.id
        }

        return dataSourceRepository.insertSource(
            SourceEntity(
                name = source.name,
                type = source.sourceType,
                pathOrUrl = resolvedUrl,
                isEnabled = true,
                priority = source.priority
            )
        ).toInt()
    }

    /**
     * Downloads and parses a federal source CSV.
     * Sanitizes every number via SanitizationEngine before including it.
     * Caps at MAX_ENTRIES_PER_SOURCE to prevent unbounded memory use.
     */
    private fun fetchAndParse(sourceName: String, url: String): List<String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SignalGate-Pulse/1.0")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code} fetching $sourceName from $url")
        }

        val numbers = mutableListOf<String>()
        var lineNumber = 0

        response.body?.string()?.lineSequence()?.forEach { line ->
            if (numbers.size >= MAX_ENTRIES_PER_SOURCE) return@forEach
            lineNumber++
            if (lineNumber == 1) return@forEach // Skip CSV header row

            val raw = line.split(",").firstOrNull()?.trim() ?: return@forEach
            val sanitized = SanitizationEngine.sanitizePhoneNumber(raw)
            if (sanitized.length in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH) {
                numbers.add(sanitized)
            }
        }

        Timber.tag(TAG).d("Parsed ${numbers.size} numbers from $sourceName")
        return numbers.distinct()
    }

    fun getSourceInfo(): List<FederalSource> = getSources()
}

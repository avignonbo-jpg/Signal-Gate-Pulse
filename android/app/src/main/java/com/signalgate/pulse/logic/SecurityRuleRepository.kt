// DO NOT let this file's package/imports revert to com.signalgate.multipoint.
// This file was added to consumer-v1 independently of the 2026-08-14/15
// pulse-package-rename merge, so it was never run through that rename script.
// A prior CI run caught it still declaring "package com.signalgate.multipoint...",
// which broke compilation for every file that depends on this one (see
// PROJECT_LEDGER.md, 2026-08-14/15 entry). If this file is ever regenerated,
// restored from a backup/snapshot, or reintroduced via a future merge, verify
// its package and every import still say com.signalgate.pulse before trusting it,
// even if it lands with no conflict markers — "no conflict" is not the same as
// "correct," as this incident showed.
package com.signalgate.pulse.logic

import androidx.room.withTransaction
import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * SecurityRuleRepository — Layer 5 (Application). The single authoritative
 * entry point for every decision-affecting mutation in the app, per
 * Architecture Contract §5.2 / INV-001 (Authoritative Security State).
 *
 * Phase 0.1 (Security Control-Plane Integrity): introduced to close the
 * divergence tracked as Known Violation §11.7. BlocklistRepository previously
 * wrote UnifiedEntryEntity rows directly via UnifiedEntryDao, bypassing the
 * Bloom-index chokepoint that DataSourceRepository.insertEntry() maintains
 * for every other write path (federal sync, CSV/XLSX import, contacts
 * allowlist). That meant a manual block/allow could silently diverge from
 * the Bloom filter's view of the world — a direct INV-001 violation.
 *
 * All manual mutation now routes through DataSourceRepository.insertEntry(),
 * which already owns the DB-write + Bloom-insert + sanitization pairing (see
 * its own class doc). SecurityRuleRepository does not duplicate that
 * pairing — it is a thin, explicit application-boundary wrapper around it,
 * so "which classes may write decision-affecting state" has exactly one
 * answer instead of two.
 *
 * Call flow (§5.2): UI → ViewModel → SecurityRuleRepository →
 * DataSourceRepository → DAO + Bloom. No repository, ViewModel, receiver, or
 * worker outside this class should call UnifiedEntryDao.insertEntry()
 * directly for a manual/user-facing rule.
 */
class SecurityRuleRepository(
    private val dataSourceRepository: DataSourceRepository,
    private val database: SignalGateDatabase,
    private val unifiedEntryDao: UnifiedEntryDao,
    private val settingRepository: SettingRepository
) {

    @Volatile
    private var cachedManualSourceId: Int? = null

    private suspend fun manualSourceId(): Int {
        cachedManualSourceId?.let { return it }
        val resolved = settingRepository.getSettingValue("manual_source_id")?.toIntOrNull()
            ?: throw IllegalStateException(
                "manual_source_id not found in settings — DatabaseInitializer.seedRequiredSources() " +
                    "must run before SecurityRuleRepository is used."
            )
        cachedManualSourceId = resolved
        return resolved
    }

    /**
     * Adds a manual block rule via the single insertEntry() chokepoint, so
     * the Bloom filter and DAO write happen together automatically.
     *
     * `reason` is passed RAW. DataSourceRepository.insertEntry() sanitizes
     * category/metadata internally via SanitizationEngine.sanitizeTextField(),
     * which is NOT idempotent (its quote-escaping doubles up on repeat
     * application). Do not pre-sanitize here — that would double-escape and
     * corrupt the stored reason text. This mirrors the exact caveat
     * BlocklistRepository used to document for its own (now-removed) direct
     * DAO write.
     */
    suspend fun addManualBlock(phoneNumber: String, reason: String = "Manual Block") {
        dataSourceRepository.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = phoneNumber,
                action = "BLOCK",
                sourceId = manualSourceId(),
                category = "Manual",
                confidence = 100,
                metadata = reason
            )
        )
    }

    /** See addManualBlock() doc — same single-chokepoint / raw-reason rules apply. */
    suspend fun addManualAllow(phoneNumber: String, reason: String = "Manual Allow") {
        dataSourceRepository.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = phoneNumber,
                action = "ALLOW",
                sourceId = manualSourceId(),
                category = "Manual",
                confidence = 100,
                metadata = reason
            )
        )
    }

    /**
     * Removes a manual rule. This is the one path that legitimately bypasses
     * DataSourceRepository: BloomFilterEngine supports insertion only, not
     * deletion (a normal Bloom filter property, not a bug — see
     * DataSourceRepository.rehydrateBloomFilters()). A deleted rule simply
     * lingers as a possible false-positive in the Bloom filter until the
     * next rehydration; that's safe because a Bloom false positive only ever
     * triggers an extra, correct Room read — it can never produce a false
     * ALLOW/BLOCK. So INV-001 (no derived-index divergence may change a
     * decision) still holds even though this write skips insertEntry().
     */
    suspend fun removeRule(phoneNumber: String) {
        unifiedEntryDao.deleteEntryByNumberAndSource(normalize(phoneNumber), manualSourceId())
    }

    suspend fun getAllUserRules(): List<UnifiedEntryEntity> =
        unifiedEntryDao.getAllBySource(manualSourceId())

    /**
     * Phase 0.4 / INV-002: replace one external source as an atomic snapshot.
     *
     * The attempt timestamp is deliberately recorded before the transaction so
     * a failed candidate is observable as attempted. The active entries and
     * accepted timestamp change only inside the Room transaction; any failure
     * rolls the replacement back and preserves the last-known-good entry set.
     * Bloom filters are rebuilt only after commit, so a failed transaction never
     * replaces the derived view of the prior active snapshot.
     */
    suspend fun replaceSourceSnapshot(
        sourceId: Int,
        entries: List<UnifiedEntryEntity>
    ): SnapshotActivationResult {
        val attemptTime = System.currentTimeMillis()
        return try {
            val updatedRows = database.sourceDao().recordSyncAttempt(sourceId, attemptTime)
            check(updatedRows == 1) {
                "Sync attempt was not recorded for source $sourceId (updatedRows=$updatedRows)"
            }
            database.withTransaction {
                unifiedEntryDao.deleteEntriesBySourceId(sourceId)
                dataSourceRepository.insertEntries(entries)
                database.sourceDao().recordSnapshotAccepted(
                    id = sourceId,
                    timestamp = attemptTime,
                    entriesCount = entries.size
                )
            }

            // A Bloom rebuild is derived state. If it cannot complete, the DB
            // remains authoritative and bloomReady stays false, forcing safe
            // Room reads rather than changing a decision.
            try {
                dataSourceRepository.rehydrateBloomFilters()
            } catch (e: Exception) {
                Timber.e(e, "Snapshot accepted but Bloom rebuild deferred for source $sourceId")
            }
            SnapshotActivationResult.Accepted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Snapshot replace failed for source $sourceId — last-known-good preserved")
            SnapshotActivationResult.Failed(e)
        }
    }

    /**
     * Mirrors DataSourceRepository's private normalizePhoneNumber() exactly,
     * so a removeRule() lookup matches whatever insertEntry() actually
     * stored. Duplicated rather than exposed from DataSourceRepository
     * because that method is private there by design — if a third caller
     * ever needs this, promote it to a shared Cross-Cutting util (§4) rather
     * than adding a second copy-paste.
     */
    private fun normalize(raw: String): String {
        val safe = SanitizationEngine.sanitizePhoneNumber(raw)
        var cleaned = safe.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("1") && cleaned.length == 11) cleaned = "+$cleaned"
        else if (!cleaned.startsWith("+")) cleaned = "+1$cleaned"
        return cleaned
    }
}

sealed interface SnapshotActivationResult {
    data object Accepted : SnapshotActivationResult
    data class Failed(val cause: Exception) : SnapshotActivationResult
}

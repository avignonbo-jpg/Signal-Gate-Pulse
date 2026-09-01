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
 * DataSourceRepository authoritative-write and derived-index boundary used by
 * other write paths. That meant a manual block/allow could silently diverge
 * from the Bloom filter's view of the world — a direct INV-001 violation.
 *
 * All manual mutation now routes through DataSourceRepository's explicit
 * authoritative-write and post-commit rebuild APIs. SecurityRuleRepository
 * does not duplicate that pairing — it is a thin, explicit application-boundary
 * wrapper around it, so "which classes may write decision-affecting state" has
 * exactly one answer instead of two.
 *
 * Call flow (§5.2): UI → ViewModel → SecurityRuleRepository →
 * DataSourceRepository → authoritative DAO write → committed-state Bloom rebuild.
 * No repository, ViewModel, receiver, or worker outside this class should call
 * UnifiedEntryDao.insertEntry() directly for a manual/user-facing rule.
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
     * Adds a manual block rule through the authoritative-write boundary, then
     * rebuilds Bloom-derived indexes after the write completes.
     *
     * `reason` is passed RAW. DataSourceRepository's authoritative insert sanitizes
     * category/metadata internally via SanitizationEngine.sanitizeTextField(),
     * which is NOT idempotent (its quote-escaping doubles up on repeat
     * application). Do not pre-sanitize here — that would double-escape and
     * corrupt the stored reason text. This mirrors the exact caveat
     * BlocklistRepository used to document for its own (now-removed) direct
     * DAO write.
     */
    suspend fun addManualBlock(phoneNumber: String, reason: String = "Manual Block") {
        dataSourceRepository.insertEntriesAuthoritative(
            listOf(
                UnifiedEntryEntity(
                    phoneNumber = phoneNumber,
                    action = "BLOCK",
                    sourceId = manualSourceId(),
                    category = "Manual",
                    confidence = 100,
                    metadata = reason
                )
            )
        )
        dataSourceRepository.rebuildDerivedIndexes()
    }

    /** See addManualBlock() doc — same authoritative-write / raw-reason rules apply. */
    suspend fun addManualAllow(phoneNumber: String, reason: String = "Manual Allow") {
        dataSourceRepository.insertEntriesAuthoritative(
            listOf(
                UnifiedEntryEntity(
                    phoneNumber = phoneNumber,
                    action = "ALLOW",
                    sourceId = manualSourceId(),
                    category = "Manual",
                    confidence = 100,
                    metadata = reason
                )
            )
        )
        dataSourceRepository.rebuildDerivedIndexes()
    }

    /**
     * Adds an allow rule imported from the contacts provider while preserving
     * the dedicated contacts source attribution. The write still passes through
     * DataSourceRepository's authoritative insert, followed by an explicit
     * post-commit derived-index rebuild.
     */
    suspend fun addContactAllow(phoneNumber: String, sourceId: Int, displayName: String) {
        dataSourceRepository.insertEntriesAuthoritative(
            listOf(
                UnifiedEntryEntity(
                    phoneNumber = phoneNumber,
                    action = "ALLOW",
                    sourceId = sourceId,
                    category = "Contact",
                    confidence = 100,
                    metadata = displayName
                )
            )
        )
        dataSourceRepository.rebuildDerivedIndexes()
    }

    /**
     * Removes a manual rule. This is the one path that legitimately bypasses
     * DataSourceRepository's insertion boundary: BloomFilterEngine supports insertion only, not
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
        entries: List<UnifiedEntryEntity>,
        metadata: SnapshotMetadata = SnapshotMetadata(acceptedRecordCount = entries.size),
        attemptTimestamp: Long? = null
    ): SnapshotActivationResult {
        val attemptTime = attemptTimestamp ?: System.currentTimeMillis()
        return try {
            if (attemptTimestamp == null) {
                val updatedRows = database.sourceDao().recordSyncAttempt(sourceId, attemptTime)
                check(updatedRows == 1) {
                    "Sync attempt was not recorded for source $sourceId (updatedRows=$updatedRows)"
                }
            }
            check(entries.isNotEmpty()) {
                "Snapshot activation rejected: candidate contains no accepted records"
            }
            database.withTransaction {
                unifiedEntryDao.deleteEntriesBySourceId(sourceId)
                dataSourceRepository.insertEntriesAuthoritative(entries)
                database.sourceDao().recordSnapshotAccepted(
                    id = sourceId,
                    timestamp = attemptTime,
                    entriesCount = metadata.acceptedRecordCount,
                    snapshotVersion = metadata.version,
                    snapshotHash = metadata.hash
                )
            }

            // This call is intentionally outside the transaction. The Room commit
            // above establishes authoritative state before any Bloom mutation.
            try {
                dataSourceRepository.rebuildDerivedIndexes()
            } catch (e: Exception) {
                Timber.e(e, "Snapshot accepted but Bloom rebuild deferred for source $sourceId")
            }
            SnapshotActivationResult.Accepted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            database.sourceDao().recordSyncFailure(
                id = sourceId,
                state = if (e.message?.startsWith("Snapshot activation rejected") == true) {
                    SourceLifecycleState.REJECTED.name
                } else {
                    SourceLifecycleState.FAILED.name
                },
                reason = e.message ?: "Snapshot activation failed",
                timestamp = System.currentTimeMillis()
            )
            Timber.e(e, "Snapshot replace failed for source $sourceId — last-known-good preserved")
            SnapshotActivationResult.Failed(e)
        }
    }

    /**
     * Replaces an external source from bounded parser batches inside one
     * authoritative Room transaction. The producer may suspend between batches,
     * but no batch is committed independently and any producer/parser failure
     * rolls back the complete candidate. Bloom is rebuilt only after commit.
     */
    suspend fun replaceSourceSnapshotBatched(
        sourceId: Int,
        snapshotVersion: String? = null,
        snapshotHash: String? = null,
        attemptTimestamp: Long? = null,
        produceBatches: suspend (suspend (List<UnifiedEntryEntity>) -> Unit) -> Unit
    ): SnapshotActivationResult {
        val attemptTime = attemptTimestamp ?: System.currentTimeMillis()
        return try {
            if (attemptTimestamp == null) {
                val updatedRows = database.sourceDao().recordSyncAttempt(sourceId, attemptTime)
                check(updatedRows == 1) {
                    "Sync attempt was not recorded for source $sourceId (updatedRows=$updatedRows)"
                }
            }
            var acceptedCount = 0
            database.withTransaction {
                unifiedEntryDao.deleteEntriesBySourceId(sourceId)
                produceBatches { batch ->
                    check(batch.isNotEmpty()) { "Snapshot activation rejected: empty batch" }
                    dataSourceRepository.insertEntriesAuthoritative(batch)
                    acceptedCount += batch.size
                }
                check(acceptedCount > 0) {
                    "Snapshot activation rejected: candidate contains no accepted records"
                }
                database.sourceDao().recordSnapshotAccepted(
                    id = sourceId,
                    timestamp = attemptTime,
                    entriesCount = acceptedCount,
                    snapshotVersion = snapshotVersion,
                    snapshotHash = snapshotHash
                )
            }
            try {
                dataSourceRepository.rebuildDerivedIndexes()
            } catch (e: Exception) {
                Timber.e(e, "Batched snapshot accepted but Bloom rebuild deferred for source $sourceId")
            }
            SnapshotActivationResult.Accepted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            database.sourceDao().recordSyncFailure(
                id = sourceId,
                state = if (e.message?.startsWith("Snapshot activation rejected") == true) {
                    SourceLifecycleState.REJECTED.name
                } else {
                    SourceLifecycleState.FAILED.name
                },
                reason = e.message ?: "Batched snapshot activation failed",
                timestamp = System.currentTimeMillis()
            )
            Timber.e(e, "Batched snapshot replace failed for source $sourceId — last-known-good preserved")
            SnapshotActivationResult.Failed(e)
        }
    }

    /** Marks a source as syncing before any network or parser work begins. */
    suspend fun beginSourceSync(sourceId: Int): Long {
        val timestamp = System.currentTimeMillis()
        val updatedRows = database.sourceDao().recordSyncAttempt(sourceId, timestamp)
        check(updatedRows == 1) {
            "Sync could not begin for source $sourceId (updatedRows=$updatedRows)"
        }
        return timestamp
    }

    /** Records a pre-activation rejection or fetch failure without touching entries. */
    suspend fun recordSourceFailure(
        sourceId: Int,
        state: SourceLifecycleState,
        reason: String
    ) {
        database.sourceDao().recordSyncFailure(
            id = sourceId,
            state = state.name,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
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

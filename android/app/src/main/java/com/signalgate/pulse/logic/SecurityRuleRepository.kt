package com.signalgate.pulse.logic

import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository

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

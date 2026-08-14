package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.UnifiedEntryEntity

/**
 * Thin facade over UnifiedEntryDao for user-managed block/allow rules.
 * Uses the MANUAL sourceId seeded by DatabaseInitializer.
 *
 * Fixed per Production-Readiness Procedure, Phase 4.2:
 * Previously constructed with a hardcoded `-1` sourceId (di/AppModule.kt),
 * with a comment deferring the real fix to "Step 2.4" of the old roadmap —
 * that step was never done. `-1` doesn't correspond to any row in `sources`,
 * so every insertEntry() call here would fail the UnifiedEntryEntity ->
 * SourceEntity foreign key constraint the moment this repository was
 * actually exercised (it never was — the only screen that would have used
 * it, BlockAllowList, was still a stub). Now resolves the real id lazily
 * from SettingRepository ("manual_source_id", written synchronously by
 * DatabaseInitializer.seedRequiredSources() before Koin bindings resolve)
 * and caches it — no runBlocking, no constructor-time dependency ordering
 * problem.
 */
class BlocklistRepository(
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
                    "must run before BlocklistRepository is used."
            )
        cachedManualSourceId = resolved
        return resolved
    }

    /**
     * Security fix (audit finding): this method writes UnifiedEntryEntity directly
     * via unifiedEntryDao — it does NOT go through DataSourceRepository.insertEntry(),
     * so it needs its own sanitization rather than relying on that chokepoint.
     * `reason` is sanitized here (not by the caller) — SanitizationEngine.sanitizeTextField()
     * is not idempotent (its quote-escaping doubles up on repeat application), so
     * callers such as BlockedNumbersViewModel must pass the raw reason and let this
     * be the single point that sanitizes it.
     */
    suspend fun addBlockRule(phoneNumber: String, reason: String = "Manual Block") {
        unifiedEntryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = normalize(phoneNumber),
                action = "BLOCK",
                sourceId = manualSourceId(),
                category = "Manual",
                confidence = 100,
                metadata = SanitizationEngine.sanitizeTextField(reason)
            )
        )
    }

    suspend fun addAllowRule(phoneNumber: String, reason: String = "Manual Allow") {
        unifiedEntryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = normalize(phoneNumber),
                action = "ALLOW",
                sourceId = manualSourceId(),
                category = "Manual",
                confidence = 100,
                metadata = SanitizationEngine.sanitizeTextField(reason)
            )
        )
    }

    suspend fun removeRule(phoneNumber: String) {
        unifiedEntryDao.deleteEntryByNumberAndSource(normalize(phoneNumber), manualSourceId())
    }

    suspend fun getAllUserRules(): List<UnifiedEntryEntity> = unifiedEntryDao.getAllBySource(manualSourceId())

    /**
     * Security fix (audit finding): previously stripped characters with a private
     * `[^0-9+]` regex and never called SanitizationEngine — a third divergent
     * sanitizer (alongside DataSourceRepository's and this class's own) that never
     * touched the canonical one. Now routes through SanitizationEngine.sanitizePhoneNumber()
     * first (strips anything outside the audited allowlist, enforces the 30-char
     * cap), then applies the same E.164 shaping as before on the sanitized result.
     */
    private fun normalize(raw: String): String {
        val safe = SanitizationEngine.sanitizePhoneNumber(raw)
        var cleaned = safe.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("1") && cleaned.length == 11) cleaned = "+$cleaned"
        else if (!cleaned.startsWith("+")) cleaned = "+1$cleaned"
        return cleaned
    }
}

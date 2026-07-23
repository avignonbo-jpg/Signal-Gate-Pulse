package com.signalgate.multipoint.database.repositories

import com.signalgate.multipoint.database.daos.UnifiedEntryDao
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity

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

    suspend fun addBlockRule(phoneNumber: String, reason: String = "Manual Block") {
        unifiedEntryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = normalize(phoneNumber),
                action = "BLOCK",
                sourceId = manualSourceId(),
                category = "Manual",
                confidence = 100,
                metadata = reason
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
                metadata = reason
            )
        )
    }

    suspend fun removeRule(phoneNumber: String) {
        unifiedEntryDao.deleteEntryByNumberAndSource(normalize(phoneNumber), manualSourceId())
    }

    suspend fun getAllUserRules(): List<UnifiedEntryEntity> = unifiedEntryDao.getAllBySource(manualSourceId())

    private fun normalize(raw: String): String {
        var cleaned = raw.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("1") && cleaned.length == 11) cleaned = "+$cleaned"
        else if (!cleaned.startsWith("+")) cleaned = "+1$cleaned"
        return cleaned
    }
}

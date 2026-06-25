package com.signalgate.multipoint.database.repositories

import com.signalgate.multipoint.database.daos.UnifiedEntryDao
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.database.entities.SourceEntity

/**
 * Thin facade over UnifiedEntryDao for user-managed block/allow rules.
 * Uses the MANUAL sourceId (seeded in DatabaseInitializer).
 */
class BlocklistRepository(
    private val unifiedEntryDao: UnifiedEntryDao,
    private val manualSourceId: Int
) {

    suspend fun addBlockRule(phoneNumber: String, reason: String = "Manual Block") {
        unifiedEntryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = normalize(phoneNumber),
                action = "BLOCK",
                sourceId = manualSourceId,
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
                sourceId = manualSourceId,
                category = "Manual",
                confidence = 100,
                metadata = reason
            )
        )
    }

    suspend fun removeRule(phoneNumber: String) {
        unifiedEntryDao.deleteEntryByNumberAndSource(phoneNumber, manualSourceId)
    }

    suspend fun getAllUserRules() = unifiedEntryDao.getAllBySource(manualSourceId)

    private fun normalize(raw: String): String {
        var cleaned = raw.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("1") && cleaned.length == 11) cleaned = "+$cleaned"
        else if (!cleaned.startsWith("+")) cleaned = "+1$cleaned"
        return cleaned
    }
}

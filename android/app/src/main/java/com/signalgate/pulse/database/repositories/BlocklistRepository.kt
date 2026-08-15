package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.logic.SecurityRuleRepository

/**
 * BlocklistRepository — DEPRECATED, PENDING MIGRATION.
 * Architecture Contract §4 (Layer 3 note) / Known Violation §11.7.
 *
 * Phase 0.1 (Security Control-Plane Integrity): this class previously wrote
 * UnifiedEntryEntity rows directly via UnifiedEntryDao, bypassing the
 * Bloom-index chokepoint DataSourceRepository.insertEntry() maintains for
 * every other write path — a confirmed INV-001 violation (§11.7). It is now
 * a thin pass-through to SecurityRuleRepository (Layer 5, §5.2), which owns
 * that chokepoint.
 *
 * Kept as a facade — rather than deleted outright — only so existing call
 * sites (BlockedNumbersViewModel, ContactsViewModel, PendingCardViewModel)
 * don't all need to change in this same commit. CallActionReceiver has
 * already been migrated directly onto SecurityRuleRepository as part of
 * closing §11.10 in the same pass. New code should depend on
 * SecurityRuleRepository directly; remaining callers of this class should be
 * migrated opportunistically (§12 Phase 0 follow-up), not left here
 * permanently — this class should not exist by the time Phase 0 closes.
 */
class BlocklistRepository(
    private val securityRuleRepository: SecurityRuleRepository
) {
    suspend fun addBlockRule(phoneNumber: String, reason: String = "Manual Block") =
        securityRuleRepository.addManualBlock(phoneNumber, reason)

    suspend fun addAllowRule(phoneNumber: String, reason: String = "Manual Allow") =
        securityRuleRepository.addManualAllow(phoneNumber, reason)

    suspend fun removeRule(phoneNumber: String) =
        securityRuleRepository.removeRule(phoneNumber)

    suspend fun getAllUserRules(): List<UnifiedEntryEntity> =
        securityRuleRepository.getAllUserRules()
}

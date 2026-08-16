package com.signalgate.pulse.logic

/**
 * Phase 0.4 application boundary for source synchronization.
 *
 * ViewModels may observe source rows and request a sync, but they must not
 * fabricate HEALTHY status from the existing entry count. ReliableSourceManager
 * owns fetching and SecurityRuleRepository owns atomic snapshot activation; this
 * use case is the single UI-facing bridge to their real outcome.
 */
class SourceSyncUseCase(
    private val reliableSourceManager: ReliableSourceManager
) {
    suspend fun syncSource(sourceId: Int): ReliableSourceManager.SyncResult =
        reliableSourceManager.syncSource(sourceId)

    suspend fun syncSources(sourceIds: List<Int>): List<ReliableSourceManager.SyncResult> =
        sourceIds.map { reliableSourceManager.syncSource(it) }

    suspend fun syncAllFederalSources(): List<ReliableSourceManager.SyncResult> =
        reliableSourceManager.syncAllFederalSources()
}

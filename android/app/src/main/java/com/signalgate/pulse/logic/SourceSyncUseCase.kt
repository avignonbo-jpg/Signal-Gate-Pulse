package com.signalgate.pulse.logic

import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import kotlinx.coroutines.flow.Flow

/**
 * Phase 0.4 application boundary for source synchronization.
 *
 * ViewModels may observe source rows and request a sync, but they must not
 * fabricate HEALTHY status from the existing entry count. ReliableSourceManager
 * owns fetching and SecurityRuleRepository owns atomic snapshot activation; this
 * use case is the single UI-facing bridge to their real outcome.
 */
class SourceSyncUseCase(
    private val reliableSourceManager: ReliableSourceManager,
    private val dataSourceRepository: DataSourceRepository
) {
    val isSyncing: Flow<Boolean> = reliableSourceManager.isSyncing

    data class BatchResult(
        val results: List<ReliableSourceManager.SyncResult>
    ) {
        val acceptedCount: Int
            get() = results.count { it.success }
        val totalCount: Int
            get() = results.size
        val success: Boolean
            get() = results.isNotEmpty() && results.all { it.success }
        val summaryLine: String
            get() = when {
                totalCount == 0 -> "Enabled federal source sync not attempted: 0/0 accepted"
                success -> "Enabled federal source sync succeeded: $acceptedCount/$totalCount accepted"
                else -> "Enabled federal source sync had failures: $acceptedCount/$totalCount accepted"
            }
    }

    companion object {
        /** Return only enabled FTC/FCC row IDs for the ViewModel's bulk-sync request. */
        fun enabledFederalSourceIds(sources: List<SourceEntity>): List<Int> =
            sources.filter { it.isEnabled && isManagedFederalSource(it) }.map { it.id }

        private fun isManagedFederalSource(source: SourceEntity): Boolean =
            ReliableSourceManager.SOURCES.any { managed ->
                managed.sourceType == source.type && managed.name == source.name
            }
    }

    suspend fun syncSource(sourceId: Int): ReliableSourceManager.SyncResult {
        val source = dataSourceRepository.getSourceById(sourceId)
            ?: return ReliableSourceManager.SyncResult(
                "source:$sourceId", 0, false, "Source not found"
            )
        if (!isManagedFederalSource(source)) {
            return ReliableSourceManager.SyncResult(
                source.name, 0, false, "Source is not a managed federal source"
            )
        }
        return reliableSourceManager.syncSource(sourceId)
    }

    /**
     * Sync enabled rows that correspond to the fixed, remotely-managed FTC/FCC
     * sources. MANUAL rows (Manual User Rules and Contacts Allow List) are local
     * data and must not be counted as failed network syncs.
     */
    suspend fun syncSources(sourceIds: List<Int>): BatchResult =
        reliableSourceManager.trackSyncing {
            BatchResult(sourceIds.map { syncSource(it) })
        }

    suspend fun syncAllFederalSources(): List<ReliableSourceManager.SyncResult> =
        reliableSourceManager.syncAllFederalSources()
}

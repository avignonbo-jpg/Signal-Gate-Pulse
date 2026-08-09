package com.signalgate.multipoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * SourcesViewModel — backs SourcesScreen (Contract §4 L7, Phase 3.3/3.4).
 *
 * Exposes real SourceEntity data from DataSourceRepository: health status,
 * enable/disable, manual "sync now", and removal.
 *
 * Removed: the "Add Source" custom CSV/URL/XLSX flow. That was a Multi-Port
 * (prosumer) capability that never belonged in the Pulse consumer flavor —
 * ReliableSourceManager doesn't read the sources table at all, it works off
 * its own hardcoded federal source list, so anything added through this flow
 * was silently inert regardless of type. Pulse's actual source model is
 * fixed: FCC (default), the community blocklist (default), and MANUAL
 * (contacts + post-call decisions) — none of which need a free-text
 * add-a-URL entry point. If this screen's real estate ends up serving
 * something else later, that's a fresh design, not a repurposed dialog.
 */
class SourcesViewModel(
    private val dataSourceRepository: DataSourceRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SourcesViewModel"
    }

    val sources: Flow<List<SourceEntity>> = dataSourceRepository.getAllSources()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Manual "sync now" for a single source. Mirrors the counting logic
     * DashboardViewModel already uses for its own sync actions, scoped here
     * to a single sourceId instead of all sources.
     */
    fun syncSource(sourceId: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val entriesCount = dataSourceRepository.getEntryCountBySourceId(sourceId)
                dataSourceRepository.updateSourceSyncStatus(
                    sourceId = sourceId,
                    timestamp = System.currentTimeMillis(),
                    entriesCount = entriesCount,
                    healthStatus = "HEALTHY"
                )
                Timber.tag(TAG).i("Source $sourceId synced: $entriesCount entries")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to sync source $sourceId")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Ported from DashboardViewModel (backed OperationalDashboard, now retired).
     * Enable/disable a source without deleting it — repository already had
     * toggleSourceEnabled(); this is just the missing UI-facing hookup.
     */
    fun toggleSourceEnabled(sourceId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                dataSourceRepository.toggleSourceEnabled(sourceId, isEnabled)
                Timber.tag(TAG).d("Source $sourceId toggled to $isEnabled")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to toggle source $sourceId")
            }
        }
    }

    /**
     * Ported from DashboardViewModel (backed OperationalDashboard, now retired).
     * Syncs every enabled source in one pass; per-row "Sync now" already covers
     * the single-source case via syncSource().
     */
    fun syncAllSources() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                sources.first().forEach { source ->
                    if (source.isEnabled) {
                        val entriesCount = dataSourceRepository.getEntryCountBySourceId(source.id)
                        dataSourceRepository.updateSourceSyncStatus(
                            sourceId = source.id,
                            timestamp = System.currentTimeMillis(),
                            entriesCount = entriesCount,
                            healthStatus = "HEALTHY"
                        )
                        Timber.tag(TAG).d("${source.name} synced: $entriesCount entries")
                    }
                }
                Timber.tag(TAG).i("All sources synced successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to sync all sources")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteSource(source: SourceEntity) {
        viewModelScope.launch {
            try {
                dataSourceRepository.deleteSource(source)
                Timber.tag(TAG).i("Source deleted: ${source.name}")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to delete source ${source.name}")
            }
        }
    }
}

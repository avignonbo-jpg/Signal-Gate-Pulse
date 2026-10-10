package com.signalgate.pulse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.ProtectedSourceDeletionException
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.SourceSyncUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/**
 * SourcesViewModel — backs SourcesScreen (Contract §4 L7, Phase 3.3/3.4).
 *
 * Exposes real SourceEntity data from DataSourceRepository: health status,
 * enable/disable, manual "sync now", and removal.
 *
 * The source model is fixed: FTC Do Not Call, FCC Consumer Complaints,
 * Manual User Rules, and Contacts Allow List. Only the two federal sources
 * are remotely synchronized. There is no free-text source-creation flow; any
 * future use for this screen's remaining space needs a separate design.
 */
class SourcesViewModel(
    private val dataSourceRepository: DataSourceRepository,
    private val sourceSyncUseCase: SourceSyncUseCase,
    private val settingRepository: SettingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SourcesViewModel"
        private const val CONTACTS_SOURCE_ID_SETTING = "contacts_source_id"
    }

    val sources: Flow<List<SourceEntity>> = dataSourceRepository.getAllSources()

    val contactsEntryCount: Flow<Int> = flow {
        val contactsSourceId = settingRepository
            .getSettingValue(CONTACTS_SOURCE_ID_SETTING)
            ?.toIntOrNull()
        if (contactsSourceId == null) {
            emit(0)
        } else {
            emitAll(dataSourceRepository.observeEntryCountBySourceId(contactsSourceId))
        }
    }

    val isSyncing: StateFlow<Boolean> = sourceSyncUseCase.isSyncing.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )

    private val _sourceActionError = MutableStateFlow<String?>(null)
    val sourceActionError: StateFlow<String?> = _sourceActionError.asStateFlow()

    fun clearSourceActionError() {
        _sourceActionError.value = null
    }

    /**
     * Manual "sync now" for a single source. The result comes from the real
     * fetch-and-atomic-activation path; this method never fabricates HEALTHY.
     */
    fun syncSource(sourceId: Int) {
        viewModelScope.launch {
            try {
                val result = sourceSyncUseCase.syncSource(sourceId)
                if (result.success) {
                    Timber.tag(TAG).i("Source $sourceId accepted: ${result.entriesAdded} entries")
                } else {
                    Timber.tag(TAG).w("Source $sourceId sync failed: ${result.errorMessage}")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to sync source $sourceId")
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
            _sourceActionError.value = null
            try {
                dataSourceRepository.toggleSourceEnabled(sourceId, isEnabled)
                Timber.tag(TAG).d("Source $sourceId toggled to $isEnabled")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to toggle source $sourceId")
                _sourceActionError.value = "Couldn't update this source. Please try again."
            }
        }
    }

    /**
     * Syncs through the same real application boundary as the worker. The
     * manager returns accepted/failed outcomes; no UI path writes HEALTHY.
     */
    fun syncAllSources() {
        viewModelScope.launch {
            try {
                val enabledFederalIds = SourceSyncUseCase.enabledFederalSourceIds(sources.first())
                val result = sourceSyncUseCase.syncSources(enabledFederalIds)
                Timber.tag(TAG).i(result.summaryLine)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to sync all sources")
            }
        }
    }

    fun deleteSource(source: SourceEntity) {
        viewModelScope.launch {
            _sourceActionError.value = null
            try {
                dataSourceRepository.deleteSource(source)
                Timber.tag(TAG).i("Source deleted: ${source.name}")
            } catch (e: ProtectedSourceDeletionException) {
                Timber.tag(TAG).w("Refused deletion of protected source ${source.name}")
                _sourceActionError.value = "This source can't be removed."
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to delete source ${source.name}")
            }
        }
    }
}

package com.signalgate.multipoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.data.security.SanitizationEngine
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * SourcesViewModel — backs SourcesScreen (Contract §4 L7, Phase 3.3/3.4).
 *
 * Exposes real SourceEntity data from DataSourceRepository and owns the
 * "Add Source" bottom sheet flow: visibility, field validation via
 * SanitizationEngine, and the insert call itself.
 *
 * This class previously did not exist even though SourcesScreen referenced
 * it — see Production-Readiness Procedure, Phase 0.1.
 */
class SourcesViewModel(
    private val dataSourceRepository: DataSourceRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SourcesViewModel"

        /** Manual "Add Source" only supports these — MANUAL type is reserved for BlocklistRepository. */
        val ALLOWED_TYPES = listOf("CSV", "URL", "XLSX")

        /** 100 is reserved for the MANUAL blocklist source (Contract §4 L3) — user-added sources stay below it. */
        private const val MAX_USER_PRIORITY = 99
    }

    val sources: Flow<List<SourceEntity>> = dataSourceRepository.getAllSources()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isAddSheetVisible = MutableStateFlow(false)
    val isAddSheetVisible: StateFlow<Boolean> = _isAddSheetVisible.asStateFlow()

    private val _addSourceError = MutableStateFlow<String?>(null)
    val addSourceError: StateFlow<String?> = _addSourceError.asStateFlow()

    fun showAddSheet() {
        _addSourceError.value = null
        _isAddSheetVisible.value = true
    }

    fun hideAddSheet() {
        _isAddSheetVisible.value = false
        _addSourceError.value = null
    }

    /**
     * Validates and inserts a new manually-added source.
     * Name and path/URL are always passed through SanitizationEngine
     * before persisting — bottom-sheet input is never trusted directly.
     */
    fun addSource(name: String, type: String, pathOrUrl: String, priority: Int) {
        val cleanName = SanitizationEngine.sanitizeTextField(name)
        val cleanPath = SanitizationEngine.sanitizeTextField(pathOrUrl)

        if (cleanName.isBlank()) {
            _addSourceError.value = "Source name is required."
            return
        }
        if (cleanPath.isBlank()) {
            _addSourceError.value = "Path or URL is required."
            return
        }
        if (type !in ALLOWED_TYPES) {
            _addSourceError.value = "Type must be CSV, URL, or XLSX."
            return
        }

        val clampedPriority = priority.coerceIn(0, MAX_USER_PRIORITY)

        viewModelScope.launch {
            try {
                dataSourceRepository.insertSource(
                    SourceEntity(
                        name = cleanName,
                        type = type,
                        pathOrUrl = cleanPath,
                        isEnabled = true,
                        priority = clampedPriority,
                        healthStatus = "UNKNOWN"
                    )
                )
                Timber.tag(TAG).i("Source added: $cleanName ($type, priority=$clampedPriority)")
                _addSourceError.value = null
                _isAddSheetVisible.value = false
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to add source: $cleanName")
                _addSourceError.value = "Could not save source — please try again."
            }
        }
    }

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

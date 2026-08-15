 package com.signalgate.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.BlocklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * BlockedNumbersViewModel — backs BlockAllowListScreen (Phase 4.2).
 *
 * Fixed per Production-Readiness Procedure, Phase 4.2: this previously read
 * through DataSourceRepository.getAllEntries() (every entry from every
 * source, filtered client-side to action == "BLOCK") and had no way to add,
 * remove, search, or view ALLOW rules. Per the procedure's own to-do this is
 * rewritten against BlocklistRepository, which is scoped to the user's own
 * MANUAL-source rules — the correct data for a screen the user edits
 * directly — and now supports both BLOCK and ALLOW entries, free-text
 * search, and add/delete.
 */
class BlockedNumbersViewModel(private val repository: BlocklistRepository) : ViewModel() {

    enum class Filter { ALL, BLOCKED, ALLOWED }

    private val _allRules = MutableStateFlow<List<UnifiedEntryEntity>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(Filter.ALL)
    val filter: StateFlow<Filter> = _filter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    private val _isAddSheetVisible = MutableStateFlow(false)
    val isAddSheetVisible: StateFlow<Boolean> = _isAddSheetVisible.asStateFlow()

    fun showAddSheet() {
        _formError.value = null
        _isAddSheetVisible.value = true
    }

    fun hideAddSheet() {
        _isAddSheetVisible.value = false
        _formError.value = null
    }

    val visibleRules: StateFlow<List<UnifiedEntryEntity>> =
        combine(_allRules, _searchQuery, _filter) { rules, query, filter ->
            rules
                .filter { rule ->
                    when (filter) {
                        Filter.ALL -> true
                        Filter.BLOCKED -> rule.action == "BLOCK"
                        Filter.ALLOWED -> rule.action == "ALLOW"
                    }
                }
                .filter { rule ->
                    query.isBlank() || rule.phoneNumber.contains(query.trim(), ignoreCase = true)
                }
                .sortedByDescending { it.createdAt }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _allRules.value = repository.getAllUserRules()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to load user block/allow rules")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: Filter) {
        _filter.value = filter
    }

    /**
     * Validates and adds a manual rule. [action] must be "BLOCK" or "ALLOW".
     * The phone number is run through SanitizationEngine here (needed for the
     * digit-count validation below — sanitizePhoneNumber is idempotent, so
     * BlocklistRepository re-applying it internally is harmless).
     *
     * Security fix (audit finding): [reason] is intentionally passed through
     * RAW, not pre-sanitized here. BlocklistRepository.addBlockRule()/addAllowRule()
     * now own sanitizeTextField() for this field, since sanitizeTextField() is
     * NOT idempotent (its SQL quote-escaping doubles up on repeat application) —
     * sanitizing it both here and in the repository would corrupt any reason
     * containing a quote. Applying it exactly once, at the repository's entity
     * write, is the single source of truth.
     */
    fun addRule(rawNumber: String, action: String, reason: String) {
        val cleanNumber = SanitizationEngine.sanitizePhoneNumber(rawNumber)
        val digitCount = cleanNumber.count { it.isDigit() }

        if (digitCount < 7) {
            _formError.value = "Enter a valid phone number."
            return
        }
        if (action != "BLOCK" && action != "ALLOW") {
            _formError.value = "Choose Block or Allow."
            return
        }

        val reasonOrDefault = reason.ifBlank {
            if (action == "BLOCK") "Manual Block" else "Manual Allow"
        }

        viewModelScope.launch {
            try {
                if (action == "BLOCK") {
                    repository.addBlockRule(cleanNumber, reasonOrDefault)
                } else {
                    repository.addAllowRule(cleanNumber, reasonOrDefault)
                }
                _formError.value = null
                _isAddSheetVisible.value = false
                loadRules()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to add $action rule for $cleanNumber")
                _formError.value = "Could not save that rule — please try again."
            }
        }
    }

    fun deleteRule(entry: UnifiedEntryEntity) {
        viewModelScope.launch {
            try {
                repository.removeRule(entry.phoneNumber)
                loadRules()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to delete rule for ${entry.phoneNumber}")
            }
        }
    }

    companion object {
        private const val TAG = "BlockedNumbersVM"
    }
}

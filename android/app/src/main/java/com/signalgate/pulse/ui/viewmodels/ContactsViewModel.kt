package com.signalgate.pulse.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.pulse.data.repositories.ContactsRepository
import com.signalgate.pulse.database.repositories.BlocklistRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.SecurityRuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactItem(
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val isSelected: Boolean = false
)

class ContactsViewModel(
    private val securityRuleRepository: SecurityRuleRepository,
    private val blocklistRepository: BlocklistRepository,
    private val settingRepository: SettingRepository,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()

    val filteredContacts: List<ContactItem>
        get() {
            val query = _searchQuery.value.trim().lowercase()
            return if (query.isEmpty()) _contacts.value
            else _contacts.value.filter {
                it.displayName.lowercase().contains(query) ||
                it.phoneNumber.contains(query)
            }
        }

    val selectedCount: Int
        get() = _contacts.value.count { it.isSelected }

    fun loadContacts(@Suppress("UNUSED_PARAMETER") context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = contactsRepository.loadContacts()

            _contacts.value = loaded
                .distinctBy { it.normalizedNumber }
                .sortedBy { it.displayName }

            _isLoading.value = false
        }
    }

    fun toggleContact(normalizedNumber: String) {
        _contacts.value = _contacts.value.map {
            if (it.normalizedNumber == normalizedNumber) it.copy(isSelected = !it.isSelected)
            else it
        }
    }

    fun selectAll() {
        _contacts.value = _contacts.value.map { it.copy(isSelected = true) }
    }

    fun clearSelection() {
        _contacts.value = _contacts.value.map { it.copy(isSelected = false) }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Reads the Contacts Allow List sourceId from SettingEntry (seeded by DatabaseInitializer)
     * and inserts selected contacts as ALLOW entries. Screen calls this with no arguments —
     * the sourceId lookup stays in the ViewModel where it belongs.
     */
    fun saveSelectedToAllowList() {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val selected = _contacts.value.filter { it.isSelected }
                if (selected.isEmpty()) {
                    // Nothing selected — treat as an explicit "neither" choice rather
                    // than silently refusing to advance. See skipContactImport() for
                    // the same outcome via a dedicated button.
                    _isSaved.value = true
                    return@launch
                }

                val contactsSourceId = settingRepository.getSettingValue("contacts_source_id")?.toIntOrNull()
                if (contactsSourceId == null) {
                    // Previously bailed silently here, leaving the user stuck on this
                    // screen with no explanation and no way forward. Surface it instead.
                    _saveError.value = "Couldn't save your selection — please try again."
                    return@launch
                }

                // Batched — see SecurityRuleRepository.addContactsAllowBatch() doc.
                // The previous per-contact forEach + addContactAllow() loop triggered
                // one full-table bloom rebuild per contact, which for a "Select All"
                // import of dozens/hundreds of contacts silently took many seconds
                // with no loading indicator (isLoading only ever covered the initial
                // contact-list fetch) — this is what looked like the button "not
                // working." One batched insert + one rebuild fixes both the delay
                // and, combined with isSaving below, the missing feedback.
                securityRuleRepository.addContactsAllowBatch(
                    contacts = selected.map { it.normalizedNumber to it.displayName },
                    sourceId = contactsSourceId
                )

                _isSaved.value = true
            } catch (e: Exception) {
                // Previously uncaught — an exception anywhere in the insert path
                // would silently kill this coroutine, _isSaved would never be set,
                // and the screen would sit there forever with no error and no way
                // forward. Surface it instead, same as the missing-sourceId case above.
                _saveError.value = "Couldn't save your selection — please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Explicit "neither allow nor block any contacts" path — the third onboarding
     * option. Distinct function (rather than just relying on saveSelectedToAllowList
     * with zero selected) so the screen can offer it as its own clearly-labeled
     * button rather than requiring the user to discover that clearing selection
     * and hitting "Import" happens to work.
     */
    fun skipContactImport() {
        _saveError.value = null
        _isSaved.value = true
    }

    /**
     * Block a single contact via BlocklistRepository (Step 1.3).
     */
    fun blockContact(phoneNumber: String, reason: String = "Manual block from contacts") {
        viewModelScope.launch {
            blocklistRepository.addBlockRule(phoneNumber, reason)
        }
    }

}

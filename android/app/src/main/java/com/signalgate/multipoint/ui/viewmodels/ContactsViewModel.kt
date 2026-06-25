package com.signalgate.multipoint.ui.viewmodels

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.daos.SettingDao
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
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
    private val repository: DataSourceRepository,
    private val blocklistRepository: BlocklistRepository,
    private val settingDao: SettingDao
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

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

    fun loadContacts(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = mutableListOf<ContactItem>()

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: continue
                    val number = it.getString(numberIndex) ?: continue
                    val normalized = normalizeNumber(number)
                    if (normalized.isNotBlank()) {
                        loaded.add(ContactItem(name, number, normalized))
                    }
                }
            }

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
            val selected = _contacts.value.filter { it.isSelected }
            if (selected.isEmpty()) return@launch

            val contactsSourceId = settingDao.getSettingValue("contacts_source_id")?.toIntOrNull()
                ?: return@launch // Source not seeded yet — silently bail; seeding happens at app start

            selected.forEach { contact ->
                repository.insertEntry(
                    UnifiedEntryEntity(
                        phoneNumber = contact.normalizedNumber,
                        action = "ALLOW",
                        sourceId = contactsSourceId,
                        category = "Contact",
                        confidence = 100,
                        metadata = contact.displayName
                    )
                )
            }

            _isSaved.value = true
        }
    }

    /**
     * Block a single contact via BlocklistRepository (Step 1.3).
     */
    fun blockContact(phoneNumber: String, reason: String = "Manual block from contacts") {
        viewModelScope.launch {
            blocklistRepository.addBlockRule(phoneNumber, reason)
        }
    }

    private fun normalizeNumber(raw: String): String {
        var cleaned = raw.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("1") && cleaned.length == 11) cleaned = "+$cleaned"
        else if (!cleaned.startsWith("+")) cleaned = "+1$cleaned"
        return cleaned
    }
}

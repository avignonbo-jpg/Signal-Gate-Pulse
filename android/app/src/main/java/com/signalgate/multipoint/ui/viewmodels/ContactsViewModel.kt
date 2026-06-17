package com.signalgate.multipoint.ui.viewmodels

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.database.BlocklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ContactsViewModel for Pulse consumer version
 * References: Architecture-Contract.md
 */
class ContactsViewModel(
    private val context: Context,
    private val blocklistRepository: BlocklistRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null, null, null
                )

                val contactList = mutableListOf<ContactItem>()
                cursor?.use {
                    while (it.moveToNext()) {
                        val name = it.getString(0) ?: ""
                        val number = it.getString(1) ?: ""
                        if (number.isNotBlank()) {
                            contactList.add(
                                ContactItem(
                                    displayName = name,
                                    phoneNumber = number,
                                    normalizedNumber = normalizePhoneNumber(number)
                                )
                            )
                        }
                    }
                }

                _contacts.value = contactList
                Timber.d("Loaded ${contactList.size} contacts")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load contacts")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }

    fun addToBlocklist(number: String) {
        viewModelScope.launch {
            blocklistRepository.insertUserBlock(number)
            Timber.i("User blocked number: $number")
        }
    }
}

data class ContactItem(
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String
)

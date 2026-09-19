package com.signalgate.pulse.data.repositories

import android.content.Context
import android.provider.ContactsContract
import com.signalgate.pulse.ui.viewmodels.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository boundary for the platform ContactsProvider.
 *
 * The ViewModel owns presentation state; this class owns the Binder/provider
 * query, cursor lifecycle, background dispatcher, and phone-number shaping.
 */
class ContactsRepository(private val context: Context) {

    suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ContactItem>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
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
                    result.add(ContactItem(name, number, normalized))
                }
            }
        }

        result
    }

    private fun normalizeNumber(raw: String): String {
        var cleaned = raw.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("1") && cleaned.length == 11) cleaned = "+$cleaned"
        else if (!cleaned.startsWith("+")) cleaned = "+1$cleaned"
        return cleaned
    }
}

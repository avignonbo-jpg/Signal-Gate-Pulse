package com.signalgate.ui.onboarding

import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ContactsViewModel(private val contentResolver: ContentResolver) : ViewModel() {
    val selectedContacts = MutableStateFlow<Set<String>>(emptySet())
    
    fun loadContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val number = it.getString(1)
                if (number != null) contacts.add(Contact(name, number))
            }
        }
        return contacts
    }
    
    data class Contact(val name: String, val number: String)
}

@Composable
fun ContactsImportScreen(viewModel: ContactsViewModel, onContinue: () -> Unit) {
    var contacts by remember { mutableStateOf(emptyList<ContactsViewModel.Contact>()) }
    val selected by viewModel.selectedContacts.collectAsState()
    
    LaunchedEffect(Unit) { contacts = viewModel.loadContacts() }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Import Contacts as Allowed (Auto-Allow Recommended)")
        Button(onClick = { /* Select all logic */ }) { Text("Select All Contacts") }
        
        LazyColumn {
            items(contacts) { contact ->
                Row {
                    Checkbox(
                        checked = selected.contains(contact.number),
                        onCheckedChange = { /* toggle logic */ }
                    )
                    Text("${contact.name} - ${contact.number}")
                }
            }
        }
        
        Button(onClick = onContinue) { Text("Continue to Sources") }
    }
}

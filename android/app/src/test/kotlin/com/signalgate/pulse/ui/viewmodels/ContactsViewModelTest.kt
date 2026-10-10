package com.signalgate.pulse.ui.viewmodels

import android.content.Context
import com.signalgate.pulse.data.repositories.ContactsRepository
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.SecurityRuleRepository
import com.signalgate.pulse.ui.screens.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ContactsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadContacts_leavesContactsUnselectedAndDoesNotWriteAllowList(): Unit {
        runTest(mainDispatcherRule.dispatcher) {
            val contactsRepository = mock<ContactsRepository>()
            whenever(contactsRepository.loadContacts()).thenReturn(
                listOf(
                    ContactItem("Ada", "+1 555 0100", "15550100"),
                    ContactItem("Grace", "+1 555 0101", "15550101")
                )
            )
            val securityRuleRepository = mock<SecurityRuleRepository>()
            val viewModel = ContactsViewModel(
                securityRuleRepository = securityRuleRepository,
                blocklistRepository = mock(),
                settingRepository = mock(),
                contactsRepository = contactsRepository
            )

            viewModel.loadContacts(mock<Context>())
            advanceUntilIdle()

            assertEquals(0, viewModel.selectedCount)
            verify(securityRuleRepository, never()).addContactsAllowBatch(any(), any())
        }
    }

    @Test
    fun skipContactImport_doesNotWriteAllowListOrEntries(): Unit {
        runTest(mainDispatcherRule.dispatcher) {
            val dataSourceRepository = mock<DataSourceRepository>()
            val database = mock<SignalGateDatabase>()
            val unifiedEntryDao = mock<UnifiedEntryDao>()
            val viewModel = ContactsViewModel(
                securityRuleRepository = SecurityRuleRepository(
                    dataSourceRepository = dataSourceRepository,
                    database = database,
                    unifiedEntryDao = unifiedEntryDao,
                    settingRepository = mock<SettingRepository>()
                ),
                blocklistRepository = mock(),
                settingRepository = mock(),
                contactsRepository = mock()
            )

            viewModel.skipContactImport()

            verifyNoInteractions(dataSourceRepository, database, unifiedEntryDao)
        }
    }
}

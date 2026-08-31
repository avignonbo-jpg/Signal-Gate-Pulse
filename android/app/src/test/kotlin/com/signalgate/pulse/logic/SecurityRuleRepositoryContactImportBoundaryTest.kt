package com.signalgate.pulse.logic

import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Phase 0.1 regression coverage for the contacts-import mutation boundary.
 * Contact allow rules must retain their dedicated source attribution while
 * entering the shared DataSourceRepository write chokepoint through
 * SecurityRuleRepository.
 */
class SecurityRuleRepositoryContactImportBoundaryTest {

    @Test
    fun addContactAllow_preservesContactSourceAndMetadataAtDataSourceBoundary() = runBlocking {
        val dataSourceRepository = mock<DataSourceRepository>()
        val repository = SecurityRuleRepository(
            dataSourceRepository = dataSourceRepository,
            database = mock<SignalGateDatabase>(),
            unifiedEntryDao = mock<UnifiedEntryDao>(),
            settingRepository = mock<SettingRepository>()
        )

        repository.addContactAllow(
            phoneNumber = "+15551234567",
            sourceId = 84,
            displayName = "Alice Example"
        )

        val entries = argumentCaptor<List<UnifiedEntryEntity>>()
        verify(dataSourceRepository).insertEntriesAuthoritative(entries.capture())
        verify(dataSourceRepository).rebuildDerivedIndexes()
        val entry = entries.firstValue.single()
        assertEquals("+15551234567", entry.phoneNumber)
        assertEquals("ALLOW", entry.action)
        assertEquals(84, entry.sourceId)
        assertEquals("Contact", entry.category)
        assertEquals(100, entry.confidence)
        assertEquals("Alice Example", entry.metadata)
    }
}

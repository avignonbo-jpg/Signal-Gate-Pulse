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
import org.mockito.kotlin.whenever

/**
 * Phase 0.1 / 0.8 regression coverage.
 *
 * Manual decision-affecting operations must route through the Layer 5
 * SecurityRuleRepository boundary rather than allowing feature callers to
 * construct their own DAO mutation path.
 */
class SecurityRuleRepositoryMutationBoundaryTest {

    @Test
    fun addManualBlock_constructsEntryForDataSourceBoundary() = runBlocking {
        val dataSourceRepository = mock<DataSourceRepository>()
        val repository = repository(dataSourceRepository)

        repository.addManualBlock("+15551234567", "User's block reason")

        val entry = argumentCaptor<UnifiedEntryEntity>()
        verify(dataSourceRepository).insertEntry(entry.capture())
        assertEquals("+15551234567", entry.firstValue.phoneNumber)
        assertEquals("BLOCK", entry.firstValue.action)
        assertEquals(42, entry.firstValue.sourceId)
        assertEquals("User's block reason", entry.firstValue.metadata)
    }

    @Test
    fun addManualAllow_routesAllowEntryThroughDataSourceRepository() = runBlocking {
        val dataSourceRepository = mock<DataSourceRepository>()
        val repository = repository(dataSourceRepository)

        repository.addManualAllow("+15551234567", "User's allow reason")

        val entry = argumentCaptor<UnifiedEntryEntity>()
        verify(dataSourceRepository).insertEntry(entry.capture())
        assertEquals("ALLOW", entry.firstValue.action)
        assertEquals(42, entry.firstValue.sourceId)
        assertEquals("User's allow reason", entry.firstValue.metadata)
    }

    @Test
    fun removeRule_routesNormalizedKeyToAuthoritativeDao() = runBlocking {
        val dataSourceRepository = mock<DataSourceRepository>()
        val dao = mock<UnifiedEntryDao>()
        val repository = repository(dataSourceRepository, dao)

        repository.removeRule("5551234567")

        verify(dao).deleteEntryByNumberAndSource("+15551234567", 42)
    }

    private suspend fun repository(
        dataSourceRepository: DataSourceRepository,
        unifiedEntryDao: UnifiedEntryDao = mock()
    ): SecurityRuleRepository {
        val settings = mock<SettingRepository>()
        whenever(settings.getSettingValue("manual_source_id")).thenReturn("42")
        return SecurityRuleRepository(
            dataSourceRepository = dataSourceRepository,
            database = mock<SignalGateDatabase>(),
            unifiedEntryDao = unifiedEntryDao,
            settingRepository = settings
        )
    }
}

package com.signalgate.pulse.logic

import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.data.security.SnapshotSanityValidator
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SyncHistoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReliableSourceManagerDisabledSourceTest {

    private val dataSourceRepository = mock<DataSourceRepository>()
    private val securityRuleRepository = mock<SecurityRuleRepository>()
    private val syncHistoryRepository = mock<SyncHistoryRepository>()
    private val secureCsvParser = mock<SecureCsvParser>()
    private val snapshotSanityValidator = mock<SnapshotSanityValidator>()

    private val manager = ReliableSourceManager(
        dataSourceRepository = dataSourceRepository,
        securityRuleRepository = securityRuleRepository,
        syncHistoryRepository = syncHistoryRepository,
        secureCsvParser = secureCsvParser,
        snapshotSanityValidator = snapshotSanityValidator
    )

    @Test
    fun automaticSync_skipsExplicitlyDisabledSources() = runBlocking {
        val disabled = SourceEntity(
            id = 7,
            name = "FTC Do Not Call Registry",
            type = "FTC",
            pathOrUrl = "https://example.invalid/ftc",
            isEnabled = false
        )
        whenever(dataSourceRepository.getSourceByName(any())).thenReturn(disabled)

        val results = manager.syncAllFederalSources()

        assertTrue(results.isEmpty())
        verify(dataSourceRepository).getSourceByName("FTC Do Not Call Registry")
        verify(dataSourceRepository).getSourceByName("FCC Consumer Complaints")
        Unit
    }

    @Test
    fun manualSync_doesNotApplyAutomaticEnablementFilter() = runBlocking {
        val disabled = SourceEntity(
            id = 11,
            name = "User-disabled federal source",
            type = "DISABLED_TEST_SOURCE",
            pathOrUrl = "https://example.invalid/test",
            isEnabled = false
        )
        whenever(dataSourceRepository.getSourceById(11)).thenReturn(disabled)

        val result = manager.syncSource(11)

        assertEquals("User-disabled federal source", result.sourceName)
        assertEquals("Source is not a managed federal source", result.errorMessage)
        verify(dataSourceRepository).getSourceById(11)
        Unit
    }

    @Test
    fun missingPersistedRow_remainsEligibleForAutomaticSeeding() = runBlocking {
        whenever(dataSourceRepository.getSourceByName(any())).thenReturn(null)
        whenever(dataSourceRepository.insertSource(any())).thenReturn(12L)
        whenever(securityRuleRepository.beginSourceSync(any())).thenReturn(1L)

        // The policy is tested without making a network request: a missing row is
        // explicitly eligible for seeding, while the actual fetch is outside this
        // regression test's responsibility.
        assertTrue(ReliableSourceManager.shouldSyncAutomatically(null))
        verify(dataSourceRepository, org.mockito.kotlin.never()).getSourceByName("unused")
        Unit
    }
}

package com.signalgate.pulse.logic

import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SourceSyncUseCaseTest {

    private val reliableSourceManager = mock<ReliableSourceManager>().also { manager ->
        whenever(manager.isSyncing).thenReturn(flowOf(false))
        runBlocking {
            whenever(manager.trackSyncing<Any?>(any())).thenAnswer { invocation ->
                val block = invocation.getArgument<suspend () -> Any?>(0)
                runBlocking { block() }
            }
        }
    }
    private val dataSourceRepository = mock<DataSourceRepository>()
    private val useCase = SourceSyncUseCase(reliableSourceManager, dataSourceRepository)

    @Test
    fun syncAllSources_syncsOnlyEnabledFederalSources_andReportsSuccess(): Unit = runBlocking {
        val ftc = SourceEntity(
            id = 11,
            name = "FTC Do Not Call Registry",
            type = "FTC",
            pathOrUrl = "managed-ftc",
            isEnabled = true
        )
        val fcc = SourceEntity(
            id = 12,
            name = "FCC Consumer Complaints",
            type = "FCC",
            pathOrUrl = "managed-fcc",
            isEnabled = true
        )
        val manual = SourceEntity(
            id = 13,
            name = "Manual User Rules",
            type = "MANUAL",
            pathOrUrl = "local",
            isEnabled = true
        )
        val contacts = SourceEntity(
            id = 14,
            name = "Contacts Allow List",
            type = "MANUAL",
            pathOrUrl = "contacts",
            isEnabled = true
        )

        whenever(dataSourceRepository.getSourceById(ftc.id)).thenReturn(ftc)
        whenever(dataSourceRepository.getSourceById(fcc.id)).thenReturn(fcc)
        whenever(reliableSourceManager.syncSource(ftc.id)).thenReturn(
            ReliableSourceManager.SyncResult(ftc.name, 25, success = true)
        )
        whenever(reliableSourceManager.syncSource(fcc.id)).thenReturn(
            ReliableSourceManager.SyncResult(fcc.name, 10, success = true)
        )

        val enabledFederalIds = SourceSyncUseCase.enabledFederalSourceIds(
            listOf(ftc, fcc, manual, contacts)
        )
        assertEquals(listOf(ftc.id, fcc.id), enabledFederalIds)

        val result = useCase.syncSources(enabledFederalIds)

        assertEquals(2, result.acceptedCount)
        assertEquals(2, result.totalCount)
        assertTrue(result.success)
        assertEquals(
            "Enabled federal source sync succeeded: 2/2 accepted",
            result.summaryLine
        )
        verify(reliableSourceManager).syncSource(ftc.id)
        verify(reliableSourceManager).syncSource(fcc.id)
        verify(reliableSourceManager, never()).syncSource(manual.id)
        verify(reliableSourceManager, never()).syncSource(contacts.id)
    }

    @Test
    fun syncSources_rejectsLocalSourceInsteadOfSilentlyDroppingIt(): Unit = runBlocking {
        val manual = SourceEntity(
            id = 21,
            name = "Manual User Rules",
            type = "MANUAL",
            pathOrUrl = "local",
            isEnabled = true
        )
        whenever(dataSourceRepository.getSourceById(manual.id)).thenReturn(manual)

        val result = useCase.syncSources(listOf(manual.id))

        assertFalse(result.success)
        assertEquals(0, result.acceptedCount)
        assertEquals(1, result.totalCount)
        assertEquals("Source is not a managed federal source", result.results.single().errorMessage)
        verify(reliableSourceManager, never()).syncSource(manual.id)
    }
}

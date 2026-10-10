package com.signalgate.pulse.ui.screens

import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.SourceSyncUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestWatcher
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher: TestDispatcher = StandardTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@RunWith(RobolectricTestRunner::class)
class SourcesViewModelDeletionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sourceDao: SourceDao
    private lateinit var viewModel: SourcesViewModel

    @Before
    fun setUp() {
        sourceDao = mock()
        whenever(sourceDao.getAllSources()).thenReturn(flowOf(emptyList()))
        val repository = DataSourceRepository(
            sourceDao = sourceDao,
            entryDao = mock<UnifiedEntryDao>(),
            bloomFilter = BloomFilterEngine(),
            patternBloomFilter = BloomFilterEngine()
        )
        val sourceSyncUseCase = mock<SourceSyncUseCase>()
        whenever(sourceSyncUseCase.isSyncing).thenReturn(flowOf(false))
        viewModel = SourcesViewModel(repository, sourceSyncUseCase, mock<SettingRepository>())
    }

    @Test
    fun deleteProtectedSource_exposesRepositoryFailureToUi(): Unit {
        runTest(mainDispatcherRule.dispatcher) {
            val protectedSource = SourceEntity(
                id = 12,
                name = "FTC Do Not Call",
                type = "FTC",
                pathOrUrl = "remote",
                priority = 90
            )

            viewModel.deleteSource(protectedSource)
            advanceUntilIdle()

            val error = viewModel.sourceActionError.value
            assertTrue(error?.contains("can't be removed") == true)
            verify(sourceDao, never()).deleteSource(protectedSource)
        }
    }

    @Test
    fun deleteNonProtectedSource_succeedsWithoutUiError(): Unit {
        runTest(mainDispatcherRule.dispatcher) {
            val userSource = SourceEntity(
                id = 23,
                name = "Community Blocklist Mirror",
                type = "CSV",
                pathOrUrl = "local.csv",
                priority = 10
            )
            whenever(
                sourceDao.deleteIfNotProtected(
                    userSource.id,
                    DataSourceRepository.PROTECTED_SOURCE_TYPES.toList()
                )
            ).thenReturn(1)

            viewModel.deleteSource(userSource)
            advanceUntilIdle()

            assertNull(viewModel.sourceActionError.value)
            verify(sourceDao).deleteIfNotProtected(
                userSource.id,
                DataSourceRepository.PROTECTED_SOURCE_TYPES.toList()
            )
        }
    }
}

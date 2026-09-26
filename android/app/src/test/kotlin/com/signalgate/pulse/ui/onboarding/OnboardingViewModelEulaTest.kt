package com.signalgate.pulse.ui.onboarding

import android.os.Looper
import com.signalgate.pulse.database.daos.SettingDao
import com.signalgate.pulse.database.entities.SettingEntry
import com.signalgate.pulse.database.repositories.SettingKeys
import com.signalgate.pulse.database.repositories.SettingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/**
 * Hand-rolled fake instead of a Mockito mock, deliberately. SettingRepository
 * wraps suspend DAO calls with `any()` matchers involved in what this test
 * needs to verify, and plain Mockito's matcher stack does not reliably align
 * against a suspend function's hidden Continuation parameter -- see the
 * regression history in PROJECT_LEDGER.md for this file. A real in-memory
 * SettingDao sidesteps that class of problem entirely: no matchers, no
 * suspend/mock interop, just a map.
 */
private class FakeSettingDao : SettingDao {
    val stored = mutableMapOf<String, String>()
    var shouldThrow = false
    var throwOnKey: String? = null

    private fun failIfRequested(key: String) {
        if (shouldThrow || key == throwOnKey) {
            throw IllegalStateException("database unavailable")
        }
    }

    override suspend fun insertSetting(setting: SettingEntry): Long {
        failIfRequested(setting.key)
        stored[setting.key] = setting.value
        return 1L
    }

    override suspend fun updateSetting(setting: SettingEntry) {
        failIfRequested(setting.key)
        stored[setting.key] = setting.value
    }

    override suspend fun getSettingByKey(key: String): SettingEntry? =
        stored[key]?.let { SettingEntry(key = key, value = it) }

    override suspend fun getSettingValue(key: String): String? = stored[key]

    override suspend fun updateSettingValue(key: String, value: String, timestamp: Long) {
        failIfRequested(key)
        stored[key] = value
    }

    override suspend fun getAllSettings(): List<SettingEntry> =
        stored.map { SettingEntry(key = it.key, value = it.value) }
}

@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelEulaTest {

    private val fakeDao = FakeSettingDao()
    private val settingRepository = SettingRepository(fakeDao)

    private fun drainMainDispatcher() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun markEulaAccepted_successPersistsAllFieldsAndSetsAccepted() {
        val viewModel = OnboardingViewModel(settingRepository)

        viewModel.markEulaAccepted(SettingKeys.EULA_CURRENT_VERSION)
        drainMainDispatcher()

        assertTrue(viewModel.eulaAccepted.value)
        assertEquals(null, viewModel.eulaAcceptError.value)
        assertEquals("true", fakeDao.stored[SettingKeys.EULA_ACCEPTED])
        assertEquals(SettingKeys.EULA_CURRENT_VERSION, fakeDao.stored[SettingKeys.EULA_VERSION])
        assertNotNull(fakeDao.stored[SettingKeys.EULA_ACCEPTED_AT])
    }

    @Test
    fun markEulaAccepted_persistenceFailureLeavesStateFalseAndSurfacesError() {
        fakeDao.shouldThrow = true
        val viewModel = OnboardingViewModel(settingRepository)

        viewModel.markEulaAccepted(SettingKeys.EULA_CURRENT_VERSION)
        drainMainDispatcher()

        assertFalse(viewModel.eulaAccepted.value)
        assertEquals(
            "Couldn't save your agreement — please try again.",
            viewModel.eulaAcceptError.value
        )
        assertTrue(fakeDao.stored.isEmpty())
    }

    @Test
    fun markEulaAccepted_versionWriteFailureLeavesAcceptedFlagPersistedWithoutMetadata() {
        fakeDao.throwOnKey = SettingKeys.EULA_VERSION
        val viewModel = OnboardingViewModel(settingRepository)

        viewModel.markEulaAccepted(SettingKeys.EULA_CURRENT_VERSION)
        drainMainDispatcher()

        assertFalse(viewModel.eulaAccepted.value)
        assertEquals(
            "Couldn't save your agreement — please try again.",
            viewModel.eulaAcceptError.value
        )
        assertEquals("true", fakeDao.stored[SettingKeys.EULA_ACCEPTED])
        assertNull(fakeDao.stored[SettingKeys.EULA_VERSION])
        assertNull(fakeDao.stored[SettingKeys.EULA_ACCEPTED_AT])
    }
}

package com.signalgate.pulse.ui.onboarding

import android.os.Looper
import com.signalgate.pulse.database.repositories.SettingKeys
import com.signalgate.pulse.database.repositories.SettingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelEulaTest {

    private val settingRepository = mock<SettingRepository>()

    private fun drainMainDispatcher() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun markEulaAccepted_successPersistsAllFieldsAndSetsAccepted() = runBlocking {
        val viewModel = OnboardingViewModel(settingRepository)

        viewModel.markEulaAccepted(SettingKeys.EULA_CURRENT_VERSION)
        drainMainDispatcher()

        assertTrue(viewModel.eulaAccepted.value)
        assertEquals(null, viewModel.eulaAcceptError.value)
        verify(settingRepository).setSetting(SettingKeys.EULA_ACCEPTED, "true")
        verify(settingRepository).setSetting(
            SettingKeys.EULA_VERSION,
            SettingKeys.EULA_CURRENT_VERSION
        )
        verify(settingRepository).setSetting(
            SettingKeys.EULA_ACCEPTED_AT,
            any()
        )
    }

    @Test
    fun markEulaAccepted_persistenceFailureLeavesStateFalseAndSurfacesError() = runBlocking {
        doThrow(IllegalStateException("database unavailable"))
            .`when`(settingRepository)
            .setSetting(any(), any())
        val viewModel = OnboardingViewModel(settingRepository)

        viewModel.markEulaAccepted(SettingKeys.EULA_CURRENT_VERSION)
        drainMainDispatcher()

        assertFalse(viewModel.eulaAccepted.value)
        assertEquals(
            "Couldn't save your agreement — please try again.",
            viewModel.eulaAcceptError.value
        )
    }
}

package com.signalgate.pulse.logic

import com.signalgate.pulse.CallTier
import com.signalgate.pulse.database.repositories.DataSourceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Phase 1.1 deterministic decision-matrix coverage.
 *
 * Repository-level precedence and source-state behavior remain instrumented because
 * they require Room. This suite verifies the engine's mapping from each authoritative
 * repository decision to the six domain outcomes.
 */
class CallScreeningEngineDecisionMatrixTest {

    @Test
    fun manualAllow_mapsToAllowlistedAndAllow() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("ALLOW", "manual allow", 100, "manual_allow")
        )

        val result = CallScreeningEngine(repository).screenCall("+15551234567", null)

        assertEquals(CallTier.ALLOWLISTED, result.tier)
        assertEquals(ScreeningAction.ALLOW, result.callDecision)
    }

    @Test
    fun externalBlock_mapsToFederalBlockAndBlock() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("BLOCK", "federal block", 85, "aggregated")
        )

        val result = CallScreeningEngine(repository).screenCall("+15551234567", null)

        assertEquals(CallTier.FEDERAL_BLOCK, result.tier)
        assertEquals(ScreeningAction.BLOCK, result.callDecision)
    }

    @Test
    fun manualBlock_mapsToFederalBlockAndBlock() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("BLOCK", "manual block", 100, "manual_block")
        )

        val result = CallScreeningEngine(repository).screenCall("+15551234567", null)

        assertEquals(CallTier.FEDERAL_BLOCK, result.tier)
        assertEquals(ScreeningAction.BLOCK, result.callDecision)
    }

    @Test
    fun highConfidencePattern_mapsToHeuristicBlockAndBlock() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("BLOCK", "pattern", 70, "pattern")
        )

        val result = CallScreeningEngine(repository).screenCall("+15551234567", null)

        assertEquals(CallTier.HEURISTIC_BLOCK, result.tier)
        assertEquals(ScreeningAction.BLOCK, result.callDecision)
    }

    @Test
    fun lowConfidencePattern_mapsToHeuristicFlagAndScreen() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("BLOCK", "pattern", 69, "pattern")
        )

        val result = CallScreeningEngine(repository).screenCall("+15551234567", null)

        assertEquals(CallTier.HEURISTIC_FLAG, result.tier)
        assertEquals(ScreeningAction.SCREEN, result.callDecision)
    }

    @Test
    fun defaultDecision_mapsToCleanUnknownAndAllow() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("ALLOW", "no rule matched", 0, "default")
        )

        val result = CallScreeningEngine(repository).screenCall("+15551234567", null)

        assertEquals(CallTier.CLEAN_UNKNOWN, result.tier)
        assertEquals(ScreeningAction.ALLOW, result.callDecision)
    }

    @Test
    fun formattedCallerId_isNormalizedBeforeAuthoritativeLookup() = runBlocking {
        val repository = repositoryReturning(
            DataSourceRepository.CallDecision("ALLOW", "manual allow", 100, "manual_allow")
        )

        CallScreeningEngine(repository).screenCall("+1 (555) 123-4567", null)

        val number = argumentCaptor<String>()
        verify(repository).getCallDecision(number.capture())
        assertEquals("+15551234567", number.firstValue)
    }

    private suspend fun repositoryReturning(
        decision: DataSourceRepository.CallDecision
    ): DataSourceRepository {
        val repository = mock<DataSourceRepository>()
        whenever(repository.getCallDecision(any())).thenReturn(decision)
        return repository
    }
}


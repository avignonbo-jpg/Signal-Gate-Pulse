package com.signalgate.multipoint

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService as TelecomCallScreeningService
import android.util.Log
import com.signalgate.multipoint.database.entities.CallLogEntry
import com.signalgate.multipoint.database.entities.PendingCardEntity
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.daos.PendingCardDao
import com.signalgate.multipoint.logic.CallScreeningEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * SignalGateCallScreeningService extends the Android framework's CallScreeningService.
 * The class is intentionally named differently from the framework class to avoid
 * the self-extension compile error that existed in a previous revision.
 *
 * Implements the Priority Hierarchy:
 * 1. Manual Allow-list (Whitelist)
 * 2. Manual Block-list
 * 3. Pattern/Prefix Rules
 * 4. Aggregated Data Sources
 * 5. Default (Allow)
 *
 * Step 1.6 changes:
 * - Overlay trigger fixed: keys off callDecision == BLOCK, not spamStatus string
 * - Every BLOCK writes to CallLogEntry (permanent audit) AND PendingCardEntity (digest queue)
 * - Every ALLOW also writes to CallLogEntry so Calls Screened counter works (Step 2.5)
 */
class SignalGateCallScreeningService : TelecomCallScreeningService() {

    private val screeningEngine: CallScreeningEngine by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val pendingCardDao: PendingCardDao by inject()

    companion object {
        private const val TAG = "SignalGateCallScreening"
    }

    /**
     * Enum representing the decision for an incoming call.
     */
    enum class CallDecision {
        ALLOW,
        BLOCK,
        SCREEN
    }

    override fun onScreenCall(details: Call.Details) {
        Log.d(TAG, "Screening call from: ${details.handle?.schemeSpecificPart}")

        val phoneNumber = details.handle?.schemeSpecificPart ?: return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val callInfo = analyzeIncomingCall(phoneNumber)
                val decision = callInfo.callDecision

                applyCallDecision(details, decision)
                writeAuditRecords(callInfo, decision)

                // Bug fix (Step 1.6): trigger overlay on CallDecision.BLOCK,
                // NOT on spamStatus string. Pattern-matched blocks set
                // decision=BLOCK but spamStatus=UNKNOWN — the old string check
                // meant those blocks never triggered the overlay.
                if (decision == CallDecision.BLOCK) {
                    triggerOverlay(callInfo)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error screening call", e)
                val safeResponse = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                respondToCall(details, safeResponse)
            }
        }
    }

    /**
     * Analyzes an incoming call via the screening engine.
     */
    private suspend fun analyzeIncomingCall(phoneNumber: String): CallInfo {
        return try {
            screeningEngine.screenCall(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing call", e)
            CallInfo(
                originalPhoneNumber = phoneNumber,
                normalizedPhoneNumber = normalizePhoneNumber(phoneNumber),
                spamStatus = "UNKNOWN",
                spamCategory = null,
                confidence = null,
                riskLevel = null,
                matchedSources = emptyList(),
                callDecision = CallDecision.ALLOW
            )
        }
    }

    /**
     * Applies the call decision by building a CallResponse and responding to the call.
     * setSkipCallLog(true) and setSkipNotification(true) on BLOCK are correct Android
     * behavior — we suppress the OS-level log and notification because we handle them
     * ourselves via CallLogEntry and the post-call digest card system.
     */
    private fun applyCallDecision(details: Call.Details, decision: CallDecision) {
        val response = when (decision) {
            CallDecision.ALLOW -> CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            CallDecision.BLOCK -> CallResponse.Builder()
                .setDisallowCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
            CallDecision.SCREEN -> CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        }
        respondToCall(details, response)
    }

    /**
     * Writes both audit records on BLOCK. Writes CallLogEntry only on ALLOW/SCREEN.
     *
     * BLOCK -> CallLogEntry (permanent) + PendingCardEntity (digest queue)
     * ALLOW -> CallLogEntry only (for Calls Screened counter, Step 2.5)
     * SCREEN -> CallLogEntry only
     *
     * These are written on Dispatchers.IO to avoid blocking the screening coroutine.
     */
    private fun writeAuditRecords(callInfo: CallInfo, decision: CallDecision) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val matchedSourcesJson = callInfo.matchedSources
                    .joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
                    .takeIf { callInfo.matchedSources.isNotEmpty() }

                callLogRepository.insertCallLog(
                    CallLogEntry(
                        phoneNumber = callInfo.originalPhoneNumber,
                        normalizedPhoneNumber = callInfo.normalizedPhoneNumber,
                        timestamp = System.currentTimeMillis(),
                        decision = decision.name,
                        spamStatus = callInfo.spamStatus,
                        spamCategory = callInfo.spamCategory,
                        confidence = callInfo.confidence,
                        riskLevel = callInfo.riskLevel,
                        matchedSources = matchedSourcesJson
                    )
                )

                if (decision == CallDecision.BLOCK) {
                    pendingCardDao.insertCard(
                        PendingCardEntity(
                            phoneNumber = callInfo.normalizedPhoneNumber,
                            timestamp = System.currentTimeMillis(),
                            decision = decision.name,
                            confidence = callInfo.confidence,
                            decisionSource = callInfo.matchedSources.firstOrNull(),
                            dismissed = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write audit records for ${callInfo.normalizedPhoneNumber}", e)
            }
        }
    }

    /**
     * Triggers the overlay service to display the blocked call card.
     * Only called when decision == CallDecision.BLOCK.
     */
    private fun triggerOverlay(callInfo: CallInfo) {
        val overlayIntent = Intent(this, CallOverlayService::class.java).apply {
            putExtra("call_info", callInfo)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent)
        } else {
            startService(overlayIntent)
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }
}

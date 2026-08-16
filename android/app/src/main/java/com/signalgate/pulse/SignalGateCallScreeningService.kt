package com.signalgate.pulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService as TelecomCallScreeningService
import androidx.core.app.NotificationCompat
import com.signalgate.pulse.CallInfo
import com.signalgate.pulse.CallTier
import com.signalgate.pulse.database.entities.CallLogEntry
import com.signalgate.pulse.database.entities.PendingCardEntity
import com.signalgate.pulse.database.repositories.CallLogRepository
import com.signalgate.pulse.database.repositories.PendingCardRepository
import com.signalgate.pulse.logic.CallScreeningEngine
import com.signalgate.pulse.logic.ScreeningAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class SignalGateCallScreeningService : TelecomCallScreeningService() {

    private val screeningEngine: CallScreeningEngine by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val pendingCardRepository: PendingCardRepository by inject()

    companion object {
        private const val TAG = "SignalGateScreening"
        const val BLOCKED_CALL_CHANNEL_ID   = "blocked_call_review"
        const val BLOCKED_CALL_CHANNEL_NAME = "Blocked Call Review"
    }

    override fun onScreenCall(details: Call.Details) {
        val phoneNumber = details.handle?.schemeSpecificPart ?: return
        Timber.d("onScreenCall: $phoneNumber")

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // screeningEngine.screenCall() catches its own internal errors and
                // returns a typed ScreeningAction.SECURITY_FAILURE CallInfo rather
                // than throwing (§0.6) — so a thrown exception reaching this catch
                // means failure in the code around the engine (response mapping,
                // audit write, notification), not in decisioning itself. Both paths
                // are handled the same way below: fail safe, never as a disguised
                // ALLOW/CLEAN_UNKNOWN.
                val callInfo = screeningEngine.screenCall(phoneNumber, details)
                respondToCall(details, toCallResponse(callInfo.callDecision))
                writeAuditRecords(callInfo)
                if (callInfo.tier == CallTier.HEURISTIC_BLOCK) {
                    fireBlockedCallNotification(callInfo)
                }
            } catch (e: Exception) {
                Timber.e(e, "SECURITY_FAILURE — unhandled error screening $phoneNumber")
                handleSecurityFailure(details, phoneNumber)
            }
        }
    }

    /**
     * §0.6 — the Android `CallResponse` policy, defined separately from the
     * domain decision ([ScreeningAction]). This is the only function in the
     * app permitted to translate a domain outcome into telecom framework
     * semantics.
     *
     * SECURITY_FAILURE is handled explicitly rather than falling through to
     * the ALLOW/SCREEN branch: it rings through today (blocking on an
     * unverified failure risks silencing legitimate calls with no recourse),
     * but that is a deliberate, visible policy choice made here — not an
     * accidental byproduct of SECURITY_FAILURE aliasing ALLOW.
     */
    private fun toCallResponse(action: ScreeningAction): CallResponse = when (action) {
        ScreeningAction.ALLOW, ScreeningAction.SCREEN, ScreeningAction.SECURITY_FAILURE ->
            CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

        ScreeningAction.BLOCK -> CallResponse.Builder()
            .setSilenceCall(true)
            .setSkipCallLog(true)
            .setSkipNotification(true)
            .build()
    }

    /**
     * Reached only when an exception escapes CallScreeningEngine's own
     * SECURITY_FAILURE handling (e.g. response mapping or the audit write
     * itself throws). Applies the same fail-safe CallResponse policy and
     * makes a best-effort attempt to still leave an auditable
     * SECURITY_FAILURE trail — an untyped, silent allow with no log entry
     * would satisfy neither §0.6 invariant.
     */
    private fun handleSecurityFailure(details: Call.Details, phoneNumber: String) {
        respondToCall(details, toCallResponse(ScreeningAction.SECURITY_FAILURE))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                callLogRepository.insertCallLog(
                    CallLogEntry(
                        phoneNumber = phoneNumber,
                        normalizedPhoneNumber = phoneNumber,
                        timestamp = System.currentTimeMillis(),
                        decision = ScreeningAction.SECURITY_FAILURE.name,
                        spamStatus = "SECURITY_FAILURE",
                        spamCategory = null,
                        confidence = null,
                        riskLevel = null,
                        matchedSources = null,
                        notes = CallTier.SECURITY_FAILURE.name
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to write SECURITY_FAILURE audit record for $phoneNumber")
            }
        }
    }

    private fun writeAuditRecords(callInfo: CallInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sourcesJson = callInfo.matchedSources
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")

                callLogRepository.insertCallLog(
                    CallLogEntry(
                        phoneNumber = callInfo.originalPhoneNumber,
                        normalizedPhoneNumber = callInfo.normalizedPhoneNumber,
                        timestamp = System.currentTimeMillis(),
                        decision = callInfo.callDecision.name,
                        spamStatus = callInfo.spamStatus,
                        spamCategory = callInfo.spamCategory,
                        confidence = callInfo.confidence,
                        riskLevel = callInfo.riskLevel,
                        matchedSources = sourcesJson,
                        notes = callInfo.tier.name
                    )
                )

                if (callInfo.tier == CallTier.HEURISTIC_BLOCK) {
                    pendingCardRepository.insertCard(
                        PendingCardEntity(
                            phoneNumber = callInfo.normalizedPhoneNumber,
                            timestamp = System.currentTimeMillis(),
                            decision = callInfo.callDecision.name,
                            confidence = callInfo.confidence ?: 0,
                            decisionSource = "Tier 3 HEURISTIC_BLOCK"
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to write audit records")
            }
        }
    }

    private fun fireBlockedCallNotification(callInfo: CallInfo) {
        val context = applicationContext
        val notificationId = callInfo.normalizedPhoneNumber.hashCode()

        createBlockedCallChannel(context)

        val digestIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("signalgate://digest"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            digestIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notSpamIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_NOT_SPAM
            putExtra(CallActionReceiver.EXTRA_PHONE_NUMBER, callInfo.normalizedPhoneNumber)
            putExtra(CallActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val notSpamPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            notSpamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confidenceText = callInfo.confidence?.let { " ($it% match)" } ?: ""
        val bodyText = "${callInfo.originalPhoneNumber}$confidenceText"

        val notification = NotificationCompat.Builder(context, BLOCKED_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.shield_logo)
            .setContentTitle("Call Blocked")
            .setContentText(bodyText)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Not Spam", notSpamPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    private fun createBlockedCallChannel(context: Context) {
        val channel = NotificationChannel(
            BLOCKED_CALL_CHANNEL_ID,
            BLOCKED_CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Review calls blocked by SignalGate Pulse"
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}

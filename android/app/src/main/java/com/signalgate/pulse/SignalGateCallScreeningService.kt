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
import com.signalgate.pulse.logic.NotificationPolicy
import com.signalgate.pulse.logic.ScreeningDecision
import com.signalgate.pulse.ui.notifications.PulseHapticsController
import com.signalgate.pulse.ui.notifications.PulseTriggerLimiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.koin.android.ext.android.inject
import timber.log.Timber

class SignalGateCallScreeningService : TelecomCallScreeningService() {

    override fun onCreate() {
        super.onCreate()
        StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_SERVICE_ON_CREATE)
    }

    private val screeningEngine: CallScreeningEngine by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val pendingCardRepository: PendingCardRepository by inject()
    private val pulseHapticsController: PulseHapticsController by inject()
    private val pulseTriggerLimiter: PulseTriggerLimiter by inject()
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(4)
    )

    companion object {
        private const val TAG = "SignalGateScreening"
        const val BLOCKED_CALL_CHANNEL_ID   = "blocked_call_review"
        const val BLOCKED_CALL_CHANNEL_NAME = "Blocked Call Review"
    }

    override fun onScreenCall(details: Call.Details) {
        handleScreeningRequest(details)
    }

    /**
     * Thin Telecom entrypoint delegate. The injectable callbacks let JVM tests
     * exercise the exact null-handle and deadline branches without depending on
     * framework response interception or asynchronous sleeps.
     */
    internal fun handleScreeningRequest(
        details: Call.Details,
        respond: (CallResponse) -> Unit = { response -> respondToCall(details, response) },
        launch: ((suspend () -> Unit) -> Unit) = { block ->
            serviceScope.launch { block() }
            Unit
        },
        onSecurityFailure: (String) -> Unit = { phoneNumber ->
            handleSecurityFailure(details, phoneNumber, respond = respond)
        }
    ) {
        // A malformed handle must still receive an explicit response; a bare return
        // would let the Telecom framework treat the missing response as implicit ALLOW.
        val phoneNumber = details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
        if (phoneNumber == null) {
            Timber.e("SECURITY_FAILURE — onScreenCall received a null/malformed Call.Details.handle")
            onSecurityFailure("UNKNOWN_MALFORMED_HANDLE")
            return
        }
        Timber.d("onScreenCall: screening request received")

        launch {
            try {
                // Resolve dependencies before the measured decision begins, but keep
                // all post-response work behind the explicit service-owned scope.
                val engine = screeningEngine
                val auditRepository = callLogRepository
                val reviewRepository = pendingCardRepository
                pulseHapticsController
                pulseTriggerLimiter
                StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_DEPENDENCIES_READY)
                StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_DECISION_ENGINE_READY)
                StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_DECISION_BEGIN)

                processScreeningCall(
                    phoneNumber = phoneNumber,
                    details = details,
                    engine = engine,
                    respond = respond,
                    persist = { callInfo, decision ->
                        executeDecisionConsequences(
                            callInfo = callInfo,
                            decision = decision,
                            callLogRepository = auditRepository,
                            pendingCardRepository = reviewRepository
                        )
                    },
                    dispatchUx = ::dispatchDecisionUx
                )
            } catch (e: TimeoutCancellationException) {
                Timber.e(e, "SECURITY_FAILURE — screening decision exceeded internal deadline")
                onSecurityFailure(phoneNumber)
            } catch (e: Exception) {
                Timber.e(e, "SECURITY_FAILURE — unhandled screening error")
                onSecurityFailure(phoneNumber)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Runs the decision and emits its telecom response before persistence or UX.
     * The callback is internal so JVM tests can prove that a blocked or throwing
     * persistence operation cannot delay or suppress the response.
     */
    internal suspend fun processScreeningCall(
        phoneNumber: String,
        details: Call.Details,
        engine: CallScreeningEngine,
        respond: (CallResponse) -> Unit,
        persist: suspend (CallInfo, ScreeningDecision) -> Unit,
        dispatchUx: (CallInfo, ScreeningDecision) -> Unit
    ) {
        val callInfo = withTimeout(3_500) { engine.screenCall(phoneNumber, details) }
        val decision = callInfo.screeningDecision

        // Telecom response is the deadline-critical operation. It deliberately
        // precedes audit/review persistence, notification, and haptic dispatch.
        respond(toCallResponse(decision.callAction))

        try {
            persist(callInfo, decision)
        } catch (e: Exception) {
            // The decision response is already complete. A persistence failure must
            // not trigger a second response or rewrite the completed call outcome.
            Timber.e(e, "Post-response consequence persistence failed")
        }

        try {
            dispatchUx(callInfo, decision)
        } catch (e: Exception) {
            // UX is best effort after both decision and response are complete.
            Timber.e(e, "Optional screening UX dispatch failed")
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
     * Reached when an exception escapes CallScreeningEngine's own
     * SECURITY_FAILURE handling (e.g. response mapping or consequence work).
     * Emits the explicit failure response first, then audits asynchronously in the
     * service-owned scope. The callbacks keep this edge directly testable without
     * constructing a Telecom framework callback or relying on sleeps.
     */
    internal fun handleSecurityFailure(
        details: Call.Details,
        phoneNumber: String,
        respond: (CallResponse) -> Unit = { response -> respondToCall(details, response) },
        audit: suspend (CallLogEntry) -> Unit = { entry -> callLogRepository.insertCallLog(entry) }
    ) {
        respond(toCallResponse(ScreeningAction.SECURITY_FAILURE))
        val failureEntry = CallLogEntry(
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
        serviceScope.launch {
            try {
                audit(failureEntry)
            } catch (e: Exception) {
                Timber.e(e, "Failed to write SECURITY_FAILURE audit record")
            }
        }
    }

    /**
     * Dispatches only the UX consequences described by the immutable decision.
     * Persistence has already completed when this function is called. The limiter
     * can suppress repeated review UX but cannot alter the decision or its records.
     */
    private fun dispatchDecisionUx(
        callInfo: CallInfo,
        decision: ScreeningDecision
    ) {
        when (decision.notificationPolicy) {
            NotificationPolicy.BLOCK_REVIEW -> {
                if (pulseTriggerLimiter.shouldDispatchNotification(
                        NotificationPolicy.BLOCK_REVIEW,
                        callInfo.normalizedPhoneNumber
                    )
                ) {
                    fireBlockedCallNotification(callInfo)
                }
                pulseHapticsController.dispatch(decision.hapticPolicy)
            }
            NotificationPolicy.REVIEW_AVAILABLE -> {
                // Notification and haptic share one cooldown decision so they are
                // co-throttled and never present inconsistent review UX.
                if (pulseTriggerLimiter.shouldDispatchNotification(
                        NotificationPolicy.REVIEW_AVAILABLE,
                        callInfo.normalizedPhoneNumber
                    )
                ) {
                    fireReviewAvailableNotification(callInfo)
                    pulseHapticsController.dispatch(decision.hapticPolicy)
                }
            }
            NotificationPolicy.NONE -> Unit
        }
    }

    /**
     * Persists the explicit decision consequences at the application boundary.
     *
     * This seam contains the persistence half of the consequence contract. The
     * caller completes it before dispatching Phase 2.1 notification or haptic UX.
     * Keeping this function internal makes the complete gray-zone persistence
     * chain testable without constructing TelecomCallScreeningService or relying
     * on asynchronous sleeps in tests.
     */
    internal suspend fun executeDecisionConsequences(
        callInfo: CallInfo,
        decision: com.signalgate.pulse.logic.ScreeningDecision,
        callLogRepository: CallLogRepository,
        pendingCardRepository: PendingCardRepository,
        now: () -> Long = { System.currentTimeMillis() }
    ) {
        if (!decision.auditRequired) return

        val sourcesJson = callInfo.matchedSources
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
        val timestamp = now()

        callLogRepository.insertCallLog(
            CallLogEntry(
                phoneNumber = callInfo.originalPhoneNumber,
                normalizedPhoneNumber = callInfo.normalizedPhoneNumber,
                timestamp = timestamp,
                decision = callInfo.callDecision.name,
                spamStatus = callInfo.spamStatus,
                spamCategory = callInfo.spamCategory,
                confidence = callInfo.confidence,
                riskLevel = callInfo.riskLevel,
                matchedSources = sourcesJson,
                notes = callInfo.tier.name
            )
        )

        if (decision.reviewCardRequired) {
            pendingCardRepository.insertCard(
                PendingCardEntity(
                    phoneNumber = callInfo.normalizedPhoneNumber,
                    timestamp = timestamp,
                    decision = decision.callAction.name,
                    confidence = callInfo.confidence ?: 0,
                    decisionSource = "${decision.tier.name} review"
                )
            )
        }
    }

    private fun fireBlockedCallNotification(callInfo: CallInfo) {
        val context = applicationContext
        val notificationId = callInfo.normalizedPhoneNumber.hashCode()
        createBlockedCallChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, buildBlockedCallNotification(context, callInfo))
    }

    /**
     * Builds the blocked-call notification with an explicit private visibility
     * policy. The raw number is carried only in the explicit, non-exported
     * PendingIntent extra required to route the action through CallActionReceiver;
     * it is never rendered in notification content or its public redaction.
     */
    internal fun buildBlockedCallNotification(
        context: Context,
        callInfo: CallInfo
    ): android.app.Notification {
        val notificationId = callInfo.normalizedPhoneNumber.hashCode()
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
        return NotificationCompat.Builder(context, BLOCKED_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.shield_logo)
            .setContentTitle("Call Blocked")
            .setContentText("A blocked call is available for review.")
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Not Spam", notSpamPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(buildRedactedPublicVersion(context))
            .build()
    }

    private fun fireReviewAvailableNotification(callInfo: CallInfo) {
        val context = applicationContext
        val notificationId = callInfo.normalizedPhoneNumber.hashCode()
        createBlockedCallChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, buildReviewAvailableNotification(context, callInfo))
    }

    internal fun buildReviewAvailableNotification(
        context: Context,
        callInfo: CallInfo
    ): android.app.Notification {
        val notificationId = callInfo.normalizedPhoneNumber.hashCode()
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
        return NotificationCompat.Builder(context, BLOCKED_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.shield_logo)
            .setContentTitle("Call Needs Review")
            .setContentText("A suspicious call needs review.")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(buildRedactedPublicVersion(context))
            .build()
    }

    /**
     * Redacted representation used when Android mirrors a private notification
     * to a lock screen or other privacy-sensitive surface. It intentionally has
     * no phone number, confidence, action, or call-specific identifying detail.
     */
    private fun buildRedactedPublicVersion(context: Context): android.app.Notification =
        NotificationCompat.Builder(context, BLOCKED_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.shield_logo)
            .setContentTitle("SignalGate Pulse")
            .setContentText("Call review available")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

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

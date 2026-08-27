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

    companion object {
        private const val TAG = "SignalGateScreening"
        const val BLOCKED_CALL_CHANNEL_ID   = "blocked_call_review"
        const val BLOCKED_CALL_CHANNEL_NAME = "Blocked Call Review"
    }

    override fun onScreenCall(details: Call.Details) {
        // Phase 4.0.1 fix (2026-08-25): this used to be
        // `details.handle?.schemeSpecificPart ?: return` — a bare return here
        // never called respondToCall(), and Android treats a non-response
        // within its ~5s deadline as the call proceeding: an implicit ALLOW
        // reached through a path that bypassed every SECURITY_FAILURE
        // machinery below entirely (the null case exited before the
        // try/catch that feeds handleSecurityFailure() even started).
        // Confirmed live against source before this fix, not assumed stale —
        // see PROJECT_LEDGER.md for the verification.
        val handleSchemeSpecificPart = details.handle?.schemeSpecificPart
        if (handleSchemeSpecificPart == null) {
            Timber.e("SECURITY_FAILURE — onScreenCall received a null/malformed Call.Details.handle")
            handleSecurityFailure(details, phoneNumber = "UNKNOWN_MALFORMED_HANDLE")
            return
        }
        val phoneNumber = handleSchemeSpecificPart
        Timber.d("onScreenCall: screening request received")

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Force resolution of every service dependency before the decision
                // begins so the measured readiness marker includes Koin resolution.
                val engine = screeningEngine
                val auditRepository = callLogRepository
                val reviewRepository = pendingCardRepository
                pulseHapticsController
                pulseTriggerLimiter
                StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_DEPENDENCIES_READY)
                StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_DECISION_ENGINE_READY)
                // screeningEngine.screenCall() catches its own internal errors and
                // returns a typed ScreeningAction.SECURITY_FAILURE CallInfo rather
                // than throwing (§0.6) — so a thrown exception reaching this catch
                // means failure in the code around the engine (response mapping,
                // audit write, notification), not in decisioning itself. Both paths
                // are handled the same way below: fail safe, never as a disguised
                // ALLOW/CLEAN_UNKNOWN.
                //
                // Phase 4.0.1 fix (2026-08-25): explicit deadline around the
                // decision call itself. Android's own CallScreeningService
                // contract is ~5s; this is intentionally set well under that so
                // a timeout here is caught by the same catch block below and
                // produces an explicit, audited SECURITY_FAILURE response —
                // rather than letting a hang consume the whole platform
                // deadline and produce a silent implicit-ALLOW non-response.
                // TimeoutCancellationException is a CancellationException, so
                // it's caught by `catch (e: Exception)` below like any other
                // failure — that's deliberate here (a fire-and-forget
                // CoroutineScope launch, not structured concurrency where
                // re-throwing cancellation would matter).
                StartupDiagnostics.mark(StartupDiagnostics.Event.SCREENING_DECISION_BEGIN)
                val callInfo = withTimeout(3_500) { engine.screenCall(phoneNumber, details) }
                val decision = callInfo.screeningDecision
                respondToCall(details, toCallResponse(decision.callAction))
                // Persist every explicit consequence before consulting the UX limiter.
                // Rate limiting may suppress dispatch, never audit or review state.
                executeDecisionConsequences(
                    callInfo = callInfo,
                    decision = decision,
                    callLogRepository = auditRepository,
                    pendingCardRepository = reviewRepository
                )
                try {
                    dispatchDecisionUx(callInfo, decision)
                } catch (e: Exception) {
                    // UX dispatch is best effort after persistence. A notification
                    // or platform haptic failure must not rewrite the completed
                    // domain outcome as SECURITY_FAILURE.
                    Timber.e(e, "Optional screening UX dispatch failed")
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e(e, "SECURITY_FAILURE — screening decision exceeded internal deadline")
                handleSecurityFailure(details, phoneNumber)
            } catch (e: Exception) {
                Timber.e(e, "SECURITY_FAILURE — unhandled screening error")
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

package com.signalgate.multipoint

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService as TelecomCallScreeningService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.signalgate.multipoint.CallInfo
import com.signalgate.multipoint.CallTier
import com.signalgate.multipoint.database.entities.CallLogEntry
import com.signalgate.multipoint.database.entities.PendingCardEntity
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.PendingCardRepository
import com.signalgate.multipoint.logic.CallScreeningEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class SignalGateCallScreeningService : TelecomCallScreeningService() {

    private val screeningEngine: CallScreeningEngine by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val pendingCardRepository: PendingCardRepository by inject()

    enum class CallDecision { ALLOW, BLOCK, SCREEN }

    companion object {
        private const val TAG = "SignalGateScreening"
        const val BLOCKED_CALL_CHANNEL_ID   = "blocked_call_review"
        const val BLOCKED_CALL_CHANNEL_NAME = "Blocked Call Review"
    }

    override fun onScreenCall(details: Call.Details) {
        val phoneNumber = details.handle?.schemeSpecificPart ?: return
        Log.d(TAG, "onScreenCall: $phoneNumber")

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val callInfo = screeningEngine.screenCall(phoneNumber, details)
                applyCallDecision(details, callInfo)
                writeAuditRecords(callInfo)
                if (callInfo.tier == CallTier.HEURISTIC_BLOCK) {
                    fireBlockedCallNotification(callInfo)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error screening call — defaulting to allow", e)
                respondToCall(
                    details,
                    CallResponse.Builder()
                        .setDisallowCall(false)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()
                )
            }
        }
    }

    private fun applyCallDecision(details: Call.Details, callInfo: CallInfo) {
        val response = when (callInfo.callDecision) {
            CallDecision.ALLOW, CallDecision.SCREEN -> CallResponse.Builder()
                .setDisallowCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            CallDecision.BLOCK -> CallResponse.Builder()
                .setSilenceCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
        }
        respondToCall(details, response)
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
                            confidence = callInfo.confidence,
                            decisionSource = callInfo.matchedSources.firstOrNull()
                                ?: "Tier 3 HEURISTIC_BLOCK"
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
}

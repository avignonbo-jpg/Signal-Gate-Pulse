package com.signalgate.pulse

import android.os.Parcelable
import com.signalgate.pulse.logic.ScreeningAction
import kotlinx.parcelize.Parcelize

/**
 * CallTier represents which tier of the call handling architecture matched
 * this call. Set by CallScreeningEngine, consumed by CallScreeningService
 * to determine post-call behavior (log, PendingCard, notification, etc.).
 *
 * Tier 1 ALLOWLISTED    — contact allowlist match. Rings through. No notification.
 * Tier 2 FEDERAL_BLOCK  — FTC/FCC list or manual block. Silent to voicemail. Log only.
 * Tier 3 HEURISTIC_BLOCK — pattern/heuristic block. Silent to voicemail. Notification + PendingCard.
 * Tier 4 HEURISTIC_FLAG  — low-confidence near-miss. Rings through. Faint log tag.
 * Tier 5 CLEAN_UNKNOWN  — no signal. Rings through. Standard log.
 *
 * SECURITY_FAILURE (§0.6) is deliberately not part of the five-tier decision
 * matrix — it is not a decision outcome at all, but a typed record that the
 * decision/security subsystem itself failed (e.g. CallScreeningEngine threw
 * before a tier could be determined). It exists so that a subsystem failure
 * can never be represented, logged, or displayed as CLEAN_UNKNOWN — the two
 * are not interchangeable: one means "we checked and found nothing," the
 * other means "we could not check."
 *
 * Steps 1.6, 1.7, 1.8 wired. Step 1.11 will populate Tiers 3/4 with real heuristics.
 */
enum class CallTier {
    ALLOWLISTED,
    FEDERAL_BLOCK,
    HEURISTIC_BLOCK,
    HEURISTIC_FLAG,
    CLEAN_UNKNOWN,
    SECURITY_FAILURE
}

/**
 * CallInfo is the carrier object flowing from CallScreeningEngine through
 * CallScreeningService. Parcelable so it can be passed via Intent extras if needed.
 *
 * The [tier] field drives all post-call behaviour in CallScreeningService — which
 * combination of CallLogEntry, PendingCardEntity, and notification fires depends
 * entirely on the tier, not on string comparisons of spamStatus.
 *
 * [callDecision] is [ScreeningAction] — a domain-only type with no dependency
 * on `android.telecom`. §0.6 requires the Android `CallResponse` policy to be
 * defined separately from the domain decision; the mapping from
 * [ScreeningAction] to the framework's `CallResponse` lives solely in
 * `SignalGateCallScreeningService.toCallResponse()`.
 */
@Parcelize
data class CallInfo(
    val originalPhoneNumber: String,
    val normalizedPhoneNumber: String,
    val spamStatus: String,
    val spamCategory: String?,
    val confidence: Int?,
    val riskLevel: String?,
    val matchedSources: List<String>,
    val callDecision: ScreeningAction,
    val tier: CallTier
) : Parcelable

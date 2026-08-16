package com.signalgate.pulse.logic

/**
 * ScreeningAction is the domain-level outcome of [CallScreeningEngine.screenCall],
 * independent of the Android telecom API.
 *
 * Build plan §0.6 requires the Android `CallResponse` policy to be defined
 * *separately* from the domain decision. Previously `CallInfo.callDecision`
 * carried `SignalGateCallScreeningService.CallDecision` — an Android-framework
 * type — directly out of the domain layer (`CallScreeningEngine`), and the
 * engine's own exception handler used that same ALLOW value to represent an
 * internal subsystem failure. `ScreeningAction` replaces that type as the
 * carrier inside [com.signalgate.pulse.CallInfo]. The Android-facing
 * `CallResponse` is now derived from this value exclusively inside
 * `SignalGateCallScreeningService.toCallResponse()`, which is the one place
 * permitted to know about `android.telecom.CallScreeningService.CallResponse`.
 *
 * Required invariants (§0.6):
 *   exception ≠ ALLOW
 *   security failure ≠ CLEAN_UNKNOWN
 *
 * [SECURITY_FAILURE] is the typed state satisfying both: it is a distinct
 * value from [ALLOW], and it is carried alongside [com.signalgate.pulse.CallTier.SECURITY_FAILURE]
 * rather than [com.signalgate.pulse.CallTier.CLEAN_UNKNOWN], so a failed
 * decision/security subsystem can never be silently recorded, displayed, or
 * audited as a clean/allowed call.
 */
enum class ScreeningAction {
    ALLOW,
    BLOCK,
    SCREEN,
    SECURITY_FAILURE
}

// DO NOT let this file's package/imports revert to com.signalgate.multipoint.
// This file was added to consumer-v1 independently of the 2026-08-14/15
// pulse-package-rename merge, so it was never run through that rename script.
// A prior CI run caught it still declaring "package com.signalgate.multipoint...",
// which broke compilation for every file that depends on this one (see
// PROJECT_LEDGER.md, 2026-08-14/15 entry). If this file is ever regenerated,
// restored from a backup/snapshot, or reintroduced via a future merge, verify
// its package and every import still say com.signalgate.pulse before trusting it,
// even if it lands with no conflict markers — "no conflict" is not the same as
// "correct," as this incident showed.
package com.signalgate.pulse.database.repositories

/**
 * SettingKeys — centralizes the SettingEntry key strings for values migrated
 * off SharedPreferences under Step 2.6.
 *
 * Small on purpose: this exists so a key name is a compile-time-checked
 * constant reference, not a repeated raw string prone to typos across
 * OnboardingViewModel, DashboardViewModel, and SettingsViewModel. Add to this
 * object as further keys migrate — don't reintroduce raw string literals at
 * new call sites once a key has an entry here.
 *
 * Deliberately NOT included (see Step 2.6 scope decision):
 *   - eula_accepted / eula_version / eula_accepted_at — kept on SharedPreferences.
 *     Independent lifecycle semantics (a future terms-version bump needs to
 *     force re-acceptance without re-running the whole wizard); migrating it
 *     alongside these three would have mixed two different-shaped concerns
 *     into one push.
 */
object SettingKeys {
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val SHIELD_RED = "shield_red"
    const val SHIELD_GREEN = "shield_green"
    const val SHIELD_BLUE = "shield_blue"

    /**
     * On-device gray-zone heuristics protection level, set on onboarding Step 3
     * and editable later from Settings. Value is one of HeuristicsMode's `key`
     * strings ("OFF" / "CONSERVATIVE" / "BALANCED" / "AGGRESSIVE"). Read by
     * CallScreeningEngine on every screenCall() to decide whether the gray-zone
     * CallRiskEvaluator check runs at all, and if so, at what score threshold.
     */
    const val HEURISTICS_MODE = "heuristics_mode"
}

/**
 * HeuristicsMode — the four on-device heuristics protection levels exposed in
 * onboarding Step 3 (and later, Settings).
 *
 * [riskThreshold] is the score (0–100, from CallRiskEvaluator) a gray-zone call
 * must meet or exceed to be elevated to HEURISTIC_FLAG (rings through + digest
 * entry) instead of passing as CLEAN_UNKNOWN. Lower threshold = more aggressive
 * = more calls get flagged for review = more false positives but fewer missed
 * spam calls. null threshold means heuristics are OFF: CallRiskEvaluator never
 * runs and every gray-zone call is CLEAN_UNKNOWN.
 *
 * CONSERVATIVE/BALANCED/AGGRESSIVE correspond to the onboarding copy's "low" /
 * "medium" / and the highest of the three sensitivity settings.
 */
enum class HeuristicsMode(val key: String, val label: String, val riskThreshold: Int?) {
    OFF("OFF", "Off", null),
    CONSERVATIVE("CONSERVATIVE", "Low (Conservative)", 70),
    BALANCED("BALANCED", "Medium (Balanced)", 55),
    AGGRESSIVE("AGGRESSIVE", "High (Aggressive)", 40);

    companion object {
        val DEFAULT = BALANCED
        fun fromKey(key: String?): HeuristicsMode = entries.find { it.key == key } ?: DEFAULT
    }
}

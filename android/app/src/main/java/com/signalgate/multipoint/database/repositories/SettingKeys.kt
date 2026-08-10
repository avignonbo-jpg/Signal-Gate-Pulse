package com.signalgate.multipoint.database.repositories

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
}

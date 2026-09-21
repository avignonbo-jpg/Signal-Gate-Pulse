# =============================================
# Signal Gate - ProGuard / R8 rules
# =============================================

# Keep everything in our package (important for CallScreeningService)
# DO NOT let these -keep rules revert to com.signalgate.multipoint.
# Until the 2026-08-14/15 session, all four rules below pointed at the old
# multipoint package, which no longer contains any real classes post-rename —
# meaning they were silently protecting nothing. Since debug builds don't run
# R8/ProGuard, this bug produced no CI failure and would only have surfaced as
# unexplained crashes/missing classes in an actual release build. The package
# these rules protect (com.signalgate.pulse) must match the app's real source
# tree, NOT applicationId (which is deliberately still com.signalgate.multipoint.pulse
# — see build.gradle and PROJECT_LEDGER.md). Package and applicationId are
# different things; don't "fix" one to match the other.
-keep class com.signalgate.pulse.** { *; }
-keepclassmembers class com.signalgate.pulse.** { *; }

# Keep Room Database, Entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keepclassmembers class * {
    @androidx.room.* *;
}

# Keep Kotlin Coroutines (very important)
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }


# Suppress common warnings
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.android.material.**

# Optional: Keep all fragment and activity classes
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.appcompat.app.AppCompatActivity

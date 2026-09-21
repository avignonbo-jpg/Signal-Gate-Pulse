# =============================================
# Signal Gate - ProGuard / R8 rules
# =============================================

# Room entities/DAOs/database use reflection internally; keep them.
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keepclassmembers class * {
    @androidx.room.* *;
}

# KoinWorkerFactory.createWorker() resolves Worker subclasses via
# Class.forName(workerClassName) at runtime (see di/KoinWorkerFactory.kt) --
# a plain string lookup that R8 can't trace. Without this rule, a renamed
# Worker class fails that lookup silently (caught by the factory's own
# try/catch, returning null with no crash) and background sync just stops
# working in a release build with nothing to signal why.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Keep Kotlin Coroutines (very important)
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Suppress common warnings
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.android.material.**

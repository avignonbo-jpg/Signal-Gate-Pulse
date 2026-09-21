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

# SQLCipher (net.zetetic:sqlcipher-android) — AAR ships no consumer-rules.pro/proguard.txt.
# register_android_database_SQLiteConnection() looks up SQLiteCustomFunction's 'name' (String)
# and 'numArgs' (int) fields by name via JNI GetFieldID during JNI_OnLoad. A failed lookup here
# is fatal (ART calls Runtime::Abort, not a catchable Java exception). includedescriptorclasses
# additionally protects types referenced only in field/method descriptors, which plain -keep does not.
-keep,includedescriptorclasses class net.zetetic.database.** { *; }
-keep,includedescriptorclasses interface net.zetetic.database.** { *; }

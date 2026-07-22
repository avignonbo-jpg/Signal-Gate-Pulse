# =============================================
# Signal Gate - ProGuard / R8 rules
# =============================================

# Keep everything in our package (important for CallScreeningService)
-keep class com.signalgate.multipoint.** { *; }
-keepclassmembers class com.signalgate.multipoint.** { *; }

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

# Keep CallScreeningService (critical - must not be obfuscated)
-keep public class com.signalgate.multipoint.CallScreeningService { *; }

# Keep SettingsFragment and anything using SharedPreferences
-keep class com.signalgate.multipoint.ui.SettingsFragment { *; }

# Suppress common warnings
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.android.material.**

# Optional: Keep all fragment and activity classes
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.appcompat.app.AppCompatActivity

package com.signalgate.pulse

import android.os.SystemClock
import timber.log.Timber

/**
 * Debug startup timeline markers for real-device cold-process diagnosis.
 *
 * Markers contain only fixed event names and monotonic elapsed milliseconds.
 * They never include phone numbers, database contents, keys, preference values,
 * source names, or exception payloads. Timber's existing release filtering keeps
 * these informational markers out of release logs.
 */
object StartupDiagnostics {
    private val processStartNanos = SystemClock.elapsedRealtimeNanos()

    fun mark(event: Event) {
        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - processStartNanos) / 1_000_000L
        Timber.tag("SignalGate").i("STARTUP_CHECKPOINT: event=${event.value}, elapsedMs=$elapsedMs")
    }

    enum class Event(val value: String) {
        APPLICATION_ON_CREATE_BEGIN("application_on_create_begin"),
        APPLICATION_ON_CREATE_END("application_on_create_end"),
        KOIN_START_BEGIN("koin_start_begin"),
        KOIN_STARTED("koin_started"),
        DATABASE_INIT_BEGIN("database_init_begin"),
        KEYSTORE_INIT_BEGIN("keystore_init_begin"),
        KEYSTORE_INITIALIZED("keystore_initialized"),
        ENCRYPTED_PREFS_READ_BEGIN("encrypted_preferences_read_begin"),
        ENCRYPTED_PREFS_READ_END("encrypted_preferences_read_end"),
        SQLCIPHER_INIT_BEGIN("sqlcipher_init_begin"),
        SQLCIPHER_FACTORY_READY("sqlcipher_factory_ready"),
        ROOM_OPEN_MIGRATION_BEGIN("room_open_migration_begin"),
        MIGRATION_1_2_BEGIN("migration_1_2_begin"),
        MIGRATION_1_2_END("migration_1_2_end"),
        MIGRATION_2_3_BEGIN("migration_2_3_begin"),
        MIGRATION_2_3_END("migration_2_3_end"),
        MIGRATION_3_4_BEGIN("migration_3_4_begin"),
        MIGRATION_3_4_END("migration_3_4_end"),
        ROOM_OPEN_MIGRATION_END("room_open_migration_end"),
        SOURCE_SEED_BEGIN("source_seed_begin"),
        SOURCE_SEED_END("source_seed_end"),
        DATABASE_INIT_END("database_init_end"),
        BLOOM_REHYDRATION_BEGIN("bloom_rehydration_begin"),
        BLOOM_READY("bloom_ready"),
        APP_READINESS_TRUE("app_readiness_true"),
        ACTIVITY_ON_CREATE_BEGIN("activity_on_create_begin"),
        ACTIVITY_CONTENT_SET("activity_content_set"),
        ACTIVITY_FIRST_FRAME("activity_first_frame"),
        SCREENING_SERVICE_ON_CREATE("screening_service_on_create"),
        SCREENING_DEPENDENCIES_READY("screening_dependencies_ready"),
        SCREENING_DECISION_ENGINE_READY("screening_decision_engine_ready"),
        SCREENING_DECISION_BEGIN("screening_decision_begin")
    }
}

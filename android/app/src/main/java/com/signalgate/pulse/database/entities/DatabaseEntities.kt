package com.signalgate.pulse.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SourceEntity represents a data source (local file or remote URL) in the MultiPoint Hub.
 */
@Entity(
    tableName = "sources",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["type"])
    ]
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String, // "CSV", "XLSX", "URL", "MANUAL"
    val pathOrUrl: String,
    val isEnabled: Boolean = true,
    val lastSynced: Long = 0,
    @ColumnInfo(name = "last_attempted_sync")
    val lastAttemptedSync: Long? = null,
    @ColumnInfo(name = "last_accepted_snapshot")
    val lastAcceptedSnapshot: Long? = null,
    val priority: Int = 0,
    val entriesCount: Int = 0,
    val healthStatus: String = "UNKNOWN",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * UnifiedEntryEntity represents a phone number entry with its action and source.
 */
@Entity(
    tableName = "unified_entries",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["phoneNumber", "action"]),
        Index(value = ["sourceId"]),
        Index(value = ["isPattern"]),
        Index(value = ["action"])
    ]
)
data class UnifiedEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val action: String, // "BLOCK", "ALLOW"
    val sourceId: Int,
    val isPattern: Boolean = false,
    val category: String? = null,
    val confidence: Int? = null,
    val riskLevel: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * CallLogEntry is the permanent audit record of every screened call.
 * Written on every BLOCK and ALLOW decision. Never deleted except by
 * explicit purge (deleteOldCallLogs). Not to be confused with PendingCardEntity.
 */
@Entity(
    tableName = "call_log",
    indices = [
        Index(value = ["phoneNumber"]),
        Index(value = ["timestamp"]),
        Index(value = ["decision"])
    ]
)
data class CallLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val normalizedPhoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val decision: String, // "ALLOW", "BLOCK", "SCREEN"
    val spamStatus: String,
    val spamCategory: String? = null,
    val confidence: Int? = null,
    val riskLevel: String? = null,
    val matchedSources: String? = null, // JSON array of source names
    val duration: Int = 0,
    val notes: String? = null
)

/**
 * SettingEntry is the single config store for all app settings.
 * SharedPreferences must not be used for any key defined here.
 */
@Entity(
    tableName = "settings",
    indices = [
        Index(value = ["key"], unique = true)
    ]
)
data class SettingEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val key: String,
    val value: String,
    val type: String = "STRING",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * SyncHistoryEntry tracks the sync history for each source.
 */
@Entity(
    tableName = "sync_history",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["timestamp"])
    ]
)
data class SyncHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sourceId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,
    val entriesAdded: Int = 0,
    val entriesUpdated: Int = 0,
    val entriesRemoved: Int = 0,
    val errorMessage: String? = null,
    val duration: Long = 0
)

/**
 * PendingCardEntity is the short-lived post-call digest queue.
 *
 * Distinct from CallLogEntry:
 * - CallLogEntry = permanent audit record, never auto-deleted
 * - PendingCardEntity = ephemeral UI queue, deleted on card dismissal
 *
 * Created on every BLOCK decision alongside the CallLogEntry.
 * Dismissed = true when user swipes the card. Row deleted on swipe or
 * 'Dismiss All'. Never surfaces in history — that is CallLogEntry's job.
 */
@Entity(
    tableName = "pending_cards",
    indices = [
        Index(value = ["dismissed"]),
        Index(value = ["timestamp"])
    ]
)
data class PendingCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val decision: String,           // "BLOCK"
    val confidence: Int?,
    val decisionSource: String?,    // Which source triggered the block
    val dismissed: Boolean = false
)

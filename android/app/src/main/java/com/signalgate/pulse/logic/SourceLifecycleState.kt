package com.signalgate.pulse.logic

/**
 * Persisted lifecycle states for externally managed security-data sources.
 *
 * These states describe the accepted dataset lifecycle, not merely whether a
 * network request returned successfully. A source with a failed or rejected
 * candidate may remain STALE while its last-known-good entries stay active.
 */
enum class SourceLifecycleState {
    ENABLED,
    SYNCING,
    HEALTHY,
    STALE,
    FAILED,
    REJECTED,
    DISABLED
}

/**
 * Metadata committed only with an accepted snapshot.
 */
data class SnapshotMetadata(
    val version: String? = null,
    val hash: String? = null,
    val acceptedRecordCount: Int
)

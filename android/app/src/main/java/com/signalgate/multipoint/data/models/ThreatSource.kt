package com.signalgate.multipoint.data.models

enum class SourceStatus { HEALTHY, ERROR, DISABLED }
enum class SourceType { REMOTE_URL, LOCAL_CSV }

data class ThreatSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val entriesCount: Int,
    val status: SourceStatus,
    val statusMessage: String,
    val lastSynced: String,
    val isEnabled: Boolean
)

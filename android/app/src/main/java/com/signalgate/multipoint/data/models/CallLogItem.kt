package com.signalgate.multipoint.data.models

enum class CallType { INCOMING, OUTGOING, BLOCKED, SPAM }

data class CallLogItem(
    val id: String,
    val phoneNumber: String,
    val location: String,
    val timestamp: String,
    val type: CallType,
    val matchedSources: List<String>,
    val riskConfidence: Int // Percentage (e.g., 92%)
)

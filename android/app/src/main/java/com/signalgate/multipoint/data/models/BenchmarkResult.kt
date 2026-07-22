package com.signalgate.multipoint.data.models

data class BenchmarkResult(
    val ioReadSpeedMs: Long,
    val availableMemoryMb: Long,
    val storageSpaceGb: Long,
    val isFullThrottleSupported: Boolean,
    val scoreText: String
)

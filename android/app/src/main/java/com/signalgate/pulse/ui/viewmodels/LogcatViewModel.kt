package com.signalgate.pulse.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.pulse.diagnostics.AppLogCapture
import com.signalgate.pulse.diagnostics.AppLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exposes both diagnostic sources at once:
 * - [appLogs] is the live, in-process Timber buffer.
 * - [systemLogs] is the existing filtered Android Logcat snapshot, refreshed on demand.
 */
class LogcatViewModel(
    private val systemLogcatReader: () -> List<String> = ::readSystemLogcat
) : ViewModel() {

    val appLogs: StateFlow<List<AppLogEntry>> = AppLogCapture.entries

    private val _systemLogs = MutableStateFlow<List<String>>(emptyList())
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    /** Refreshes the Android Logcat snapshot; the app buffer continues updating live. */
    fun captureLogcat() {
        viewModelScope.launch {
            _systemLogs.value = withContext(Dispatchers.IO) {
                try {
                    systemLogcatReader()
                } catch (e: Exception) {
                    listOf("Error capturing system Logcat: ${e.message ?: e::class.simpleName}")
                }
            }
        }
    }

    /** Clears only the in-app buffer; Android's system Logcat buffer is independent. */
    fun clearAppLogs() = AppLogCapture.clear()
}

/** Same filtered snapshot path used by the live consumer-v1 viewer. */
internal fun readSystemLogcat(): List<String> = try {
    val process = Runtime.getRuntime().exec("logcat -d -v time SignalGate:*")
    process.inputStream.bufferedReader().use { it.readLines() }.takeLast(500)
} catch (e: Exception) {
    listOf("Error capturing system Logcat: ${e.message ?: e::class.simpleName}")
}

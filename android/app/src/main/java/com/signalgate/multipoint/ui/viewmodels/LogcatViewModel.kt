package com.signalgate.multipoint.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the in-app Logcat viewer.
 * Dev builds only — never expose in release.
 */
class LogcatViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun captureLogcat() {
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v time SignalGate:*")
            val reader = process.inputStream.bufferedReader()
            _logs.value = reader.readLines().takeLast(500)
        } catch (e: Exception) {
            _logs.value = listOf("Error capturing logs: ${e.message}")
        }
    }
}

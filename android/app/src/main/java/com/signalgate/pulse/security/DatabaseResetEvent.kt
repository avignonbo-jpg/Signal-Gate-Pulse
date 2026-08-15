package com.signalgate.pulse.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Signals to the UI layer that the local database was just reset because the
 * Keystore-protected passphrase could no longer be decrypted (see
 * SecureDatabase.getDatabase() and KeystoreInvalidatedException).
 *
 * This is intentionally minimal — a single flag, not a queue — since this event
 * is rare (Keystore invalidation) and only needs to be shown once per occurrence.
 * The UI observes [wasReset] and calls [acknowledge] after showing the notice.
 */
object DatabaseResetEvent {
    private val _wasReset = MutableStateFlow(false)
    val wasReset: StateFlow<Boolean> = _wasReset.asStateFlow()

    fun signal() {
        _wasReset.value = true
    }

    fun acknowledge() {
        _wasReset.value = false
    }
}

package com.signalgate.pulse.security

/**
 * Thrown when a wrapped database passphrase exists in SharedPreferences but cannot
 * be decrypted with the current Android Keystore key (key invalidated — e.g.
 * lock-screen removed on some OEMs — or the stored blob is corrupt).
 *
 * This is distinct from "no passphrase stored yet" (first install), which is
 * expected and handled silently. This case means an existing encrypted database
 * is about to become permanently unreadable, and callers MUST surface that —
 * see SecureDatabase.getDatabase() for the handling (log, delete the orphaned
 * DB file, generate a fresh passphrase, notify the UI layer).
 */
class KeystoreInvalidatedException(cause: Throwable) :
    Exception("Stored database passphrase could not be decrypted — Keystore key invalidated or blob corrupt", cause)

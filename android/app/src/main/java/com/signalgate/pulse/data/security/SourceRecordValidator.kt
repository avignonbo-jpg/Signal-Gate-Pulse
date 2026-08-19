package com.signalgate.pulse.data.security

/**
 * Validates and canonicalizes one externally supplied source record.
 *
 * This is deliberately separate from the bounded parsers: parsers emit raw
 * records and enforce resource limits; this boundary decides whether a raw
 * phone field is a canonical, security-acceptable record. Snapshot activation
 * remains owned by the application/repository layer.
 */
object SourceRecordValidator {
    const val MIN_PHONE_LENGTH = 7
    const val MAX_PHONE_LENGTH = 15

    /**
     * Returns the canonical phone value, or null when the field is blank or
     * outside the accepted length boundary after sanitization.
     */
    fun canonicalizePhone(raw: String?): String? {
        val sanitized = SanitizationEngine.sanitizePhoneNumber(raw)
        val canonical = sanitized.replace(Regex("[^0-9+]"), "")
        return canonical.takeIf { it.length in MIN_PHONE_LENGTH..MAX_PHONE_LENGTH }
    }
}

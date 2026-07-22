package com.signalgate.multipoint.data.security

object SanitizationEngine {
    
    // Strict pattern matching only allowing standard global E.164 phone digits, wildcards, and spaces
    private val SANITIZATION_REGEX = Regex("[^0-9+*#xX\\s]")
    
    /**
     * Sanitizes raw incoming phone string data to strip out SQL injection fragments or malformed buffer blocks.
     */
    fun sanitizePhoneNumber(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) return ""
        // Strip out any characters matching the malicious payload block list
        val clean = rawInput.replace(SANITIZATION_REGEX, "").trim()
        // Enforce an absolute truncation length limit to block buffer exploits or extreme regex processing
        return if (clean.length > 30) clean.substring(0, 30) else clean
    }

    /**
     * Checks text descriptions or source metadata names to strip potentially damaging script tags or SQL escaping.
     */
    fun sanitizeTextField(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) return ""
        return rawInput
            .replace("'", "''") // Protect against manual SQL escaping breakouts
            .replace(";", "")   // Drop command chaining targets
            .replace(Regex("<[^>]*>"), "") // Remove malicious HTML/Script injections
            .trim()
    }
}
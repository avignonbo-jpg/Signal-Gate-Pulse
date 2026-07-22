package com.signalgate.multipoint.utils

object PhoneNumberUtils {

    /**
     * Formats a phone number for display (e.g., +1234567890 -> (123) 456-7890)
     */
    fun formatPhoneNumberForDisplay(phoneNumber: String): String {
        return try {
            // Remove all non-digit characters
            val digits = phoneNumber.replace(Regex("[^0-9]"), "")

            // Format based on length
            when (digits.length) {
                10 -> formatUSNumber(digits)
                11 -> formatInternationalNumber(digits)
                else -> phoneNumber // Return original if format not recognized
            }
        } catch (e: Exception) {
            phoneNumber // Return original on error
        }
    }

    private fun formatUSNumber(digits: String): String {
        return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
    }

    private fun formatInternationalNumber(digits: String): String {
        return "+${digits.substring(0, digits.length - 10)} (${digits.substring(digits.length - 10, digits.length - 7)}) ${digits.substring(digits.length - 7, digits.length - 4)}-${digits.substring(digits.length - 4)}"
    }

    /**
     * Normalizes phone number by removing formatting characters
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }
}

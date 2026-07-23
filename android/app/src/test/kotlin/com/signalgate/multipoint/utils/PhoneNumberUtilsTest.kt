package com.signalgate.multipoint.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberUtilsTest {

    @Test
    fun normalizePhoneNumberRemovesFormattingCorrectly() {
        // These match your real implementation
        assertEquals("+18005551212", PhoneNumberUtils.normalizePhoneNumber("+1-800-555-1212"))
        assertEquals("3105551212", PhoneNumberUtils.normalizePhoneNumber("(310) 555-1212"))
        assertEquals("18005551212", PhoneNumberUtils.normalizePhoneNumber("1.800.555.1212"))
    }

    @Test
    fun formatPhoneNumberForDisplayWorksForUsNumbers() {
        // Matches your formatUSNumber + formatInternationalNumber logic
        assertEquals("+1 (800) 555-1212", PhoneNumberUtils.formatPhoneNumberForDisplay("+18005551212"))
        assertEquals("(310) 555-1212", PhoneNumberUtils.formatPhoneNumberForDisplay("3105551212"))
        
    }
}

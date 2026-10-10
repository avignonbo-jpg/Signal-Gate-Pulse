package com.signalgate.pulse.ui.screens

import com.signalgate.pulse.database.entities.SourceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceRowControlsTest {

    @Test
    fun controlsFor_coversEverySupportedRowType(): Unit {
        val federal = SourceRowControls.controlsFor(
            source = SourceEntity(name = "FTC Do Not Call Registry", type = "FTC", pathOrUrl = "remote"),
            contactCount = 4,
            isFederal = true
        )
        assertEquals(
            SourceRowControlsState(true, false, true, true, false, null),
            federal
        )

        val manual = SourceRowControls.controlsFor(
            source = SourceEntity(name = "Manual User Rules", type = "MANUAL", pathOrUrl = "local"),
            contactCount = 4,
            isFederal = false
        )
        assertEquals(
            SourceRowControlsState(false, true, false, false, false, "Manage elsewhere"),
            manual
        )

        val contacts = SourceRowControls.controlsFor(
            source = SourceEntity(name = "Contacts Allow List", type = "MANUAL", pathOrUrl = "contacts"),
            contactCount = 4,
            isFederal = false
        )
        assertEquals(
            SourceRowControlsState(true, false, false, false, false, "4 contacts imported"),
            contacts
        )

        val contactsEmpty = SourceRowControls.controlsFor(
            source = SourceEntity(name = "Contacts Allow List", type = "MANUAL", pathOrUrl = "contacts"),
            contactCount = 0,
            isFederal = false
        )
        assertEquals(
            SourceRowControlsState(true, false, false, false, false, "No contacts imported yet"),
            contactsEmpty
        )

        val unknown = SourceRowControls.controlsFor(
            source = SourceEntity(name = "Future Source", type = "CSV", pathOrUrl = "file.csv"),
            contactCount = 4,
            isFederal = false
        )
        assertEquals(
            SourceRowControlsState(true, false, false, false, true, null),
            unknown
        )
    }

    @Test
    fun contactsCountLabel_handlesEmptyAndNonEmptyCounts(): Unit {
        assertEquals("No contacts imported yet", contactsCountLabel(0))
        assertEquals("7 contacts imported", contactsCountLabel(7))
    }
}

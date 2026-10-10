package com.signalgate.pulse.logic

import com.signalgate.pulse.database.entities.SourceEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliableSourceManagerPolicyTest {

    @Test
    fun automaticSync_allowsMissingAndEnabledSources_butSkipsDisabledSources() {
        assertTrue(ReliableSourceManager.shouldSyncAutomatically(null))
        assertTrue(
            ReliableSourceManager.shouldSyncAutomatically(
                SourceEntity(name = "Enabled", type = "FTC", pathOrUrl = "test")
            )
        )
        assertFalse(
            ReliableSourceManager.shouldSyncAutomatically(
                SourceEntity(
                    name = "Disabled",
                    type = "FTC",
                    pathOrUrl = "test",
                    isEnabled = false
                )
            )
        )
    }

    @Test
    fun isManagedFederalSource_requiresMatchingTypeAndName() {
        assertTrue(
            ReliableSourceManager.isManagedFederalSource(
                SourceEntity(name = "FTC Do Not Call Registry", type = "FTC", pathOrUrl = "managed-ftc")
            )
        )
        assertTrue(
            ReliableSourceManager.isManagedFederalSource(
                SourceEntity(name = "FCC Consumer Complaints", type = "FCC", pathOrUrl = "managed-fcc")
            )
        )
        assertFalse(
            ReliableSourceManager.isManagedFederalSource(
                SourceEntity(name = "Manual User Rules", type = "MANUAL", pathOrUrl = "local")
            )
        )
        assertFalse(
            ReliableSourceManager.isManagedFederalSource(
                SourceEntity(name = "Contacts Allow List", type = "MANUAL", pathOrUrl = "contacts")
            )
        )
        assertFalse(
            ReliableSourceManager.isManagedFederalSource(
                SourceEntity(name = "Not the FTC row", type = "FTC", pathOrUrl = "managed-ftc")
            )
        )
        assertFalse(
            ReliableSourceManager.isManagedFederalSource(
                SourceEntity(name = "Not the FCC row", type = "FCC", pathOrUrl = "managed-fcc")
            )
        )
    }
}

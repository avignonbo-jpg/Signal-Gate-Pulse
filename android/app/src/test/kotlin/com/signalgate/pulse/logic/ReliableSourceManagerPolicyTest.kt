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
}

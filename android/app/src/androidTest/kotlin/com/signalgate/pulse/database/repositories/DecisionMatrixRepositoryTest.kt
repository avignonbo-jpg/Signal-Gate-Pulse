package com.signalgate.pulse.database.repositories

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 1.1 repository decision-matrix coverage.
 *
 * These vectors exercise the authoritative Room path directly. Bloom is left
 * cold so the assertions test persistence and query semantics rather than a
 * derived-index shortcut.
 */
@RunWith(AndroidJUnit4::class)
class DecisionMatrixRepositoryTest {

    private lateinit var db: SignalGateDatabase
    private lateinit var repository: DataSourceRepository
    private var manualSourceId: Int = 0
    private var federalSourceId: Int = 0

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java).build()
        repository = DataSourceRepository(
            db.sourceDao(),
            db.unifiedEntryDao(),
            BloomFilterEngine(),
            BloomFilterEngine()
        )
        manualSourceId = db.sourceDao().insertSource(
            SourceEntity(name = "Manual", type = "MANUAL", pathOrUrl = "local", priority = 100)
        ).toInt()
        federalSourceId = db.sourceDao().insertSource(
            SourceEntity(name = "Federal", type = "FTC", pathOrUrl = "remote", priority = 85)
        ).toInt()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun manualAllow_overridesExternalBlock_bySourcePriority() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+15551234567", action = "BLOCK", sourceId = federalSourceId, metadata = "external block")
        )
        insert(
            UnifiedEntryEntity(phoneNumber = "+15551234567", action = "ALLOW", sourceId = manualSourceId, metadata = "manual allow")
        )

        val decision = repository.getCallDecision("(555) 123-4567")

        assertEquals("ALLOW", decision.action)
        assertEquals("manual_allow", decision.source)
    }

    @Test
    fun higherPriorityBlock_winsAgainstLowerPriorityBlock() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+15551234567", action = "BLOCK", sourceId = federalSourceId, metadata = "lower priority")
        )
        val higherPriorityId = db.sourceDao().insertSource(
            SourceEntity(name = "Higher Federal", type = "FCC", pathOrUrl = "remote2", priority = 90)
        ).toInt()
        insert(
            UnifiedEntryEntity(phoneNumber = "+15551234567", action = "BLOCK", sourceId = higherPriorityId, metadata = "higher priority")
        )

        val decision = repository.getCallDecision("+15551234567")

        assertEquals("BLOCK", decision.action)
        assertEquals("higher priority", decision.reason)
    }

    @Test
    fun exactMatch_precedesPatternMatch() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+1555", action = "BLOCK", sourceId = federalSourceId, isPattern = true, metadata = "pattern")
        )
        insert(
            UnifiedEntryEntity(phoneNumber = "+15551234567", action = "ALLOW", sourceId = manualSourceId, metadata = "exact allow")
        )

        val decision = repository.getCallDecision("+15551234567")

        assertEquals("ALLOW", decision.action)
        assertEquals("manual_allow", decision.source)
    }

    @Test
    fun patternMatch_blocksMatchingPrefix() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+1900", action = "BLOCK", sourceId = federalSourceId, isPattern = true)
        )

        val decision = repository.getCallDecision("+19005551234")

        assertEquals("BLOCK", decision.action)
        assertEquals("pattern", decision.source)
    }

    @Test
    fun disabledExactAndPatternSources_areIgnored() = runBlocking {
        val disabledId = db.sourceDao().insertSource(
            SourceEntity(name = "Disabled", type = "FTC", pathOrUrl = "disabled", isEnabled = false, priority = 100)
        ).toInt()
        insert(
            UnifiedEntryEntity(phoneNumber = "+15551234567", action = "BLOCK", sourceId = disabledId, metadata = "disabled exact")
        )
        insert(
            UnifiedEntryEntity(phoneNumber = "+1900", action = "BLOCK", sourceId = disabledId, isPattern = true, metadata = "disabled pattern")
        )

        val exactDecision = repository.getCallDecision("+15551234567")
        val patternDecision = repository.getCallDecision("+19005551234")

        assertEquals("default", exactDecision.source)
        assertEquals("default", patternDecision.source)
        assertEquals("ALLOW", exactDecision.action)
        assertEquals("ALLOW", patternDecision.action)
    }

    @Test
    fun malformedOrEmptyInput_defaultsToAllowWithoutRule() = runBlocking {
        val malformed = repository.getCallDecision("not-a-phone-number")
        val empty = repository.getCallDecision("")

        assertEquals("default", malformed.source)
        assertEquals("default", empty.source)
        assertEquals("ALLOW", malformed.action)
        assertEquals("ALLOW", empty.action)
    }

    private suspend fun insert(entry: UnifiedEntryEntity) {
        db.unifiedEntryDao().insertEntry(entry)
    }
}

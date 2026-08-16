package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.SourceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Phase 0.3 (Security Control-Plane Integrity — source lifecycle semantics,
 * Architecture Contract §7 / INV-008).
 *
 * Proves DataSourceRepository.deleteSource() refuses a delete against a
 * protected source (MANUAL, and by extension the Contacts source which is
 * also seeded with type = "MANUAL" — see DatabaseInitializer; plus FTC/FCC
 * federal sources) and never reaches SourceDao.deleteSource() for those
 * cases. Also proves a non-protected (future user-created) source type is
 * still deletable, since the guard must not become a blanket delete-block.
 *
 * Plain JVM unit test — SourceDao is a Room-generated interface (mockable
 * directly, no Robolectric/instrumentation needed) and BloomFilterEngine is
 * pure JVM (java.util.BitSet), so no Android framework dependency exists on
 * this path.
 */
class DataSourceRepositoryDeletionTest {

    private lateinit var sourceDao: SourceDao
    private lateinit var entryDao: UnifiedEntryDao
    private lateinit var repository: DataSourceRepository

    @Before
    fun setUp() {
        sourceDao = mock()
        entryDao = mock()
        repository = DataSourceRepository(
            sourceDao = sourceDao,
            entryDao = entryDao,
            bloomFilter = BloomFilterEngine(),
            patternBloomFilter = BloomFilterEngine()
        )
    }

    private fun protectedSource(type: String, name: String) = SourceEntity(
        id = 1,
        name = name,
        type = type,
        pathOrUrl = "local",
        isEnabled = true,
        priority = 100
    )

    @Test
    fun deleteSource_refusesManualSource() = runBlocking {
        val manual = protectedSource("MANUAL", "Manual User Rules")

        assertThrows(ProtectedSourceDeletionException::class.java) {
            runBlocking { repository.deleteSource(manual) }
        }
        verifyNoInteractions(sourceDao)
    }

    /**
     * The Contacts Allow List source is seeded with type = "MANUAL" too (not
     * a distinct type) — see DatabaseInitializer.seedRequiredSources(). This
     * test exists specifically so the guard is proven against that source by
     * name, not just assumed to work because it happens to share a type
     * string with the first test above.
     */
    @Test
    fun deleteSource_refusesContactsSource() = runBlocking {
        val contacts = protectedSource("MANUAL", "Contacts Allow List")

        assertThrows(ProtectedSourceDeletionException::class.java) {
            runBlocking { repository.deleteSource(contacts) }
        }
        verifyNoInteractions(sourceDao)
    }

    @Test
    fun deleteSource_refusesFtcSource() = runBlocking {
        val ftc = protectedSource("FTC", "FTC Do Not Call Registry")

        assertThrows(ProtectedSourceDeletionException::class.java) {
            runBlocking { repository.deleteSource(ftc) }
        }
        verifyNoInteractions(sourceDao)
    }

    @Test
    fun deleteSource_refusesFccSource() = runBlocking {
        val fcc = protectedSource("FCC", "FCC Consumer Complaints")

        assertThrows(ProtectedSourceDeletionException::class.java) {
            runBlocking { repository.deleteSource(fcc) }
        }
        verifyNoInteractions(sourceDao)
    }

    /**
     * Negative case — the guard must not become a blanket delete-block. A
     * future user-created source (any type outside PROTECTED_SOURCE_TYPES,
     * e.g. a CSV/XLSX/URL source per the contract) must still delete
     * successfully, reaching the DAO exactly once.
     */
    @Test
    fun deleteSource_allowsNonProtectedSource() = runBlocking {
        val userSource = protectedSource("CSV", "Community Blocklist Mirror")

        repository.deleteSource(userSource)

        verify(sourceDao).deleteSource(userSource)
    }

    @Test
    fun protectedSourceTypes_containsExactlyManualFtcFcc() {
        assertEquals(
            setOf("MANUAL", "FTC", "FCC"),
            DataSourceRepository.PROTECTED_SOURCE_TYPES
        )
    }
}

package com.signalgate.pulse.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseInitializerTest {

    private lateinit var database: SignalGateDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedRequiredSources_insertsManualBeforeContactsWithDistinctIds(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DatabaseInitializer.seedRequiredSources(
            context = context,
            sourceDao = database.sourceDao(),
            settingDao = database.settingDao()
        )

        val manual = database.sourceDao().getSourceByName("Manual User Rules")
        val contacts = database.sourceDao().getSourceByName("Contacts Allow List")
        assertEquals("local", manual?.pathOrUrl)
        assertEquals("contacts", contacts?.pathOrUrl)
        assertEquals(1, manual?.id)
        assertEquals(2, contacts?.id)
        assertNotEquals(manual?.id, contacts?.id)

        assertEquals("1", database.settingDao().getSettingByKey("manual_source_id")?.value)
        assertEquals("2", database.settingDao().getSettingByKey("contacts_source_id")?.value)
    }
}

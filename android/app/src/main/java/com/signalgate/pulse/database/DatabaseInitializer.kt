package com.signalgate.pulse.database

import android.content.Context
import com.signalgate.pulse.database.daos.SettingDao
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.entities.SettingEntry
import com.signalgate.pulse.database.entities.SourceEntity

/**
 * Idempotent first-install seeding for required SourceEntity rows.
 * After seeding, stores both sourceIds in SettingEntry so repositories and
 * ViewModels can retrieve them without re-querying on every operation.
 * Must be called from MainApplication.onCreate() after Koin starts, before
 * any repository module that depends on these IDs is resolved.
 */
object DatabaseInitializer {

    suspend fun seedRequiredSources(
        context: Context,
        sourceDao: SourceDao,
        settingDao: SettingDao
    ) {
        val manualId = ensureSourceExists(
            sourceDao = sourceDao,
            name = "Manual User Rules",
            type = "MANUAL",
            pathOrUrl = "local",
            priority = 100
        )
        storeSourceId(settingDao, "manual_source_id", manualId)

        val contactsId = ensureSourceExists(
            sourceDao = sourceDao,
            name = "Contacts Allow List",
            type = "MANUAL",
            pathOrUrl = "contacts",
            priority = 100
        )
        storeSourceId(settingDao, "contacts_source_id", contactsId)
    }

    private suspend fun ensureSourceExists(
        sourceDao: SourceDao,
        name: String,
        type: String,
        pathOrUrl: String,
        priority: Int
    ): Int {
        val existing = sourceDao.getSourceByName(name)
        if (existing != null) return existing.id

        val source = SourceEntity(
            name = name,
            type = type,
            pathOrUrl = pathOrUrl,
            isEnabled = true,
            priority = priority
        )
        return sourceDao.insertSource(source).toInt()
    }

    private suspend fun storeSourceId(settingDao: SettingDao, key: String, id: Int) {
        val existing = settingDao.getSettingByKey(key)
        if (existing != null) {
            settingDao.updateSettingValue(key, id.toString())
        } else {
            settingDao.insertSetting(SettingEntry(key = key, value = id.toString(), type = "INT"))
        }
    }
}

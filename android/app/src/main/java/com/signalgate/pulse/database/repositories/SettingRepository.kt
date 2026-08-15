package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.database.daos.SettingDao
import com.signalgate.pulse.database.entities.SettingEntry
import timber.log.Timber

/**
 * SettingRepository — thin repository wrapper over SettingDao (Contract §5.5).
 * No DAO imports outside this file.
 *
 * Created alongside the architecture drift-detection lint (Roadmap Step 0.4):
 * the new UI -> DAO check found ContactsViewModel was injected with SettingDao
 * directly. This repository closes that gap and also gives the Phase 4.4
 * SharedPreferences -> SettingEntry migration a home to read/write through.
 */
class SettingRepository(private val settingDao: SettingDao) {

    suspend fun getSettingValue(key: String): String? = settingDao.getSettingValue(key)

    suspend fun getSettingByKey(key: String): SettingEntry? = settingDao.getSettingByKey(key)

    suspend fun getAllSettings(): List<SettingEntry> = settingDao.getAllSettings()

    /**
     * Upserts a setting: updates the row if [key] already exists, otherwise
     * inserts a new one. Callers don't need to know which case applies.
     */
    suspend fun setSetting(key: String, value: String) {
        try {
            val existing = settingDao.getSettingByKey(key)
            if (existing != null) {
                settingDao.updateSettingValue(key, value)
            } else {
                settingDao.insertSetting(SettingEntry(key = key, value = value))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set setting: $key")
            throw e
        }
    }
}

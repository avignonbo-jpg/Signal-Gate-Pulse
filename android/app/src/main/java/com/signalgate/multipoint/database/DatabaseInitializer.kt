package com.signalgate.multipoint.database

import android.content.Context
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.daos.SourceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Idempotent first-install seeding for required SourceEntity rows.
 * Must be called early (e.g. in Application onCreate or first DB access).
 */
object DatabaseInitializer {

    suspend fun seedRequiredSources(context: Context, sourceDao: SourceDao) = withContext(Dispatchers.IO) {
        // MANUAL source - for user-created block/allow rules
        ensureSourceExists(
            sourceDao = sourceDao,
            name = "Manual User Rules",
            type = "MANUAL",
            pathOrUrl = "local",
            priority = 100
        )

        // Contacts Allow List source
        ensureSourceExists(
            sourceDao = sourceDao,
            name = "Contacts Allow List",
            type = "MANUAL",
            pathOrUrl = "contacts",
            priority = 100
        )
    }

    private suspend fun ensureSourceExists(
        sourceDao: SourceDao,
        name: String,
        type: String,
        pathOrUrl: String,
        priority: Int
    ) {
        val existing = sourceDao.getSourceByName(name)
        if (existing != null) return

        val source = SourceEntity(
            name = name,
            type = type,
            pathOrUrl = pathOrUrl,
            isEnabled = true,
            priority = priority
        )
        sourceDao.insertSource(source)
    }
}

package com.signalgate.logic
import android.content.Context
import com.signalgate.sources.ReliableSourceManager
import com.signalgate.database.BlocklistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Layer 3: DataSyncEngine
 * Orchestrates public reliable sources → sanitization → Room persistence (Layer 2)
 * References: Architecture-Contract.md (Layers 2+3 boundary)
 */
class DataSyncEngine(private val context: Context) : KoinComponent {

    private val repository: BlocklistRepository by inject()

    suspend fun syncPublicSources(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting public sources sync")
            val rawNumbers = ReliableSourceManager.fetchReliableBlocklist(context)

            // Sanitize before entering Layer 2
            val sanitized = rawNumbers
                .mapNotNull { PublicSourceParser.sanitizeNumber(it) }
                .filter { it.isNotEmpty() && it.length >= 10 }
                .distinct()

            repository.insertPublicBlocklist(sanitized)

            Timber.i("Synced ${sanitized.size} numbers from public sources")
            SyncResult.Success(sanitized.size)
        } catch (e: Exception) {
            Timber.e(e, "Public sources sync failed")
            SyncResult.Failure(e)
        }
    }

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Failure(val error: Exception) : SyncResult()
    }
}

package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.database.daos.PendingCardDao
import com.signalgate.pulse.database.entities.PendingCardEntity
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * PendingCardRepository (Phase 1.4) — Clean repository wrapper per §5.5.
 * No DAO imports outside this file. Full CRUD with validation + logging.
 */
class PendingCardRepository(private val pendingCardDao: PendingCardDao) {

    fun getUndismissedCards(): Flow<List<PendingCardEntity>> = pendingCardDao.getUndismissedCards()

    fun getUndismissedCount(): Flow<Int> = pendingCardDao.getUndismissedCount()

    suspend fun insertCard(card: PendingCardEntity): Long {
        require(card.phoneNumber.isNotBlank()) { "Phone number required for PendingCard" }
        return try {
            pendingCardDao.insertCard(card).also {
                Timber.d("Inserted PendingCard for ${card.phoneNumber} (id=$it)")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert PendingCard")
            throw e
        }
    }

    suspend fun dismissCard(cardId: Int) {
        require(cardId > 0) { "Invalid cardId" }
        pendingCardDao.dismissCard(cardId)
    }

    suspend fun dismissByPhoneNumber(phoneNumber: String) {
        val normalized = phoneNumber.replace(Regex("[^0-9+]"), "")
        pendingCardDao.dismissByPhoneNumber(normalized)
    }

    suspend fun deleteAll() {
        pendingCardDao.deleteAll()
    }
}

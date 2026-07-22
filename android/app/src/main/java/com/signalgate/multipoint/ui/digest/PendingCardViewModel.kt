package com.signalgate.multipoint.ui.digest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.entities.PendingCardEntity
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.PendingCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel backing Screen.Digest — the blocked call review queue.
 *
 * Exposes the live [undismissedCards] Flow from [PendingCardRepository]. The UI
 * observes this and recomposes automatically as cards are dismissed or added.
 *
 * Three actions:
 *   [dismissCard]  — marks one card dismissed (user swipe or explicit dismiss tap).
 *   [dismissAll]   — clears the entire queue (Dismiss All button).
 *   [markAsNotSpam] — allowlists the number via BlocklistRepository, then dismisses
 *                     the card. Single tap from the "Not Spam" button on each card
 *                     or from the inline notification RemoteAction.
 *
 * Injected via Koin: PendingCardRepository (get()), BlocklistRepository (get()).
 * Registered in AppModule.kt viewModelModule.
 *
 * Fixed per architecture drift-detection lint (Roadmap Step 0.4): this
 * previously injected PendingCardDao directly, violating the UI -> DAO
 * layer boundary — PendingCardRepository already existed and wraps it.
 */
class PendingCardViewModel(
    private val pendingCardRepository: PendingCardRepository,
    private val blocklistRepository: BlocklistRepository
) : ViewModel() {

    val undismissedCards: Flow<List<PendingCardEntity>> =
        pendingCardRepository.getUndismissedCards()

    val undismissedCount: Flow<Int> =
        pendingCardRepository.getUndismissedCount()

    fun dismissCard(cardId: Int) {
        viewModelScope.launch {
            pendingCardRepository.dismissCard(cardId)
        }
    }

    fun dismissAll() {
        viewModelScope.launch {
            pendingCardRepository.deleteAll()
        }
    }

    /**
     * Overturn a heuristic block decision:
     *   1. Allowlists the phone number so future calls ring through.
     *   2. Dismisses the card from the digest queue.
     *
     * The reason string surfaces in BlocklistRepository audit records and in
     * the [UnifiedEntryEntity] notes field for traceability.
     */
    fun markAsNotSpam(card: PendingCardEntity) {
        viewModelScope.launch {
            blocklistRepository.addAllowRule(
                phoneNumber = card.phoneNumber,
                reason = "Not Spam — user overturn via digest"
            )
            pendingCardRepository.dismissCard(card.id)
        }
    }
}

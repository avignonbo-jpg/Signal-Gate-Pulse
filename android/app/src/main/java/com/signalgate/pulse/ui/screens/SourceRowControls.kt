package com.signalgate.pulse.ui.screens

import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository

internal data class SourceRowControlsState(
    val showSwitch: Boolean,
    val showReadOnlyStatus: Boolean,
    val showSyncNow: Boolean,
    val showSyncMetadata: Boolean,
    val showRemove: Boolean,
    val hintText: String?
)

internal object SourceRowControls {
    fun controlsFor(
        source: SourceEntity,
        contactCount: Int,
        isFederal: Boolean
    ): SourceRowControlsState {
        if (isFederal) {
            return SourceRowControlsState(
                showSwitch = true,
                showReadOnlyStatus = false,
                showSyncNow = true,
                showSyncMetadata = true,
                showRemove = false,
                hintText = null
            )
        }

        if (source.type == "MANUAL" && source.pathOrUrl == "local") {
            return SourceRowControlsState(
                showSwitch = false,
                showReadOnlyStatus = true,
                showSyncNow = false,
                showSyncMetadata = false,
                showRemove = false,
                hintText = "Manage elsewhere"
            )
        }

        if (source.type == "MANUAL" && source.pathOrUrl == "contacts") {
            return SourceRowControlsState(
                showSwitch = true,
                showReadOnlyStatus = false,
                showSyncNow = false,
                showSyncMetadata = false,
                showRemove = false,
                hintText = contactsCountLabel(contactCount)
            )
        }

        return SourceRowControlsState(
            showSwitch = true,
            showReadOnlyStatus = false,
            showSyncNow = false,
            showSyncMetadata = false,
            showRemove = source.type !in DataSourceRepository.PROTECTED_SOURCE_TYPES,
            hintText = null
        )
    }
}

internal fun contactsCountLabel(count: Int): String =
    if (count > 0) "$count contacts imported" else "No contacts imported yet"

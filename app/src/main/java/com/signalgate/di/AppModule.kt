package com.signalgate.di

import android.content.Context
import com.signalgate.database.BlocklistRepository
import com.signalgate.database.SignalGateDatabase
import com.signalgate.logic.DataSyncEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    single { SignalGateDatabase.getDatabase(androidContext()) }
    single { get<SignalGateDatabase>().blocklistDao() }
    single { BlocklistRepository(get()) }
    single { DataSyncEngine(androidContext()) }
}

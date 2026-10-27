package com.wenjin.magnetplayer.di

import com.wenjin.magnetplayer.home.HomeViewModel
import com.wenjin.magnetplayer.torrent.TorrServerManager
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.torrent.TorrentRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val TorrentModule = module {
  single { TorrentPreferences(get()) }
  single { TorrServerManager(androidContext(), get()) }
  single { TorrentRepository(get(), get()) }
  viewModelOf(::HomeViewModel)
}

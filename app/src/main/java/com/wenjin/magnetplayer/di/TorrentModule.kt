package com.wenjin.magnetplayer.di

import androidx.room.Room
import com.wenjin.magnetplayer.data.MagnetDatabase
import com.wenjin.magnetplayer.data.RecentRepository
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

  single { Room.databaseBuilder(androidContext(), MagnetDatabase::class.java, "magnet.db").build() }
  single { get<MagnetDatabase>().recentPlayDao() }
  single { RecentRepository(get()) }

  viewModelOf(::HomeViewModel)
}

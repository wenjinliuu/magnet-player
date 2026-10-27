package com.wenjin.magnetplayer

import android.app.Application
import com.wenjin.magnetplayer.di.PreferencesModule
import com.wenjin.magnetplayer.di.TorrentModule
import com.wenjin.magnetplayer.di.domainModule
import com.wenjin.magnetplayer.presentation.crash.CrashActivity
import com.wenjin.magnetplayer.presentation.crash.GlobalExceptionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@App)
      modules(PreferencesModule, domainModule, TorrentModule)
    }
    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))
  }
}

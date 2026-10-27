package com.wenjin.magnetplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.wenjin.magnetplayer.home.HomePage
import com.wenjin.magnetplayer.preferences.AppearancePreferences
import com.wenjin.magnetplayer.preferences.preference.collectAsState
import com.wenjin.magnetplayer.torrent.TorrServerManager
import com.wenjin.magnetplayer.ui.theme.DarkMode
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()
  private val torrServer by inject<TorrServerManager>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val darkMode by appearancePreferences.darkMode.collectAsState()
      val dark = darkMode == DarkMode.Dark || (darkMode == DarkMode.System && isSystemInDarkTheme())
      enableEdgeToEdge(
        SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
        SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
      )
      MagnetTheme(dark) {
        HomePage()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // 退出 App 时停止 TorrServer，内存缓存随之清空
    if (isFinishing) torrServer.stop()
  }
}

package com.wenjin.magnetplayer

import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()
  private val torrServer by inject<TorrServerManager>()

  /** 从外部打开的 magnet: 链接 / .torrent 文件 */
  private val incoming = MutableStateFlow<Intent?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) incoming.value = intent.takeIf { it.data != null || it.hasExtra(Intent.EXTRA_TEXT) }
    setContent {
      val darkMode by appearancePreferences.darkMode.collectAsState()
      val dark = darkMode == DarkMode.Dark || (darkMode == DarkMode.System && isSystemInDarkTheme())
      enableEdgeToEdge(
        SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
        SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
      )
      MagnetTheme(dark) {
        HomePage(incoming, onIncomingHandled = { incoming.value = null })
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    incoming.value = intent
  }

  override fun onDestroy() {
    super.onDestroy()
    // 退出 App 时停止 TorrServer，内存缓存随之清空
    if (isFinishing) torrServer.stop()
  }
}

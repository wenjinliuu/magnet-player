package com.wenjin.magnetplayer

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.wenjin.magnetplayer.data.RecentPlay
import com.wenjin.magnetplayer.home.HomeMain
import com.wenjin.magnetplayer.home.HomeTab
import com.wenjin.magnetplayer.home.NavRail
import com.wenjin.magnetplayer.home.RecentPage
import com.wenjin.magnetplayer.home.SettingsPage
import com.wenjin.magnetplayer.preferences.AppearancePreferences
import com.wenjin.magnetplayer.home.ResolveUi
import com.wenjin.magnetplayer.preferences.preference.AndroidPreferenceStore
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 本地截图：用设计稿里的占位数据渲染界面，输出到 app/build/screenshots，和 docs/design 对比。
 * 运行：./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp-land-mdpi", application = Application::class)
class HomeScreenshotTest {
  @get:Rule
  val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

  private val gb = 1024L * 1024 * 1024

  private val info = TorrentInfo(
    hash = "3f9c",
    link = "magnet:?xt=urn:btih:3f9c",
    title = "示例视频合集 [2160p]",
    totalSize = (18.6 * gb).toLong(),
    peers = 46,
    files = listOf(
      TorrentFile(1, "示例视频合集/示例视频 Part 1 2160p HEVC.mkv", (8.2 * gb).toLong(), 0),
      TorrentFile(2, "示例视频合集/示例视频 Part 2 2160p HEVC.mkv", (7.9 * gb).toLong(), 0),
      TorrentFile(3, "示例视频合集/花絮 1080p H.264.mp4", (1.4 * gb).toLong(), 0),
      TorrentFile(4, "示例视频合集/中文字幕.ass", 86 * 1024, 0),
    ),
  )

  private val recent = listOf(
    RecentPlay("a", 1, "", "", "示例视频 Part 1.mkv", 0, 2530.0, 7080.0, 3),
    RecentPlay("b", 1, "", "", "示例视频 B.mkv", 0, 4865.0, 6225.0, 2),
    RecentPlay("c", 1, "", "", "示例视频 C.mkv", 0, 512.0, 6592.0, 1),
  )

  @Test fun homeLight() = shot("home-light", dark = false)

  @Test fun homeDark() = shot("home-dark", dark = true)

  @Test fun homeEmpty() = shot("home-empty", dark = false, state = ResolveUi.Idle, items = emptyList())

  @Test fun settingsLight() = page("settings-light", dark = false, HomeTab.Settings) { prefs ->
    SettingsPage(prefs, {}, {})
  }

  @Test fun recentDark() = page("recent-dark", dark = true, HomeTab.Recent) {
    RecentPage(recent + recent.map { it.copy(hash = it.hash + "2") }, {}, {}, {})
  }

  private fun page(name: String, dark: Boolean, tab: HomeTab, content: @Composable (TorrentPreferences) -> Unit) {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val store = AndroidPreferenceStore(context)
    val prefs = TorrentPreferences(store)
    stopKoin()
    startKoin { modules(module { single { AppearancePreferences(store) } }) }
    compose.setContent {
      MagnetTheme(dark) {
        Row(
          Modifier
            .size(1280.dp, 800.dp)
            .background(MagnetTheme.colors.background),
        ) {
          NavRail(tab, onSelect = {})
          content(prefs)
        }
      }
    }
    capture(name)
    stopKoin()
  }

  private fun shot(
    name: String,
    dark: Boolean,
    state: ResolveUi = ResolveUi.Ready(info, 46),
    items: List<RecentPlay> = recent,
  ) {
    val prefs = TorrentPreferences(AndroidPreferenceStore(ApplicationProvider.getApplicationContext()))
    compose.setContent {
      MagnetTheme(dark) {
        Home(state, items, prefs)
      }
    }
    capture(name)
  }

  private fun capture(name: String) {
    compose.waitForIdle()
    val view = compose.activity.window.decorView.rootView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    compose.runOnIdle { view.draw(android.graphics.Canvas(bitmap)) }
    save(bitmap, name)
  }

  @Composable
  private fun Home(state: ResolveUi, items: List<RecentPlay>, prefs: TorrentPreferences) {
    Row(
      Modifier
        .size(1280.dp, 800.dp)
        .background(MagnetTheme.colors.background),
    ) {
      NavRail(HomeTab.Home, onSelect = {})
      androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        HomeMain(
          link = "magnet:?xt=urn:btih:3f9c…e21a&dn=示例视频合集",
          resolve = state,
          recent = items,
          tipDismissed = false,
          preferences = prefs,
          onLinkChange = {}, onPaste = {}, onPickTorrent = {}, onResolve = {},
          onOpenRecent = {}, onDeleteRecent = {}, onShowAllRecent = {}, onDismissTip = {},
          onPlay = { _, _ -> }, onAutoClearChange = {}, onCacheSizeChange = {},
        )
      }
    }
  }

  private fun save(bitmap: Bitmap, name: String) {
    val dir = File("build/screenshots").apply { mkdirs() }
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }
}

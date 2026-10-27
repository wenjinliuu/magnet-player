package com.wenjin.magnetplayer

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.wenjin.magnetplayer.player.AspectMode
import com.wenjin.magnetplayer.player.MediaTrack
import com.wenjin.magnetplayer.player.PlaybackUi
import com.wenjin.magnetplayer.player.PlayerActions
import com.wenjin.magnetplayer.player.PlayerInfo
import com.wenjin.magnetplayer.player.PlayerScreen
import com.wenjin.magnetplayer.player.SubtitleSize
import com.wenjin.magnetplayer.player.SubtitleUi
import com.wenjin.magnetplayer.player.TorrentUi
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 播放页截图（设计稿占位数据），输出到 app/build/screenshots */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w1280dp-h800dp-land-mdpi", application = Application::class)
class PlayerScreenshotTest {
  @get:Rule
  val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

  private val mb = 1024L * 1024

  private val subs = SubtitleUi(
    tracks = listOf(
      MediaTrack(1, "sub", "中文字幕.ass", "zh", "ass", true, "http://127.0.0.1:18090/subs/x/1?track=4", true),
      MediaTrack(2, "sub", null, "ja", "subrip", false, null, false),
    ),
    sid = 1,
    secondarySid = 2,
  )

  private val actions = object : PlayerActions {
    override fun back() {}
    override fun togglePause() {}
    override fun seekBy(seconds: Double) {}
    override fun seekTo(seconds: Double) {}
    override fun setSpeed(speed: Double) {}
    override fun selectSubtitle(track: MediaTrack) {}
    override fun subtitleOff() {}
    override fun onlineSubtitleSearch() {}
    override fun pickLocalSubtitle() {}
    override fun setBilingual(enabled: Boolean) {}
    override fun setSubtitleSize(size: SubtitleSize) {}
    override fun setSubtitleDelay(seconds: Double) {}
    override fun setSubtitleRaised(raised: Boolean) {}
    override fun selectAudio(track: MediaTrack) {}
    override fun setAspect(mode: AspectMode) {}
    override fun setHardwareDecoding(enabled: Boolean) {}
    override fun enterPip() {}
    override fun adjustBrightness(delta: Float) = 0.5f
    override fun adjustVolume(delta: Float) = 0.5f
  }

  private fun render() {
    compose.setContent {
      MagnetTheme(dark = true) {
        Box(
          Modifier
            .size(1280.dp, 800.dp)
            .background(Brush.radialGradient(listOf(Color(0xFF33394A), Color(0xFF151820), Color.Black), radius = 900f)),
        ) {
          PlayerScreen(
            info = PlayerInfo("示例视频 Part 1", "示例视频合集", "3f9c", (8.2 * 1024 * mb).toLong()),
            playback = PlaybackUi(
              paused = true, position = 2530.0, duration = 7080.0, buffering = false,
              videoWidth = 3840, videoHeight = 2160, videoCodec = "hevc", hwdec = "mediacodec",
            ),
            torrent = TorrentUi(
              downloadSpeed = 8.4 * mb, peers = 46, cacheFilled = 186 * mb, cacheCapacity = 300 * mb,
              downloaded = listOf(0f..0.41f, 0.61f..0.68f),
            ),
            subtitles = subs,
            audioTracks = emptyList(),
            subtitleSize = SubtitleSize.Normal,
            bilingual = true,
            aspect = AspectMode.Fit,
            inPip = false,
            actions = actions,
          )
        }
      }
    }
  }

  @Test fun controls() {
    render()
    capture("player-controls")
  }

  @Test fun subtitlePanel() {
    render()
    compose.onNodeWithContentDescription("字幕").performClick()
    compose.mainClock.advanceTimeBy(600)
    capture("player-subtitles")
  }

  private fun capture(name: String) {
    compose.waitForIdle()
    val view = compose.activity.window.decorView.rootView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    compose.runOnIdle { view.draw(android.graphics.Canvas(bitmap)) }
    val dir = File("build/screenshots").apply { mkdirs() }
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }
}

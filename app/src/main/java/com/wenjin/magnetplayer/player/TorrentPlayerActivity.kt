package com.wenjin.magnetplayer.player

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.wenjin.magnetplayer.data.RecentRepository
import com.wenjin.magnetplayer.databinding.PlayerLayoutBinding
import com.wenjin.magnetplayer.preferences.preference.collectAsState as collectPrefAsState
import com.wenjin.magnetplayer.torrent.SubTrack
import com.wenjin.magnetplayer.torrent.TorrentRepository
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

/**
 * 磁力视频播放页：沉浸式全屏，libmpv 播放 TorrServer 的流地址，Compose 绘制控件。
 * 复用 mpvEx 的 MPVView（mpv 初始化选项、解码设置等）。
 */
class TorrentPlayerActivity : ComponentActivity(), PlayerActions {
  private val torrentRepository: TorrentRepository by inject()
  private val recentRepository: RecentRepository by inject()
  private val uiPreferences: PlayerUiPreferences by inject()

  private lateinit var binding: PlayerLayoutBinding
  private lateinit var controller: PlayerController
  private var mpvReady = false

  private val hash: String? get() = intent.getStringExtra(PlayerLauncher.EXTRA_TORRENT_HASH)
  private val fileId: Int get() = intent.getIntExtra(PlayerLauncher.EXTRA_FILE_ID, 0)

  private var inPip by mutableStateOf(false)
  private var aspectMode by mutableStateOf(AspectMode.Fit)
  private var torrentSubtitlesLoaded = false

  private val audioManager by lazy { getSystemService(AudioManager::class.java) }
  private var audioFocus: AudioFocusRequest? = null
  private var volumeLevel = -1f

  private val pickSubtitle = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(::addLocalSubtitle)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    hideSystemBars()
    volumeControlStream = AudioManager.STREAM_MUSIC

    binding = PlayerLayoutBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val url = intent.data?.toString()
    if (url == null) {
      finish()
      return
    }
    val fileLength = intent.getLongExtra(PlayerLauncher.EXTRA_FILE_LENGTH, 0)
    val info = PlayerInfo(
      title = intent.getStringExtra("title").orEmpty(),
      torrentTitle = intent.getStringExtra(PlayerLauncher.EXTRA_TORRENT_TITLE).orEmpty(),
      hash = hash,
      fileLength = fileLength,
    )

    controller = PlayerController(torrentRepository.api, hash, fileLength)
    setupMpv()
    controller.onFileLoaded = ::onFileLoaded
    controller.onEndOfFile = ::saveProgress
    controller.observe()

    // 从上次的位置开始（start 选项，单位秒）
    intent.getDoubleExtra(PlayerLauncher.EXTRA_START, 0.0).takeIf { it > 1 }?.let {
      MPVLib.setPropertyString("start", it.toString())
    }
    MPVLib.setPropertyString("force-media-title", info.title)
    binding.player.playFile(url)

    binding.controls.setContent {
      val playback by controller.playback.collectAsState()
      val torrent by controller.torrent.collectAsState()
      val subtitles by controller.subtitles.collectAsState()
      val audio by controller.audioTracks.collectAsState()
      val size by uiPreferences.subtitleSize.collectPrefAsState()
      val bilingual by uiPreferences.bilingual.collectPrefAsState()
      // 播放页永远深色
      MagnetTheme(dark = true) {
        PlayerScreen(
          info = info,
          playback = playback,
          torrent = torrent,
          subtitles = subtitles,
          audioTracks = audio,
          subtitleSize = size,
          bilingual = bilingual,
          aspect = aspectMode,
          inPip = inPip,
          actions = this,
        )
      }
    }

    requestAudioFocus()
    // 每 15 秒保存一次进度，防止被系统回收时丢失
    lifecycleScope.launch {
      while (isActive) {
        delay(15_000)
        saveProgress()
      }
    }
  }

  private fun setupMpv() {
    runCatching { Utils.copyAssets(this) }.onFailure { Log.w(TAG, "复制 mpv 资源失败", it) }
    binding.player.initialize(filesDir.path, cacheDir.path)
    mpvReady = true
    // 边下边播：数据可能要等节点下载，读超时放宽
    MPVLib.setPropertyString("network-timeout", "120")
    // 视频内嵌字幕优先选中文
    MPVLib.setPropertyString("slang", "zh,chi,zho,chs,sc,zh-Hans,cht,tc,zh-Hant")
    MPVLib.setPropertyString("sub-auto", "no")
    Mpv.applySubtitleStyle(uiPreferences.subtitleSize.get())
  }

  private fun hideSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
      hide(WindowInsetsCompat.Type.systemBars())
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) hideSystemBars()
  }

  // ---------------------------------------------------------------- 字幕

  private fun onFileLoaded() {
    if (torrentSubtitlesLoaded) return
    torrentSubtitlesLoaded = true
    val h = hash
    lifecycleScope.launch {
      // 种子内自带的外挂字幕：TorrServer /subs 列出，mpv sub-add 加载；有中文就选中文
      val tracks = if (h != null) torrentRepository.api.subtitles(h, fileId) else emptyList()
      val embeddedChinese = controller.subtitles.value.tracks.any { !it.external && isChinese(it.lang, it.title) }
      val best = tracks.firstOrNull { isChinese(it.lang, it.name) && it.format == "ass" }
        ?: tracks.firstOrNull { isChinese(it.lang, it.name) }
      withContext(Dispatchers.IO) {
        tracks.forEach { t ->
          runCatching {
            Mpv.addSubtitle(
              url = torrentRepository.api.subtitleUrl(t),
              select = t == best,
              title = t.name,
              lang = subtitleLang(t),
            )
          }.onFailure { Log.w(TAG, "加载字幕失败：${t.name}", it) }
        }
      }
      // 没有中文字幕时，至少选中一条
      if (best == null && !embeddedChinese && controller.subtitles.value.sid <= 0) {
        delay(300)
        controller.subtitles.value.tracks.firstOrNull()?.let { Mpv.selectSubtitle(it.id) }
      }
      delay(500)
      updateSecondarySubtitle()
    }
  }

  private fun subtitleLang(t: SubTrack): String? = when {
    isChinese(t.lang, t.name) -> "zh"
    languageName(t.lang) == "日语" || Regex("(^|[._ \\[(-])(jp|jpn|ja)([._ \\])-]|$)").containsMatchIn(t.name.lowercase()) -> "ja"
    languageName(t.lang) == "英语" || Regex("(^|[._ \\[(-])(en|eng)([._ \\])-]|$)").containsMatchIn(t.name.lowercase()) -> "en"
    else -> t.lang.takeIf { it.isNotBlank() && it != "und" }
  }

  /** 双语：主字幕是中文时，副字幕选一条非中文的文本字幕（优先日语），显示在中文上方 */
  private fun updateSecondarySubtitle() {
    val state = controller.subtitles.value
    if (!uiPreferences.bilingual.get()) {
      Mpv.selectSecondarySubtitle(-1)
      return
    }
    val primary = state.tracks.firstOrNull { it.id == state.sid }
    if (primary == null || !isChinese(primary.lang, primary.title ?: primary.externalFilename)) {
      Mpv.selectSecondarySubtitle(-1)
      return
    }
    val textual = state.tracks.filter { it.id != primary.id && !isImageSubtitle(it) && !isChinese(it.lang, it.title ?: it.externalFilename) }
    val pick = textual.firstOrNull { languageName(it.lang) == "日语" }
      ?: textual.firstOrNull { languageName(it.lang) == "英语" }
      ?: textual.firstOrNull()
    Mpv.selectSecondarySubtitle(pick?.id ?: -1)
  }

  private fun isImageSubtitle(t: MediaTrack) =
    t.codec?.lowercase()?.let { "pgs" in it || "dvd" in it || "dvb" in it || "vobsub" in it } == true

  private fun addLocalSubtitle(uri: Uri) {
    lifecycleScope.launch {
      runCatching {
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
          if (it.moveToFirst()) it.getString(0) else null
        } ?: "字幕.srt"
        // 复制到缓存目录再加载（播放页关闭时清理）
        val file = withContext(Dispatchers.IO) {
          val dir = File(cacheDir, "subs").apply { mkdirs() }
          File(dir, name).also { f -> contentResolver.openInputStream(uri)!!.use { input -> f.outputStream().use(input::copyTo) } }
        }
        withContext(Dispatchers.IO) { Mpv.addSubtitle(file.path, true, name, null) }
        delay(300)
        updateSecondarySubtitle()
      }.onFailure { toast("加载字幕失败：${it.message}") }
    }
  }

  override fun selectSubtitle(track: MediaTrack) {
    val state = controller.subtitles.value
    if (track.id == state.secondarySid) Mpv.selectSecondarySubtitle(-1)
    Mpv.selectSubtitle(track.id)
    lifecycleScope.launch {
      delay(200)
      updateSecondarySubtitle()
    }
  }

  override fun subtitleOff() {
    Mpv.selectSubtitle(-1)
    Mpv.selectSecondarySubtitle(-1)
  }

  override fun onlineSubtitleSearch() = toast("在线搜索字幕（伪射手、迅雷）将在下一版加入")

  override fun pickLocalSubtitle() {
    pickSubtitle.launch(arrayOf("application/x-subrip", "text/x-ssa", "text/vtt", "text/plain", "application/octet-stream", "*/*"))
  }

  override fun setBilingual(enabled: Boolean) {
    uiPreferences.bilingual.set(enabled)
    updateSecondarySubtitle()
    if (enabled && controller.subtitles.value.secondarySid <= 0) {
      lifecycleScope.launch {
        delay(200)
        if (controller.subtitles.value.secondarySid <= 0) toast("没有可以和中文一起显示的原文字幕")
      }
    }
  }

  override fun setSubtitleSize(size: SubtitleSize) {
    uiPreferences.subtitleSize.set(size)
    Mpv.applySubtitleStyle(size)
  }

  override fun setSubtitleDelay(seconds: Double) = Mpv.setSubtitleDelay(seconds)

  override fun setSubtitleRaised(raised: Boolean) {
    if (mpvReady) Mpv.setSubtitleRaised(raised)
  }

  // ---------------------------------------------------------------- 播放控制

  override fun back() = finish()

  override fun togglePause() = Mpv.setPause(!controller.playback.value.paused)

  override fun seekBy(seconds: Double) = Mpv.seekBy(seconds)

  override fun seekTo(seconds: Double) = Mpv.seekTo(seconds)

  override fun setSpeed(speed: Double) = Mpv.setSpeed(speed)

  override fun selectAudio(track: MediaTrack) = Mpv.selectAudio(track.id)

  override fun setAspect(mode: AspectMode) {
    aspectMode = mode
    Mpv.setAspect(mode)
  }

  override fun setHardwareDecoding(enabled: Boolean) = Mpv.setHardwareDecoding(enabled)

  override fun enterPip() {
    val p = controller.playback.value
    val ratio = if (p.videoWidth > 0 && p.videoHeight > 0) {
      Rational(p.videoWidth, p.videoHeight).let {
        // 系统限制宽高比在 1:2.39 到 2.39:1 之间
        when {
          it.toFloat() > 2.39f -> Rational(239, 100)
          it.toFloat() < 1 / 2.39f -> Rational(100, 239)
          else -> it
        }
      }
    } else {
      Rational(16, 9)
    }
    runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build()) }
      .onFailure { toast("这台设备不支持画中画") }
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    inPip = isInPictureInPictureMode
  }

  override fun adjustBrightness(delta: Float): Float {
    val attrs = window.attributes
    val current = if (attrs.screenBrightness in 0f..1f) {
      attrs.screenBrightness
    } else {
      runCatching { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f }.getOrDefault(0.5f)
    }
    val next = (current + delta).coerceIn(0.01f, 1f)
    attrs.screenBrightness = next
    window.attributes = attrs
    return next
  }

  override fun adjustVolume(delta: Float): Float {
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    if (volumeLevel < 0) volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * max).toInt(), 0)
    return volumeLevel
  }

  private fun requestAudioFocus() {
    val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
          .build(),
      )
      .setOnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
          if (mpvReady) Mpv.setPause(true)
        }
      }
      .build()
    audioFocus = request
    audioManager.requestAudioFocus(request)
  }

  // ---------------------------------------------------------------- 进度与生命周期

  private fun saveProgress() {
    if (!mpvReady) return
    val p = controller.playback.value
    if (p.duration <= 0 && p.position <= 0) return
    PlayerLauncher.recentFrom(intent.extras, p.position, p.duration)?.let(recentRepository::saveAsync)
  }

  override fun onStop() {
    super.onStop()
    saveProgress()
    // 切到后台就暂停（画中画除外）
    if (mpvReady && !isInPictureInPictureMode) Mpv.setPause(true)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (!mpvReady) return
    saveProgress()
    controller.release()
    audioFocus?.let(audioManager::abandonAudioFocusRequest)
    if (isFinishing) {
      // 关闭播放：按"看完自动清空"设置移除种子，内存缓存随之释放；清掉本地加载的字幕副本
      hash?.let(torrentRepository::onPlaybackClosedAsync)
      File(cacheDir, "subs").deleteRecursively()
    }
    runCatching {
      MPVLib.setPropertyBoolean("pause", true)
      MPVLib.command("quit")
      // 给 mpv 内部线程一点时间退出，再销毁（与 mpvEx 的做法一致）
      Thread.sleep(100)
      binding.player.destroy()
    }.onFailure { Log.e(TAG, "销毁 mpv 出错", it) }
    mpvReady = false
  }

  private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

  companion object {
    private const val TAG = "TorrentPlayer"
  }
}

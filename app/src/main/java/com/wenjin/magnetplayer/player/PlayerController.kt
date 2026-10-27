package com.wenjin.magnetplayer.player

import android.util.Log
import com.wenjin.magnetplayer.torrent.CacheState
import com.wenjin.magnetplayer.torrent.TorrServerApi
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** mpv 里的一条音轨 / 字幕轨（track-list 的一项，字段名见 mpv 手册 track-list） */
data class MediaTrack(
  val id: Int,
  val type: String,
  val title: String?,
  val lang: String?,
  val codec: String?,
  val external: Boolean,
  val externalFilename: String?,
  val selected: Boolean,
) {
  val isSubtitle: Boolean get() = type == "sub"
  val isAudio: Boolean get() = type == "audio"
}

data class PlaybackUi(
  val paused: Boolean = false,
  val position: Double = 0.0,
  val duration: Double = 0.0,
  /** 等待数据（paused-for-cache）或刚开始还没出画面 */
  val buffering: Boolean = true,
  val speed: Double = 1.0,
  val videoWidth: Int = 0,
  val videoHeight: Int = 0,
  val videoCodec: String? = null,
  val hwdec: String? = null,
  val eof: Boolean = false,
)

data class TorrentUi(
  val downloadSpeed: Double = 0.0,
  val peers: Int = 0,
  val cacheFilled: Long = 0,
  val cacheCapacity: Long = 0,
  /** 已下载片段，按文件进度 0..1 表示 */
  val downloaded: List<ClosedFloatingPointRange<Float>> = emptyList(),
)

data class SubtitleUi(
  val tracks: List<MediaTrack> = emptyList(),
  val sid: Int = -1,
  val secondarySid: Int = -1,
  val delay: Double = 0.0,
)

/**
 * 播放状态：观察 mpv 属性（名称以 mpv 官方手册 DOCS/man/input.rst 为准），
 * 并轮询 TorrServer 的速度、节点、缓存分片。
 */
class PlayerController(
  private val api: TorrServerApi,
  private val hash: String?,
  /** 当前文件在种子里的大小，用来把分片映射成进度 */
  private val fileLength: Long,
) : MPVLib.EventObserver {
  private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
  private val json = Json { ignoreUnknownKeys = true }

  private val _playback = MutableStateFlow(PlaybackUi())
  val playback: StateFlow<PlaybackUi> = _playback.asStateFlow()

  private val _torrent = MutableStateFlow(TorrentUi())
  val torrent: StateFlow<TorrentUi> = _torrent.asStateFlow()

  private val _subtitles = MutableStateFlow(SubtitleUi())
  val subtitles: StateFlow<SubtitleUi> = _subtitles.asStateFlow()

  private val _audioTracks = MutableStateFlow<List<MediaTrack>>(emptyList())
  val audioTracks: StateFlow<List<MediaTrack>> = _audioTracks.asStateFlow()

  /** 文件在种子数据中的起始偏移（估算值），用来把 TorrServer 的分片映射到当前文件 */
  private var fileOffsetEstimate: Long? = null

  var onFileLoaded: (() -> Unit)? = null
  var onEndOfFile: (() -> Unit)? = null

  fun observe() {
    MPVLib.addObserver(this)
    OBSERVED.forEach { (name, format) -> MPVLib.observeProperty(name, format) }
    scope.launch { pollTorrent() }
    scope.launch { pollMpvCache() }
  }

  fun release() {
    MPVLib.removeObserver(this)
    scope.cancel()
  }

  private fun CoroutineScope.cancel() = coroutineContext[Job]?.cancel()

  // ---------------------------------------------------------------- mpv 回调（在 mpv 的事件线程）

  override fun eventProperty(property: String) {
    when (property) {
      "track-list" -> scope.launch { refreshTracks() }
    }
  }

  override fun eventProperty(property: String, value: Long) {
    when (property) {
      "video-params/w" -> _playback.update { it.copy(videoWidth = value.toInt()) }
      "video-params/h" -> _playback.update { it.copy(videoHeight = value.toInt()) }
      "sid" -> _subtitles.update { it.copy(sid = value.toInt()) }
      "secondary-sid" -> _subtitles.update { it.copy(secondarySid = value.toInt()) }
    }
  }

  override fun eventProperty(property: String, value: Boolean) {
    when (property) {
      "pause" -> _playback.update { it.copy(paused = value) }
      "paused-for-cache" -> _playback.update { it.copy(buffering = value) }
      "eof-reached" -> {
        _playback.update { it.copy(eof = value) }
        if (value) scope.launch { onEndOfFile?.invoke() }
      }
    }
  }

  override fun eventProperty(property: String, value: String) {
    when (property) {
      "hwdec-current" -> _playback.update { it.copy(hwdec = value) }
      // sid / secondary-sid 为 "no" 时以字符串形式回调
      "sid" -> _subtitles.update { it.copy(sid = value.toIntOrNull() ?: -1) }
      "secondary-sid" -> _subtitles.update { it.copy(secondarySid = value.toIntOrNull() ?: -1) }
    }
  }

  override fun eventProperty(property: String, value: Double) {
    when (property) {
      "time-pos" -> _playback.update { it.copy(position = value) }
      "duration" -> _playback.update { it.copy(duration = value) }
      "speed" -> _playback.update { it.copy(speed = value) }
      "sub-delay" -> _subtitles.update { it.copy(delay = value) }
    }
  }

  override fun eventProperty(property: String, value: MPVNode) = Unit

  override fun event(eventId: Int, data: MPVNode) {
    when (eventId) {
      MPV_EVENT_FILE_LOADED -> scope.launch {
        _playback.update { it.copy(buffering = false) }
        refreshTracks()
        onFileLoaded?.invoke()
      }
      MPV_EVENT_PLAYBACK_RESTART -> _playback.update { it.copy(buffering = false) }
    }
  }

  // ---------------------------------------------------------------- 轨道

  /** track-list 取字符串时 mpv 返回 JSON */
  private suspend fun refreshTracks() {
    val text = withContext(Dispatchers.IO) { runCatching { MPVLib.getPropertyString("track-list") }.getOrNull() } ?: return
    val tracks = runCatching {
      json.parseToJsonElement(text).jsonArray.map { el ->
        val o = el.jsonObject
        MediaTrack(
          id = o.int("id") ?: 0,
          type = o.str("type").orEmpty(),
          title = o.str("title"),
          lang = o.str("lang"),
          codec = o.str("codec"),
          external = o.bool("external") ?: false,
          externalFilename = o.str("external-filename"),
          selected = o.bool("selected") ?: false,
        )
      }
    }.getOrElse {
      Log.w(TAG, "解析 track-list 失败", it)
      return
    }
    _subtitles.update { it.copy(tracks = tracks.filter(MediaTrack::isSubtitle)) }
    _audioTracks.value = tracks.filter(MediaTrack::isAudio)
    // 视频编码：取当前视频轨的 codec（例如 hevc、h264）
    tracks.firstOrNull { it.type == "video" && it.selected }?.codec?.let { codec ->
      _playback.update { it.copy(videoCodec = codec) }
    }
  }

  // ---------------------------------------------------------------- 已下载片段

  /** TorrServer：速度、节点、缓存分片（每秒一次） */
  private suspend fun pollTorrent() {
    val hash = hash ?: return
    while (scope.isActive) {
      val status = runCatching { api.get(hash) }.getOrNull()
      val cache = api.cache(hash)
      val ranges = cache?.let { torrentRanges(it) }
      _torrent.update {
        it.copy(
          downloadSpeed = status?.downloadSpeed ?: it.downloadSpeed,
          peers = status?.totalPeers ?: it.peers,
          cacheFilled = cache?.filled ?: it.cacheFilled,
          cacheCapacity = cache?.capacity ?: it.cacheCapacity,
          downloaded = ranges ?: it.downloaded,
        )
      }
      delay(1000)
    }
  }

  /**
   * 把 TorrServer 缓存里的分片映射为当前文件的进度区间。
   *
   * /cache 接口不直接给出文件在种子中的偏移；但 Readers[].Reader = (文件内读取位置 + 文件偏移) / 分片长度，
   * 再结合 mpv 的 stream-pos（当前读到的文件字节位置）就能估算偏移，误差在一个分片以内。
   */
  private suspend fun torrentRanges(cache: CacheState): List<ClosedFloatingPointRange<Float>>? {
    if (cache.piecesLength <= 0 || fileLength <= 0) return null
    val pl = cache.piecesLength
    val offset = fileOffsetEstimate ?: estimateOffset(cache)?.also { fileOffsetEstimate = it } ?: return null
    val firstPiece = (offset / pl).toInt()
    val lastPiece = ((offset + fileLength - 1) / pl).toInt()
    val done = cache.pieces.values
      .filter { it.id in firstPiece..lastPiece && (it.completed || it.size >= it.length) }
      .map { it.id }
      .sorted()
    if (done.isEmpty()) return emptyList()
    val result = mutableListOf<ClosedFloatingPointRange<Float>>()
    var start = done.first()
    var prev = start
    fun add(a: Int, b: Int) {
      val from = ((a.toLong() * pl - offset).coerceAtLeast(0).toFloat() / fileLength).coerceIn(0f, 1f)
      val to = (((b + 1).toLong() * pl - offset).coerceAtMost(fileLength).toFloat() / fileLength).coerceIn(0f, 1f)
      if (to > from) result += from..to
    }
    for (p in done.drop(1)) {
      if (p != prev + 1) {
        add(start, prev)
        start = p
      }
      prev = p
    }
    add(start, prev)
    return result
  }

  private suspend fun estimateOffset(cache: CacheState): Long? {
    // 单文件种子：偏移就是 0
    if (cache.torrentSize > 0 && cache.torrentSize == fileLength) return 0
    val reader = cache.readers.singleOrNull() ?: return null
    val streamPos = withContext(Dispatchers.IO) {
      runCatching { MPVLib.getPropertyString("stream-pos")?.toLongOrNull() }.getOrNull()
    } ?: return null
    val estimate = reader.reader.toLong() * cache.piecesLength - streamPos
    return estimate.coerceAtLeast(0)
  }

  /** 拿不到 TorrServer 分片时，退回 mpv demuxer-cache-state 的 seekable-ranges（每秒一次） */
  private suspend fun pollMpvCache() {
    while (scope.isActive) {
      delay(1000)
      if (_torrent.value.downloaded.isNotEmpty() && fileOffsetEstimate != null) continue
      val duration = _playback.value.duration.takeIf { it > 0 } ?: continue
      val text = withContext(Dispatchers.IO) {
        runCatching { MPVLib.getPropertyString("demuxer-cache-state") }.getOrNull()
      } ?: continue
      val ranges = runCatching {
        (json.parseToJsonElement(text).jsonObject["seekable-ranges"] as? JsonArray).orEmpty().mapNotNull { r ->
          val o = r.jsonObject
          val s = o.dbl("start") ?: return@mapNotNull null
          val e = o.dbl("end") ?: return@mapNotNull null
          (s / duration).toFloat().coerceIn(0f, 1f)..(e / duration).toFloat().coerceIn(0f, 1f)
        }
      }.getOrNull() ?: continue
      _torrent.update { it.copy(downloaded = ranges) }
    }
  }

  /** 目标位置是否已下载（拖动预览用） */
  fun isDownloaded(fraction: Float): Boolean = _torrent.value.downloaded.any { fraction in it }

  private fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.takeIf { it.isString }?.content
  private fun JsonObject.int(k: String) = this[k]?.jsonPrimitive?.intOrNull
  private fun JsonObject.bool(k: String) = this[k]?.jsonPrimitive?.booleanOrNull
  private fun JsonObject.dbl(k: String) = this[k]?.jsonPrimitive?.doubleOrNull

  companion object {
    private const val TAG = "PlayerController"

    // mpv/client.h 的 mpv_event_id
    const val MPV_EVENT_FILE_LOADED = 8
    const val MPV_EVENT_PLAYBACK_RESTART = 21

    private val OBSERVED = listOf(
      "time-pos" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "duration" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "pause" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "paused-for-cache" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "eof-reached" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "speed" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "video-params/w" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "video-params/h" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "hwdec-current" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "track-list" to MPVLib.MpvFormat.MPV_FORMAT_NONE,
      "sid" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "secondary-sid" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "sub-delay" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
    )
  }
}

package com.wenjin.magnetplayer.torrent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TorrServer `/torrents` 接口返回的种子状态（字段名对应 server/torr/state/state.go 的 json 标签）。
 */
@Serializable
data class TorrentStatus(
  val title: String = "",
  val name: String = "",
  val hash: String = "",
  val stat: Int = 0,
  @SerialName("stat_string") val statString: String = "",
  @SerialName("loaded_size") val loadedSize: Long = 0,
  @SerialName("torrent_size") val torrentSize: Long = 0,
  @SerialName("preloaded_bytes") val preloadedBytes: Long = 0,
  @SerialName("download_speed") val downloadSpeed: Double = 0.0,
  @SerialName("total_peers") val totalPeers: Int = 0,
  @SerialName("active_peers") val activePeers: Int = 0,
  @SerialName("connected_seeders") val connectedSeeders: Int = 0,
  @SerialName("file_stats") val fileStats: List<TorrentFileStat> = emptyList(),
) {
  /** TorrentStat：0 已添加，1 获取信息中，2 预加载，3 工作中，4 已关闭，5 在数据库中 */
  val hasInfo: Boolean get() = fileStats.isNotEmpty()
}

/** 种子内的文件，id 从 1 开始（TorrServer 网页里 0 表示未定义） */
@Serializable
data class TorrentFileStat(
  val id: Int = 0,
  val path: String = "",
  val length: Long = 0,
)

/** `/subs/{hash}/{id}` 返回的外挂字幕（server/subs/match.go 的 Track + url） */
@Serializable
data class SubTrack(
  val index: Int = 0,
  val name: String = "",
  val title: String = "",
  val lang: String = "",
  val format: String = "",
  val url: String = "",
)

/** `/cache` 接口返回的缓存状态（server/torr/storage/state/state.go，没有 json 标签，字段名即 Go 字段名） */
@Serializable
data class CacheState(
  @SerialName("Hash") val hash: String = "",
  @SerialName("Capacity") val capacity: Long = 0,
  @SerialName("Filled") val filled: Long = 0,
  @SerialName("PiecesLength") val piecesLength: Long = 0,
  @SerialName("PiecesCount") val piecesCount: Int = 0,
  @SerialName("Pieces") val pieces: Map<String, CachePiece> = emptyMap(),
)

@Serializable
data class CachePiece(
  @SerialName("Id") val id: Int = 0,
  @SerialName("Length") val length: Long = 0,
  @SerialName("Size") val size: Long = 0,
  @SerialName("Completed") val completed: Boolean = false,
  @SerialName("Priority") val priority: Int = 0,
)

enum class FileKind { Video, Subtitle, Other }

/** 首页和播放页用的文件信息 */
data class TorrentFile(
  val id: Int,
  val path: String,
  val length: Long,
  /** 文件在整个种子数据里的起始偏移，用来把分片映射到文件进度 */
  val offset: Long,
) {
  val name: String get() = path.substringAfterLast('/')
  val extension: String get() = name.substringAfterLast('.', "").lowercase()
  val kind: FileKind
    get() = when (extension) {
      in VIDEO_EXTENSIONS -> FileKind.Video
      in SUBTITLE_EXTENSIONS -> FileKind.Subtitle
      else -> FileKind.Other
    }

  /** 从文件名猜测编码和分辨率，例如 "HEVC · 2160p" */
  val codecAndResolution: String
    get() = listOfNotNull(MediaNameParser.codec(name), MediaNameParser.resolution(name)).joinToString(" · ")

  companion object {
    val VIDEO_EXTENSIONS = setOf(
      "mkv", "mp4", "m4v", "avi", "mov", "webm", "ts", "m2ts", "mts", "wmv", "flv", "rmvb", "rm", "mpg", "mpeg", "vob", "3gp", "ogv",
    )
    val SUBTITLE_EXTENSIONS = setOf("srt", "ass", "ssa", "vtt", "sub", "sup", "idx")
  }
}

data class TorrentInfo(
  val hash: String,
  /** 重新打开这个种子用的磁力链接 */
  val link: String,
  val title: String,
  val totalSize: Long,
  val peers: Int,
  val files: List<TorrentFile>,
) {
  /** 视频在前、按文件名排序；字幕其次；其他文件最后 */
  val sortedFiles: List<TorrentFile>
    get() = files.sortedWith(compareBy<TorrentFile> { it.kind.ordinal }.thenBy(NaturalOrder) { it.name })

  val videos: List<TorrentFile> get() = sortedFiles.filter { it.kind == FileKind.Video }
  val subtitles: List<TorrentFile> get() = sortedFiles.filter { it.kind == FileKind.Subtitle }
  val others: List<TorrentFile> get() = sortedFiles.filter { it.kind == FileKind.Other }

  companion object {
    fun from(status: TorrentStatus, link: String?): TorrentInfo {
      var offset = 0L
      // file_stats 按种子内文件顺序排列（id = 序号 + 1），累加得到每个文件的偏移
      val files = status.fileStats.sortedBy { it.id }.map { fs ->
        TorrentFile(fs.id, fs.path.replace('\\', '/'), fs.length, offset).also { offset += fs.length }
      }
      val title = status.title.ifBlank { status.name }
      return TorrentInfo(
        hash = status.hash,
        link = link ?: "magnet:?xt=urn:btih:${status.hash}&dn=${android.net.Uri.encode(title)}",
        title = title,
        totalSize = status.torrentSize.takeIf { it > 0 } ?: files.sumOf { it.length },
        peers = status.totalPeers,
        files = files,
      )
    }
  }
}

/** 文件名自然排序：Part 2 排在 Part 10 前面 */
object NaturalOrder : Comparator<String> {
  private val chunk = Regex("\\d+|\\D+")

  override fun compare(a: String, b: String): Int {
    val ca = chunk.findAll(a.lowercase()).map { it.value }.toList()
    val cb = chunk.findAll(b.lowercase()).map { it.value }.toList()
    for (i in 0 until minOf(ca.size, cb.size)) {
      val x = ca[i]
      val y = cb[i]
      val r = if (x[0].isDigit() && y[0].isDigit()) {
        x.trimStart('0').length.compareTo(y.trimStart('0').length).takeIf { it != 0 }
          ?: x.trimStart('0').compareTo(y.trimStart('0'))
      } else {
        x.compareTo(y)
      }
      if (r != 0) return r
    }
    return ca.size.compareTo(cb.size)
  }
}

object MediaNameParser {
  fun resolution(name: String): String? {
    val n = name.lowercase()
    return when {
      Regex("2160p|4k|uhd").containsMatchIn(n) -> "2160p"
      "1440p" in n -> "1440p"
      "1080" in n -> "1080p"
      "720p" in n -> "720p"
      "576p" in n -> "576p"
      "480p" in n -> "480p"
      else -> null
    }
  }

  fun codec(name: String): String? {
    val n = name.lowercase()
    return when {
      Regex("hevc|[hx]\\.?265").containsMatchIn(n) -> "HEVC"
      Regex("\\bav1\\b").containsMatchIn(n) -> "AV1"
      Regex("avc|[hx]\\.?264").containsMatchIn(n) -> "H.264"
      "vp9" in n -> "VP9"
      else -> null
    }
  }
}

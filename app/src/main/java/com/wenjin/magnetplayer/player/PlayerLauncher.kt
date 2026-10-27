package com.wenjin.magnetplayer.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.wenjin.magnetplayer.data.RecentPlay
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.player.PlayerActivity

/** 一次播放请求：种子、文件、流地址和起始位置 */
data class PlayRequest(
  val info: TorrentInfo,
  val file: TorrentFile,
  val url: String,
  val startPosition: Double,
)

object PlayerLauncher {
  const val EXTRA_TORRENT_HASH = "torrent_hash"
  const val EXTRA_FILE_ID = "torrent_file_id"
  const val EXTRA_LINK = "torrent_link"
  const val EXTRA_TORRENT_TITLE = "torrent_title"
  const val EXTRA_FILE_NAME = "torrent_file_name"
  const val EXTRA_FILE_LENGTH = "torrent_file_length"
  const val EXTRA_START = "start_position"

  fun play(context: Context, request: PlayRequest) {
    val intent = Intent(context, PlayerActivity::class.java)
      .setAction(Intent.ACTION_VIEW)
      .setData(request.url.toUri())
      .putExtra("title", Format.title(request.file.name))
      .putExtra(EXTRA_TORRENT_HASH, request.info.hash)
      .putExtra(EXTRA_FILE_ID, request.file.id)
      .putExtra(EXTRA_LINK, request.info.link)
      .putExtra(EXTRA_TORRENT_TITLE, request.info.title)
      .putExtra(EXTRA_FILE_NAME, request.file.name)
      .putExtra(EXTRA_FILE_LENGTH, request.file.length)
      .putExtra(EXTRA_START, request.startPosition)
    context.startActivity(intent)
  }

  /** 从 Intent 还原出保存最近播放需要的信息 */
  fun recentFrom(extras: Bundle?, position: Double, duration: Double): RecentPlay? {
    extras ?: return null
    val hash = extras.getString(EXTRA_TORRENT_HASH) ?: return null
    return RecentPlay(
      hash = hash,
      fileId = extras.getInt(EXTRA_FILE_ID),
      link = extras.getString(EXTRA_LINK).orEmpty(),
      torrentTitle = extras.getString(EXTRA_TORRENT_TITLE).orEmpty(),
      fileName = extras.getString(EXTRA_FILE_NAME).orEmpty(),
      fileLength = extras.getLong(EXTRA_FILE_LENGTH),
      position = position,
      duration = duration,
      updatedAt = System.currentTimeMillis(),
    )
  }
}

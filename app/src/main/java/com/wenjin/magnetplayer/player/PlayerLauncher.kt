package com.wenjin.magnetplayer.player

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.player.PlayerActivity

object PlayerLauncher {
  const val EXTRA_TORRENT_HASH = "torrent_hash"
  const val EXTRA_FILE_ID = "torrent_file_id"

  fun play(context: Context, info: TorrentInfo, file: TorrentFile, streamUrl: String) {
    val intent = Intent(context, PlayerActivity::class.java)
      .setAction(Intent.ACTION_VIEW)
      .setData(streamUrl.toUri())
      .putExtra("title", Format.title(file.name))
      .putExtra(EXTRA_TORRENT_HASH, info.hash)
      .putExtra(EXTRA_FILE_ID, file.id)
    context.startActivity(intent)
  }
}

package com.wenjin.magnetplayer.home

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wenjin.magnetplayer.player.PlayerLauncher
import com.wenjin.magnetplayer.torrent.TorrentRepository
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomePage(viewModel: HomeViewModel = koinViewModel()) {
  val c = MagnetTheme.colors
  val context = LocalContext.current
  val link by viewModel.link.collectAsState()
  val resolve by viewModel.resolve.collectAsState()
  val message by viewModel.messages.collectAsState()
  var tab by rememberSaveable { mutableStateOf(HomeTab.Home) }

  LaunchedEffect(message) {
    message?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.consumeMessage()
    }
  }

  val pickTorrent = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri ?: return@rememberLauncherForActivityResult
    runCatching {
      val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
      viewModel.resolveTorrentFile(bytes, displayName(context, uri) ?: "种子文件.torrent")
    }.onFailure { viewModel.showMessage("读取种子文件失败：${it.message}") }
  }

  Row(
    Modifier
      .fillMaxSize()
      .background(c.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    NavRail(tab, onSelect = { tab = it })
    Row(
      Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(start = 36.dp, top = 36.dp, end = 40.dp, bottom = 36.dp),
      horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
      HomeLeftColumn(
        link = link,
        onLinkChange = viewModel::onLinkChange,
        onPaste = {
          val text = clipboardText(context)
          if (text.isNullOrBlank()) {
            viewModel.showMessage("剪贴板是空的")
          } else {
            viewModel.onLinkChange(text.trim())
            if (TorrentRepository.isTorrentLink(text)) viewModel.resolveLink()
          }
        },
        onPickTorrent = { pickTorrent.launch(arrayOf("application/x-bittorrent", "application/octet-stream", "*/*")) },
        onResolve = viewModel::resolveLink,
        modifier = Modifier
          .weight(1.22f)
          .fillMaxHeight(),
      )
      ResolveCard(
        state = resolve,
        preferences = viewModel.preferences,
        onPlay = { info, file ->
          viewModel.play(info, file) { url -> PlayerLauncher.play(context, info, file, url) }
        },
        onAutoClearChange = viewModel::setAutoClear,
        onCacheSizeChange = viewModel::setCacheSize,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
      )
    }
  }
}

private fun clipboardText(context: Context): String? {
  val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
  return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun displayName(context: Context, uri: Uri): String? =
  context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
    if (it.moveToFirst()) it.getString(0) else null
  }

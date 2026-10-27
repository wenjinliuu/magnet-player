package com.wenjin.magnetplayer.home

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.wenjin.magnetplayer.data.RecentPlay
import com.wenjin.magnetplayer.player.PlayerLauncher
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.torrent.TorrentRepository
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel
import com.wenjin.magnetplayer.preferences.preference.collectAsState as collectPrefAsState

/** 首页主体：左栏（输入卡、最近播放、提示条）+ 右栏（解析结果） */
@Composable
fun HomeMain(
  link: String,
  resolve: ResolveUi,
  recent: List<RecentPlay>,
  tipDismissed: Boolean,
  preferences: TorrentPreferences,
  onLinkChange: (String) -> Unit,
  onPaste: () -> Unit,
  onPickTorrent: () -> Unit,
  onResolve: () -> Unit,
  onOpenRecent: (RecentPlay) -> Unit,
  onDeleteRecent: (RecentPlay) -> Unit,
  onShowAllRecent: () -> Unit,
  onDismissTip: () -> Unit,
  onPlay: (TorrentInfo, TorrentFile) -> Unit,
  onAutoClearChange: (Boolean) -> Unit,
  onCacheSizeChange: (Int) -> Unit,
) {
  Row(
    Modifier
      .fillMaxSize()
      .padding(start = 36.dp, top = 36.dp, end = 40.dp, bottom = 36.dp),
    horizontalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    HomeLeftColumn(
      link = link,
      onLinkChange = onLinkChange,
      onPaste = onPaste,
      onPickTorrent = onPickTorrent,
      onResolve = onResolve,
      modifier = Modifier
        .weight(1.22f)
        .fillMaxHeight(),
    ) {
      RecentSection(items = recent, onOpen = onOpenRecent, onDelete = onDeleteRecent, onShowAll = onShowAllRecent)
      if (!tipDismissed) SeekTip(onDismiss = onDismissTip)
    }
    ResolveCard(
      state = resolve,
      preferences = preferences,
      onPlay = onPlay,
      onAutoClearChange = onAutoClearChange,
      onCacheSizeChange = onCacheSizeChange,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    )
  }
}

@Composable
fun HomePage(
  incoming: StateFlow<Intent?>,
  onIncomingHandled: () -> Unit,
  viewModel: HomeViewModel = koinViewModel(),
) {
  val c = MagnetTheme.colors
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val link by viewModel.link.collectAsState()
  val resolve by viewModel.resolve.collectAsState()
  val recent by viewModel.recent.collectAsState()
  val tipDismissed by viewModel.preferences.seekTipDismissed.collectPrefAsState()
  val incomingIntent by incoming.collectAsState()
  var tab by rememberSaveable { mutableStateOf(HomeTab.Home) }

  BackHandler(enabled = tab != HomeTab.Home) { tab = HomeTab.Home }

  LaunchedEffect(Unit) {
    viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
  }
  LaunchedEffect(Unit) {
    viewModel.playRequests.collect { PlayerLauncher.play(context, it) }
  }
  // 启动或回到前台时读剪贴板（Android 10+ 只有窗口获得焦点后才能读，稍等一下）
  LaunchedEffect(lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      delay(400)
      viewModel.onClipboard(clipboardText(context))
    }
  }

  val readTorrent: (Uri) -> Unit = { uri ->
    runCatching {
      val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
      tab = HomeTab.Home
      viewModel.resolveTorrentFile(bytes, displayName(context, uri) ?: "种子文件.torrent")
    }.onFailure { viewModel.showMessage("读取种子文件失败：${it.message}") }
  }

  // 从浏览器点 magnet: 链接，或用本 App 打开 .torrent 文件
  LaunchedEffect(incomingIntent) {
    val intent = incomingIntent ?: return@LaunchedEffect
    val data = intent.data
    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
    when {
      data != null && data.scheme.equals("magnet", ignoreCase = true) -> {
        tab = HomeTab.Home
        viewModel.openIncomingLink(data.toString())
      }
      data != null && (data.scheme == "content" || data.scheme == "file") -> readTorrent(data)
      text != null && TorrentRepository.isTorrentLink(text) -> {
        tab = HomeTab.Home
        viewModel.openIncomingLink(text)
      }
    }
    onIncomingHandled()
  }

  val pickTorrent = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let(readTorrent)
  }

  val slide = with(LocalDensity.current) { 8.dp.roundToPx() }
  Row(
    Modifier
      .fillMaxSize()
      .background(c.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    NavRail(tab, onSelect = { tab = it })
    AnimatedContent(
      targetState = tab,
      modifier = Modifier.weight(1f),
      transitionSpec = {
        (fadeIn(tween(200)) + slideInVertically(tween(200)) { slide }) togetherWith fadeOut(tween(120))
      },
      label = "page",
    ) { page ->
      when (page) {
        HomeTab.Home -> HomeMain(
          link = link,
          resolve = resolve,
          recent = recent,
          tipDismissed = tipDismissed,
          preferences = viewModel.preferences,
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
          onOpenRecent = viewModel::resumeRecent,
          onDeleteRecent = viewModel::deleteRecent,
          onShowAllRecent = { tab = HomeTab.Recent },
          onDismissTip = viewModel::dismissSeekTip,
          onPlay = { info, file -> viewModel.play(info, file) },
          onAutoClearChange = viewModel::setAutoClear,
          onCacheSizeChange = viewModel::setCacheSize,
        )
        HomeTab.Recent -> RecentPage(
          items = recent,
          onOpen = {
            tab = HomeTab.Home
            viewModel.resumeRecent(it)
          },
          onDelete = viewModel::deleteRecent,
          onClear = viewModel::clearRecent,
        )
        HomeTab.Cache -> CachePage(viewModel)
        HomeTab.Settings -> Box(Modifier.fillMaxSize()) {
          SettingsPage(viewModel.preferences, viewModel::setAutoClear, viewModel::setCacheSize)
        }
      }
    }
  }
}

private fun clipboardText(context: Context): String? = runCatching {
  val cm = context.getSystemService(ClipboardManager::class.java)
  cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}.getOrNull()

private fun displayName(context: Context, uri: Uri): String? = runCatching {
  context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
    if (it.moveToFirst()) it.getString(0) else null
  }
}.getOrNull()

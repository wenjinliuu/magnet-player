package com.wenjin.magnetplayer.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState as collectFlowAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.wenjin.magnetplayer.BuildConfig
import com.wenjin.magnetplayer.R
import com.wenjin.magnetplayer.data.RecentPlay
import com.wenjin.magnetplayer.preferences.AppearancePreferences
import com.wenjin.magnetplayer.preferences.preference.collectAsState
import com.wenjin.magnetplayer.torrent.TorrServerManager
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.torrent.TorrentStatus
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.components.IconBlock
import com.wenjin.magnetplayer.ui.components.MagnetSwitch
import com.wenjin.magnetplayer.ui.icons.MagnetIcons
import com.wenjin.magnetplayer.ui.theme.Accent
import com.wenjin.magnetplayer.ui.theme.DarkMode
import com.wenjin.magnetplayer.ui.theme.MagnetTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** 二级页面通用骨架：大标题 + 内容 */
@Composable
private fun PageScaffold(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  action: @Composable () -> Unit = {},
  content: @Composable ColumnScope.() -> Unit,
) {
  val c = MagnetTheme.colors
  Column(
    modifier
      .fillMaxSize()
      .padding(start = 36.dp, top = 36.dp, end = 40.dp, bottom = 0.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Row(verticalAlignment = Alignment.Bottom) {
      Column(Modifier.weight(1f)) {
        Text(subtitle, color = c.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
          title,
          Modifier.padding(top = 6.dp),
          color = c.onBackground,
          fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
          letterSpacing = (-0.02).em,
        )
      }
      action()
    }
    content()
  }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val c = MagnetTheme.colors
  Column(
    modifier
      .fillMaxWidth()
      .background(c.surface, RoundedCornerShape(24.dp))
      .padding(22.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
    content = content,
  )
}

@Composable
private fun CardTitle(text: String) {
  Text(
    text,
    Modifier.padding(bottom = 8.dp),
    color = MagnetTheme.colors.onBackground,
    fontSize = 18.sp,
    fontWeight = FontWeight.Bold,
  )
}

@Composable
private fun SettingRow(label: String, detail: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
  val c = MagnetTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .height(if (detail == null) 48.dp else 60.dp)
      .then(if (onClick != null) Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick) else Modifier),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(label, color = c.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
      if (detail != null) {
        Text(detail, Modifier.padding(top = 2.dp), color = c.onSurfaceVariant, fontSize = 12.sp)
      }
    }
    trailing()
  }
}

/** 分段选择（选中项实色） */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, accent: Accent, onSelect: (T) -> Unit) {
  val c = MagnetTheme.colors
  Row(
    Modifier
      .background(if (c.isDark) c.surfaceItem else c.divider, RoundedCornerShape(12.dp))
      .padding(3.dp),
  ) {
    options.forEach { (value, label) ->
      val on = value == selected
      Box(
        Modifier
          .clip(RoundedCornerShape(9.dp))
          .background(if (on) accent.fill else Color.Transparent)
          .clickable { onSelect(value) }
          .padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        Text(
          label,
          color = if (on) accent.onFill else c.onSurfaceVariant,
          fontSize = 13.sp,
          fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }
  }
}

// ---------------------------------------------------------------- 最近播放

@Composable
fun RecentPage(
  items: List<RecentPlay>,
  onOpen: (RecentPlay) -> Unit,
  onDelete: (RecentPlay) -> Unit,
  onClear: () -> Unit,
) {
  val c = MagnetTheme.colors
  PageScaffold(
    title = "最近播放",
    subtitle = "点卡片从上次位置继续 · 长按删除",
    action = {
      if (items.isNotEmpty()) {
        Text(
          "清空记录",
          Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClear)
            .padding(horizontal = 12.dp, vertical = 12.dp),
          color = c.pink.onTint.takeIf { c.isDark } ?: c.pink.fill,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
  ) {
    if (items.isEmpty()) {
      Box(
        Modifier
          .fillMaxWidth()
          .height(160.dp)
          .background(c.surface, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text("还没有播放记录", color = c.onSurfaceVariant, fontSize = 14.sp)
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(220.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 36.dp),
      ) {
        itemsIndexed(items, key = { _, it -> "${it.hash}/${it.fileId}" }) { i, item ->
          RecentCard(item, c.rotating(i), onOpen, onDelete)
        }
      }
    }
  }
}

// ---------------------------------------------------------------- 缓存

@Composable
fun CachePage(viewModel: HomeViewModel) {
  val c = MagnetTheme.colors
  val server by viewModel.serverState.collectFlowAsState()
  val cacheSize by viewModel.preferences.cacheSizeMb.collectAsState()
  var torrents by remember { mutableStateOf<List<Pair<TorrentStatus, Long>>>(emptyList()) }
  var refresh by remember { mutableStateOf(0) }

  LaunchedEffect(refresh) {
    while (true) {
      torrents = viewModel.activeTorrents()
      delay(2000)
    }
  }

  PageScaffold(title = "缓存", subtitle = "全部缓存都在内存里，不会在平板上留下视频文件") {
    Column(
      Modifier
        .widthIn(max = 820.dp)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 36.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Card {
        CardTitle("边下边播服务")
        SettingRow(
          label = when (server) {
            is TorrServerManager.State.Running -> "运行中"
            TorrServerManager.State.Starting -> "正在启动…"
            is TorrServerManager.State.Failed -> "启动失败"
            TorrServerManager.State.Stopped -> "未启动"
          },
          detail = when (val s = server) {
            is TorrServerManager.State.Running -> "TorrServer ${s.version} · 仅本机 127.0.0.1:${TorrServerManager.PORT}"
            is TorrServerManager.State.Failed -> s.message.lineSequence().firstOrNull()
            else -> null
          },
        ) {
          StatusDot(
            when (server) {
              is TorrServerManager.State.Running -> c.green
              is TorrServerManager.State.Failed -> c.red
              else -> c.orange
            },
          )
        }
        val used = torrents.sumOf { it.second }
        SettingRow("内存缓存", "每个种子最多 $cacheSize MB，播放时按需下载") {
          Text(
            "已用 ${Format.size(used)}",
            color = c.green.onTint,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      Card {
        CardTitle("当前种子")
        if (torrents.isEmpty()) {
          Text("没有正在使用的种子", Modifier.padding(vertical = 12.dp), color = c.onSurfaceVariant, fontSize = 14.sp)
        }
        torrents.forEachIndexed { i, (st, filled) ->
          Row(
            Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .background(c.surfaceItem, RoundedCornerShape(16.dp))
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            IconBlock(MagnetIcons.Cache, c.rotating(i))
            Column(Modifier.weight(1f)) {
              Text(
                st.title.ifBlank { st.name.ifBlank { st.hash } },
                color = c.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                "缓存 ${Format.size(filled)} · ${st.totalPeers} 个节点 · ${Format.speed(st.downloadSpeed)}",
                Modifier.padding(top = 2.dp),
                color = c.onSurfaceVariant,
                fontSize = 12.sp,
              )
            }
          }
        }
      }

      Box(
        Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(c.red.tint)
          .clickable { viewModel.clearAllCache { refresh++ } },
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(MagnetIcons.Trash, null, Modifier.size(18.dp), tint = c.red.onTint)
          Text("立即清空全部缓存", color = c.red.onTint, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun StatusDot(accent: Accent) {
  Box(
    Modifier
      .size(10.dp)
      .background(accent.icon, CircleShape),
  )
}

// ---------------------------------------------------------------- 设置

@Composable
fun SettingsPage(
  preferences: TorrentPreferences,
  onAutoClearChange: (Boolean) -> Unit,
  onCacheSizeChange: (Int) -> Unit,
) {
  val c = MagnetTheme.colors
  val appearance = koinInject<AppearancePreferences>()
  val darkMode by appearance.darkMode.collectAsState()
  val autoClear by preferences.autoClear.collectAsState()
  val cacheSize by preferences.cacheSizeMb.collectAsState()
  var showLibraries by remember { mutableStateOf(false) }

  if (showLibraries) {
    LibrariesPage(onBack = { showLibraries = false })
    return
  }

  PageScaffold(title = "设置", subtitle = "磁力播放器 ${BuildConfig.VERSION_NAME}") {
    Column(
      Modifier
        .widthIn(max = 820.dp)
        .verticalScroll(rememberScrollState())
        .padding(bottom = 36.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Card {
        CardTitle("外观")
        SettingRow("主题", "播放页始终是深色") {
          Segmented(
            listOf(DarkMode.System to "跟随系统", DarkMode.Light to "浅色", DarkMode.Dark to "深色"),
            darkMode,
            c.purple,
          ) { appearance.darkMode.set(it) }
        }
      }

      Card {
        CardTitle("缓存")
        SettingRow("看完自动清空缓存", "关闭播放页时立即释放这个种子的内存缓存", onClick = { onAutoClearChange(!autoClear) }) {
          MagnetSwitch(autoClear, onAutoClearChange, c.switchOn, c.divider)
        }
        SettingRow("缓存上限", "越大拖动进度条后越容易秒开，占用内存也越多") {
          Segmented(
            TorrentPreferences.CACHE_OPTIONS_MB.map { it to "$it" },
            cacheSize,
            c.purple,
            onCacheSizeChange,
          )
        }
      }

      AboutCard(onShowLibraries = { showLibraries = true })
    }
  }
}

private data class OpenSource(val name: String, val author: String, val license: String, val url: String)

private val openSourceProjects = listOf(
  OpenSource("mpvEx", "marlboro-advance", "Apache-2.0", "https://github.com/marlboro-advance/mpvEx"),
  OpenSource("mpv-android", "mpv-android 贡献者", "MIT", "https://github.com/mpv-android/mpv-android"),
  OpenSource("mpv", "mpv 贡献者", "LGPL-2.1+", "https://mpv.io"),
  OpenSource("FFmpeg", "FFmpeg 开发者", "LGPL-2.1+", "https://ffmpeg.org"),
  OpenSource("libass", "libass 贡献者", "ISC", "https://github.com/libass/libass"),
  OpenSource("TorrServer", "YouROK", "GPL-3.0", "https://github.com/YouROK/TorrServer"),
  OpenSource("anacrolix/torrent", "Matt Joiner", "MPL-2.0", "https://github.com/anacrolix/torrent"),
)

@Composable
private fun AboutCard(onShowLibraries: () -> Unit) {
  val c = MagnetTheme.colors
  val context = LocalContext.current
  fun open(url: String) = runCatching {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }
  Card {
    CardTitle("关于")
    SettingRow(
      "磁力播放器 ${BuildConfig.VERSION_NAME}（${BuildConfig.GIT_SHA}）",
      "GPL-3.0 开源 · 仅供个人使用",
      onClick = { open("https://github.com/wenjinliuu/magnet-player") },
    ) {
      Icon(MagnetIcons.ChevronRight, null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
    }
    Text(
      "本 App 使用了以下开源项目，感谢它们的作者：",
      Modifier.padding(top = 10.dp, bottom = 4.dp),
      color = c.onSurfaceVariant,
      fontSize = 13.sp,
    )
    openSourceProjects.forEach { p ->
      SettingRow(p.name, "${p.author} · ${p.license}", onClick = { open(p.url) }) {
        Icon(MagnetIcons.ChevronRight, null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
      }
    }
    SettingRow("全部依赖库的许可证", onClick = onShowLibraries) {
      Icon(MagnetIcons.ChevronRight, null, Modifier.size(18.dp), tint = c.onSurfaceVariant)
    }
  }
}

@Composable
private fun LibrariesPage(onBack: () -> Unit) {
  val c = MagnetTheme.colors
  val context = LocalContext.current
  val libraries by produceLibraries {
    context.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
  }
  BackHandler(onBack = onBack)
  Column(Modifier.fillMaxSize()) {
    Row(
      Modifier.padding(start = 24.dp, top = 24.dp, end = 40.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        Modifier
          .size(48.dp)
          .clip(CircleShape)
          .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
      ) {
        Icon(MagnetIcons.Back, "返回", Modifier.size(24.dp), tint = c.onBackground)
      }
      Text("依赖库许可证", color = c.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
    LibrariesContainer(libraries = libraries, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp))
  }
}

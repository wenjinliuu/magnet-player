package com.wenjin.magnetplayer.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wenjin.magnetplayer.preferences.preference.collectAsState
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.components.IconBlock
import com.wenjin.magnetplayer.ui.components.MagnetSwitch
import com.wenjin.magnetplayer.ui.components.Tag
import com.wenjin.magnetplayer.ui.components.TintButton
import com.wenjin.magnetplayer.ui.icons.MagnetIcons
import com.wenjin.magnetplayer.ui.theme.Accent
import com.wenjin.magnetplayer.ui.theme.MagnetColors
import com.wenjin.magnetplayer.ui.theme.MagnetTheme

/** 首页左栏：标题、输入卡、（最近播放、提示条由调用方传入） */
@Composable
fun HomeLeftColumn(
  link: String,
  onLinkChange: (String) -> Unit,
  onPaste: () -> Unit,
  onPickTorrent: () -> Unit,
  onResolve: () -> Unit,
  modifier: Modifier = Modifier,
  extra: @Composable () -> Unit = {},
) {
  val c = MagnetTheme.colors
  Column(
    modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Column {
      Text("边下边播 · 原画 · 不留文件", color = c.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      Text(
        "粘贴磁力链接，马上开播",
        Modifier.padding(top = 6.dp),
        color = c.onBackground,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.02).em,
      )
    }

    // 输入卡
    Column(
      Modifier
        .fillMaxWidth()
        .background(c.surface, RoundedCornerShape(24.dp))
        .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text("磁力链接", color = c.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      Row(
        Modifier
          .fillMaxWidth()
          .height(56.dp)
          .background(c.inputBackground, RoundedCornerShape(16.dp))
          .border(1.5.dp, c.inputBorder, RoundedCornerShape(16.dp))
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(MagnetIcons.Link, null, Modifier.size(20.dp), tint = c.blue.icon)
        BasicTextField(
          value = link,
          onValueChange = onLinkChange,
          modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = "磁力链接" },
          singleLine = true,
          textStyle = TextStyle(color = c.onBackground, fontSize = 15.sp),
          cursorBrush = SolidColor(MagnetColors.Primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(onGo = { onResolve() }),
          decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
              if (link.isEmpty()) {
                Text("magnet:?xt=urn:btih:…", color = c.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 15.sp, maxLines = 1)
              }
              inner()
            }
          },
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TintButton("粘贴", MagnetIcons.Paste, c.green, onPaste, Modifier.height(48.dp))
        TintButton("种子文件", MagnetIcons.File, c.orange, onPickTorrent, Modifier.height(48.dp))
        PrimaryButton(
          text = "解析并播放",
          onClick = onResolve,
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
          corner = 14,
          fontSize = 15,
        )
      }
    }

    extra()
  }
}

/** 蓝色实心主按钮 */
@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  corner: Int = 16,
  fontSize: Int = 16,
  enabled: Boolean = true,
) {
  Row(
    modifier
      .clip(RoundedCornerShape(corner.dp))
      .background(if (enabled) MagnetColors.Primary else MagnetColors.Primary.copy(alpha = 0.4f))
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
  ) {
    Icon(MagnetIcons.Play, null, Modifier.size(18.dp), tint = Color.White)
    Text(
      text,
      color = Color.White,
      fontSize = fontSize.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** 右栏：解析结果卡 */
@Composable
fun ResolveCard(
  state: ResolveUi,
  preferences: TorrentPreferences,
  onPlay: (TorrentInfo, TorrentFile) -> Unit,
  onAutoClearChange: (Boolean) -> Unit,
  onCacheSizeChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = MagnetTheme.colors
  Column(
    modifier
      .background(c.surface, RoundedCornerShape(24.dp))
      .padding(22.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("解析结果", Modifier.weight(1f), color = c.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      when (state) {
        is ResolveUi.Ready -> StatusPill("可以播放", c.green)
        is ResolveUi.Loading -> StatusPill(state.message, Accent(c.onSurfaceVariant, c.background, c.onSurfaceVariant, c.divider, c.onSurfaceVariant))
        is ResolveUi.Failed -> StatusPill("解析失败", c.red)
        ResolveUi.Idle -> {}
      }
    }

    when (state) {
      is ResolveUi.Ready -> ReadyContent(state, onPlay, Modifier.weight(1f))
      is ResolveUi.Loading -> CenterHint(
        if (state.peers > 0) "已连接 ${state.peers} 个节点，正在获取文件列表…" else "正在连接节点，获取文件列表…",
        Modifier.weight(1f),
      )
      is ResolveUi.Failed -> CenterHint(state.message, Modifier.weight(1f))
      ResolveUi.Idle -> EmptyState(Modifier.weight(1f))
    }

    CacheSettings(preferences, onAutoClearChange, onCacheSizeChange)

    val first = (state as? ResolveUi.Ready)?.info?.videos?.firstOrNull()
    PrimaryButton(
      text = if (first != null) "播放 ${Format.title(first.name)}" else "播放",
      onClick = { if (state is ResolveUi.Ready && first != null) onPlay(state.info, first) },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      enabled = first != null,
    )
  }
}

@Composable
private fun StatusPill(text: String, accent: Accent) {
  Row(
    Modifier
      .background(accent.tint, CircleShape)
      .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      Modifier
        .size(6.dp)
        .background(accent.icon, CircleShape),
    )
    Text(text, color = accent.onTint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun ReadyContent(
  state: ResolveUi.Ready,
  onPlay: (TorrentInfo, TorrentFile) -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = MagnetTheme.colors
  val info = state.info
  var showOthers by rememberSaveable(info.hash) { mutableStateOf(false) }
  Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Column {
      Text(
        info.title,
        color = c.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tag("${info.files.size} 个文件", c.blue)
        Tag(Format.size(info.totalSize), c.purple)
        Tag("${state.peers} 个节点", c.pink)
      }
    }

    Column(
      Modifier
        .weight(1f, fill = false)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val firstVideo = info.videos.firstOrNull()
      var rotation = 0
      info.videos.forEach { file ->
        val accent = if (file == firstVideo) c.blue else c.fileAccent(rotation++)
        FileRow(
          file = file,
          icon = MagnetIcons.VideoFile,
          accent = accent,
          subtitle = listOf(Format.size(file.length), file.codecAndResolution).filter { it.isNotBlank() }.joinToString(" · "),
          trailing = {
            PlayCircle(
              primary = file == firstVideo,
              accent = accent,
              label = "播放 ${Format.title(file.name)}",
              onClick = { onPlay(info, file) },
            )
          },
        )
      }
      info.subtitles.forEach { file ->
        FileRow(
          file = file,
          icon = MagnetIcons.SubtitleFile,
          accent = c.orange,
          subtitle = "${Format.size(file.length)} · 播放时自动加载",
          trailing = { Tag("字幕", c.orange) },
        )
      }
      val others = info.others
      if (others.isNotEmpty()) {
        Row(
          Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showOthers = !showOthers }
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            if (showOthers) "收起其他文件" else "其他 ${others.size} 个文件（图片、文本等）",
            Modifier.weight(1f),
            color = c.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
          )
          Icon(
            if (showOthers) MagnetIcons.ChevronDown else MagnetIcons.ChevronRight,
            null,
            Modifier.size(16.dp),
            tint = c.onSurfaceVariant,
          )
        }
        if (showOthers) {
          others.forEach { file ->
            FileRow(
              file = file,
              icon = MagnetIcons.OtherFile,
              accent = Accent(c.divider, c.onSurfaceVariant, c.onSurfaceVariant, c.divider, c.onSurfaceVariant),
              subtitle = Format.size(file.length),
              trailing = {},
            )
          }
        }
      }
      if (info.videos.isEmpty()) {
        Text("这个种子里没有可以播放的视频文件", color = c.onSurfaceVariant, fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun FileRow(
  file: TorrentFile,
  icon: ImageVector,
  accent: Accent,
  subtitle: String,
  trailing: @Composable () -> Unit,
) {
  val c = MagnetTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .background(c.surfaceItem, RoundedCornerShape(16.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    IconBlock(icon, accent)
    Column(Modifier.weight(1f)) {
      Text(
        file.name,
        color = c.onBackground,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(subtitle, Modifier.padding(top = 2.dp), color = c.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
    }
    trailing()
  }
}

@Composable
private fun PlayCircle(primary: Boolean, accent: Accent, label: String, onClick: () -> Unit) {
  Box(
    Modifier
      .size(44.dp)
      .clip(CircleShape)
      .background(if (primary) MagnetColors.Primary else accent.tint)
      .clickable(onClick = onClick)
      .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      MagnetIcons.Play,
      null,
      Modifier.size(16.dp),
      tint = if (primary) Color.White else accent.onTint,
    )
  }
}

@Composable
private fun CenterHint(text: String, modifier: Modifier = Modifier) {
  val c = MagnetTheme.colors
  Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Text(text, color = c.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
  }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
  val c = MagnetTheme.colors
  Column(
    modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    // 插画位：几块彩色圆角方块
    Box(Modifier.size(120.dp, 96.dp)) {
      Box(
        Modifier
          .padding(start = 8.dp, top = 20.dp)
          .size(64.dp)
          .background(c.pink.tint, RoundedCornerShape(18.dp)),
      )
      Box(
        Modifier
          .padding(start = 48.dp, top = 4.dp)
          .size(64.dp)
          .background(c.blue.fill, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(MagnetIcons.Link, null, Modifier.size(28.dp), tint = c.blue.onFill)
      }
      Box(
        Modifier
          .padding(start = 30.dp, top = 62.dp)
          .size(26.dp)
          .background(c.green.fill, RoundedCornerShape(8.dp)),
      )
    }
    Text(
      "粘贴链接后，这里会列出可以播放的文件",
      Modifier.padding(top = 18.dp),
      color = c.onSurfaceVariant,
      fontSize = 14.sp,
    )
  }
}

@Composable
private fun CacheSettings(
  preferences: TorrentPreferences,
  onAutoClearChange: (Boolean) -> Unit,
  onCacheSizeChange: (Int) -> Unit,
) {
  val c = MagnetTheme.colors
  val autoClear by preferences.autoClear.collectAsState()
  val cacheSize by preferences.cacheSizeMb.collectAsState()
  var menu by remember { mutableStateOf(false) }

  Column(Modifier.fillMaxWidth()) {
    Box(
      Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(c.divider),
    )
    Spacer(Modifier.height(10.dp))
    Row(
      Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clickable { onAutoClearChange(!autoClear) },
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("看完自动清空缓存", Modifier.weight(1f), color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
      MagnetSwitch(autoClear, onAutoClearChange, c.switchOn, c.divider)
    }
    Spacer(Modifier.height(2.dp))
    Row(
      Modifier
        .fillMaxWidth()
        .height(44.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("缓存上限", Modifier.weight(1f), color = c.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
      Box {
        Row(
          Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable { menu = true }
            .padding(start = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text("$cacheSize MB", color = c.purple.onTint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          Icon(MagnetIcons.ChevronRight, null, Modifier.size(16.dp), tint = c.purple.onTint)
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }) {
          TorrentPreferences.CACHE_OPTIONS_MB.forEach { mb ->
            DropdownMenuItem(
              text = {
                Text(
                  "$mb MB",
                  color = if (mb == cacheSize) c.purple.onTint else c.onBackground,
                  fontWeight = if (mb == cacheSize) FontWeight.SemiBold else FontWeight.Normal,
                )
              },
              onClick = {
                menu = false
                onCacheSizeChange(mb)
              },
            )
          }
        }
      }
    }
  }
}

/** 左侧 88dp 导航栏 */
enum class HomeTab { Home, Recent, Cache, Settings }

@Composable
fun NavRail(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
  val c = MagnetTheme.colors
  Row(modifier.fillMaxHeight()) {
    Column(
      Modifier
        .width(88.dp)
        .fillMaxHeight()
        .padding(vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        Modifier
          .padding(bottom = 18.dp)
          .size(44.dp)
          .background(c.logoBackground, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(MagnetIcons.Play, null, Modifier.size(20.dp), tint = c.logoForeground)
      }
      NavItem(MagnetIcons.Home, "首页", c.blue.icon, selected == HomeTab.Home) { onSelect(HomeTab.Home) }
      NavItem(MagnetIcons.Recent, "最近播放", c.pink.icon, selected == HomeTab.Recent) { onSelect(HomeTab.Recent) }
      NavItem(MagnetIcons.Cache, "缓存管理", c.green.icon, selected == HomeTab.Cache) { onSelect(HomeTab.Cache) }
      Spacer(Modifier.weight(1f))
      NavItem(MagnetIcons.Settings, "设置", c.purple.icon, selected == HomeTab.Settings) { onSelect(HomeTab.Settings) }
    }
    Box(
      Modifier
        .width(1.dp)
        .fillMaxHeight()
        .background(c.outline),
    )
  }
}

@Composable
private fun NavItem(
  icon: ImageVector,
  label: String,
  color: Color,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val c = MagnetTheme.colors
  val selectedBg = when (label) {
    "首页" -> c.navSelected
    "最近播放" -> c.pink.tint
    "缓存管理" -> c.green.tint
    else -> c.purple.tint
  }
  Box(
    Modifier
      .size(52.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(if (selected) selectedBg else Color.Transparent)
      .clickable(onClick = onClick)
      .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, null, Modifier.size(22.dp), tint = color)
  }
}

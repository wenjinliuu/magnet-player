package com.wenjin.magnetplayer.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenjin.magnetplayer.data.RecentPlay
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.icons.MagnetIcons
import com.wenjin.magnetplayer.ui.theme.Accent
import com.wenjin.magnetplayer.ui.theme.MagnetTheme

/** 首页"最近播放"：一行 3 张卡片 */
@Composable
fun RecentSection(
  items: List<RecentPlay>,
  onOpen: (RecentPlay) -> Unit,
  onDelete: (RecentPlay) -> Unit,
  onShowAll: () -> Unit,
) {
  val c = MagnetTheme.colors
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("最近播放", Modifier.weight(1f), color = c.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      Text(
        "全部",
        Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onShowAll)
          .padding(horizontal = 4.dp, vertical = 8.dp),
        color = c.pink.onTint.takeIf { c.isDark } ?: c.pink.fill,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
    if (items.isEmpty()) {
      Box(
        Modifier
          .fillMaxWidth()
          .height(124.dp)
          .background(c.surface, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text("还没有播放记录，看过的视频会出现在这里", color = c.onSurfaceVariant, fontSize = 13.sp)
      }
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) { i ->
          val item = items.getOrNull(i)
          if (item != null) {
            RecentCard(item, c.rotating(i), onOpen, onDelete, Modifier.weight(1f))
          } else {
            Spacer(Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentCard(
  item: RecentPlay,
  accent: Accent,
  onOpen: (RecentPlay) -> Unit,
  onDelete: (RecentPlay) -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = MagnetTheme.colors
  var menu by remember { mutableStateOf(false) }
  Column(
    modifier
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(onClick = { onOpen(item) }, onLongClick = { menu = true })
      .semantics { contentDescription = "继续播放 ${Format.title(item.fileName)}" },
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .height(124.dp)
        .background(accent.fill, RoundedCornerShape(16.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        Modifier
          .size(40.dp)
          .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(MagnetIcons.Play, null, Modifier.size(16.dp), tint = accent.fill)
      }
      DropdownMenu(menu, onDismissRequest = { menu = false }) {
        DropdownMenuItem(
          text = { Text("从最近播放中删除") },
          onClick = {
            menu = false
            onDelete(item)
          },
        )
      }
    }
    Text(
      Format.title(item.fileName),
      color = c.onBackground,
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Box(
      Modifier
        .fillMaxWidth()
        .height(4.dp)
        .background(if (c.isDark) c.divider else c.outline, RoundedCornerShape(2.dp)),
    ) {
      Box(
        Modifier
          .fillMaxWidth(item.progress)
          .height(4.dp)
          .background(accent.icon, RoundedCornerShape(2.dp)),
      )
    }
    Text(
      if (item.duration > 0) {
        "看到 ${Format.duration(item.position)} · 剩 ${Format.duration(item.remaining)}"
      } else {
        "看到 ${Format.duration(item.position)}"
      },
      color = c.onSurfaceVariant,
      fontSize = 12.sp,
      maxLines = 1,
    )
  }
}

/** 橙色提示条：拖动进度条会优先下载对应位置，可关闭 */
@Composable
fun SeekTip(onDismiss: () -> Unit) {
  val c = MagnetTheme.colors
  Row(
    Modifier
      .fillMaxWidth()
      .background(c.tipBackground, RoundedCornerShape(16.dp))
      .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier
        .size(28.dp)
        .background(c.orange.fill, RoundedCornerShape(9.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(MagnetIcons.Bolt, null, Modifier.size(16.dp), tint = c.orange.onFill)
    }
    Text(
      "拖动进度条时，会优先下载对应位置的片段，几秒内就能接着看。",
      Modifier.weight(1f),
      color = c.tipText,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
    )
    Box(
      Modifier
        .size(44.dp)
        .clip(CircleShape)
        .clickable(onClick = onDismiss)
        .semantics { contentDescription = "关闭提示" },
      contentAlignment = Alignment.Center,
    ) {
      Icon(MagnetIcons.Close, null, Modifier.size(14.dp), tint = c.tipText.copy(alpha = 0.6f))
    }
  }
}

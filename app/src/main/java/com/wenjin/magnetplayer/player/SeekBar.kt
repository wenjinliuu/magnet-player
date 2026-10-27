package com.wenjin.magnetplayer.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.theme.MagnetColors
import com.wenjin.magnetplayer.ui.theme.PlayerColors
import com.wenjin.magnetplayer.ui.theme.TabularNums

/**
 * 播放进度条（设计稿 player-controls）：5dp 高，底轨 20% 白，已下载片段 45% 白（可能不连续），
 * 已播放部分蓝色，拖动点 18dp 白色 + 4dp 蓝色外环；拖动时上方显示目标时间和是否已下载。
 */
@Composable
fun DownloadSeekBar(
  position: Double,
  duration: Double,
  downloaded: List<ClosedFloatingPointRange<Float>>,
  onSeekStart: () -> Unit,
  onSeek: (Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var dragFraction by remember { mutableStateOf<Float?>(null) }
  var widthPx by remember { mutableStateOf(1) }
  val density = LocalDensity.current
  val played = if (duration > 0) (position / duration).toFloat().coerceIn(0f, 1f) else 0f
  val shown = dragFraction ?: played

  BoxWithConstraints(
    modifier
      .fillMaxWidth()
      .height(5.dp),
  ) {
    // 拖动预览卡
    dragFraction?.let { f ->
      val cardWidth = 132.dp
      val x = with(density) { (f * widthPx).toDp() } - cardWidth / 2
      val maxX = maxWidth - cardWidth
      val ready = downloaded.any { f in it }
      Column(
        Modifier
          .wrapContentHeight(unbounded = true)
          .offset { IntOffset(with(density) { x.coerceIn(0.dp, maxX).roundToPx() }, with(density) { (-60).dp.roundToPx() }) }
          .requiredWidth(cardWidth)
          .background(PlayerColors.Panel, RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Text(
          Format.duration(f * duration),
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          style = TabularNums,
        )
        Text(
          if (ready) "已下载 · 秒开" else "需要下载",
          color = if (ready) PlayerColors.Download else PlayerColors.PanelSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
      }
    }

    // 触摸区比可见轨道高，方便手指拖动
    Box(
      Modifier
        .fillMaxWidth()
        .requiredHeight(28.dp)
        .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
        .semantics { contentDescription = "播放进度 ${Format.duration(position)} / ${Format.duration(duration)}" }
        .pointerInput(duration) {
          detectTapGestures { o ->
            if (duration > 0) onSeek((o.x / size.width).coerceIn(0f, 1f) * duration)
          }
        }
        .pointerInput(duration) {
          detectHorizontalDragGestures(
            onDragStart = { o ->
              onSeekStart()
              dragFraction = (o.x / size.width).coerceIn(0f, 1f)
            },
            onHorizontalDrag = { change, _ ->
              dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
            },
            onDragEnd = {
              dragFraction?.let { if (duration > 0) onSeek(it * duration) }
              dragFraction = null
            },
            onDragCancel = { dragFraction = null },
          )
        },
    ) {
      Canvas(
        Modifier
          .fillMaxWidth()
          .height(28.dp),
      ) {
        val trackH = 5.dp.toPx()
        val top = (size.height - trackH) / 2
        val r = CornerRadius(trackH / 2)
        drawRoundRect(PlayerColors.Track, Offset(0f, top), Size(size.width, trackH), r)
        downloaded.forEach { range ->
          val x0 = range.start * size.width
          val x1 = range.endInclusive * size.width
          if (x1 > x0) drawRoundRect(PlayerColors.Downloaded, Offset(x0, top), Size(x1 - x0, trackH), r)
        }
        drawRoundRect(MagnetColors.Primary, Offset(0f, top), Size(shown * size.width, trackH), r)
        val cx = shown * size.width
        val cy = size.height / 2
        drawCircle(MagnetColors.Primary, radius = 9.dp.toPx() + 4.dp.toPx(), center = Offset(cx, cy))
        drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(cx, cy))
      }
    }
  }
}

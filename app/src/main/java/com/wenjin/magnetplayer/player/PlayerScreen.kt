package com.wenjin.magnetplayer.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.wenjin.magnetplayer.ui.Format
import com.wenjin.magnetplayer.ui.icons.MagnetIcons
import com.wenjin.magnetplayer.ui.theme.MagnetColors
import com.wenjin.magnetplayer.ui.theme.PlayerColors
import com.wenjin.magnetplayer.ui.theme.TabularNums
import kotlinx.coroutines.delay
import kotlin.math.abs

/** 播放页需要的操作，由 Activity 实现 */
interface PlayerActions {
  fun back()
  fun togglePause()
  fun seekBy(seconds: Double)
  fun seekTo(seconds: Double)
  fun setSpeed(speed: Double)
  fun selectSubtitle(track: MediaTrack)
  fun subtitleOff()
  fun onlineSubtitleSearch()
  fun pickLocalSubtitle()
  fun setBilingual(enabled: Boolean)
  fun setSubtitleSize(size: SubtitleSize)
  fun setSubtitleDelay(seconds: Double)
  fun setSubtitleRaised(raised: Boolean)
  fun selectAudio(track: MediaTrack)
  fun setAspect(mode: AspectMode)
  fun setHardwareDecoding(enabled: Boolean)
  fun enterPip()

  /** 调亮度 / 音量，返回调整后的值（0..1） */
  fun adjustBrightness(delta: Float): Float
  fun adjustVolume(delta: Float): Float
}

data class PlayerInfo(
  val title: String,
  val torrentTitle: String,
  val hash: String?,
  val fileLength: Long,
)

private enum class Dialog { None, More, Audio, Aspect, Decoder, Torrent, Speed, Quality }

private sealed interface GestureHint {
  data class Seek(val target: Double, val delta: Double) : GestureHint
  data class Level(val icon: String, val value: Float) : GestureHint
  data class Skip(val text: String) : GestureHint
  data object FastForward : GestureHint
}

@Composable
fun PlayerScreen(
  info: PlayerInfo,
  playback: PlaybackUi,
  torrent: TorrentUi,
  subtitles: SubtitleUi,
  audioTracks: List<MediaTrack>,
  subtitleSize: SubtitleSize,
  bilingual: Boolean,
  aspect: AspectMode,
  inPip: Boolean,
  actions: PlayerActions,
) {
  var controlsVisible by remember { mutableStateOf(true) }
  var panelOpen by remember { mutableStateOf(false) }
  var locked by remember { mutableStateOf(false) }
  var dialog by remember { mutableStateOf(Dialog.None) }
  var hint by remember { mutableStateOf<GestureHint?>(null) }
  // 每次操作控件时 +1，用来重置 3 秒自动隐藏
  var touches by remember { mutableIntStateOf(0) }
  val poke: () -> Unit = { touches++ }

  LaunchedEffect(controlsVisible, playback.paused, panelOpen, dialog, touches) {
    if (controlsVisible && !playback.paused && !panelOpen && dialog == Dialog.None) {
      delay(3000)
      controlsVisible = false
    }
  }
  val showControls = controlsVisible && !panelOpen && !locked && !inPip
  LaunchedEffect(showControls) { actions.setSubtitleRaised(showControls) }
  LaunchedEffect(hint) {
    if (hint is GestureHint.Skip) {
      delay(700)
      hint = null
    }
  }

  Box(Modifier.fillMaxSize()) {
    // 手势层：轻点显示 / 隐藏，左右双击 ±10 秒，横向拖动调进度，左侧上下调亮度、右侧调音量，长按 2 倍速
    GestureLayer(
      enabled = !locked && !inPip && !panelOpen,
      playback = playback,
      onTap = {
        if (panelOpen) panelOpen = false else controlsVisible = !controlsVisible
      },
      onHint = { hint = it },
      actions = actions,
    )

    if (inPip) return@Box

    // 渐变暗角（不使用 blur）
    AnimatedVisibility(showControls, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
      Box(Modifier.fillMaxSize()) {
        Box(
          Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Brush.verticalGradient(listOf(PlayerColors.TopScrim, Color.Transparent))),
        )
        Box(
          Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(260.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, PlayerColors.BottomScrim))),
        )
        TopBar(info, playback, torrent, onBack = actions::back, onSubtitles = {
          panelOpen = true
          poke()
        }, onMore = {
          dialog = Dialog.More
          poke()
        })
        if (!playback.buffering) {
          CenterControls(playback.paused, actions, poke)
        }
        BottomBar(
          playback = playback,
          torrent = torrent,
          subtitles = subtitles,
          onSeekStart = poke,
          onSeek = {
            actions.seekTo(it)
            poke()
          },
          onSubtitles = {
            panelOpen = true
            poke()
          },
          onSpeed = { dialog = Dialog.Speed },
          onQuality = { dialog = Dialog.Quality },
          onLock = {
            locked = true
            controlsVisible = false
          },
          onPip = actions::enterPip,
        )
      }
    }

    // 缓冲中：中间圆形加载指示 + 当前下载速度
    if (playback.buffering && !playback.paused) {
      Column(
        Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CircularProgressIndicator(Modifier.size(48.dp), color = Color.White, strokeWidth = 3.dp, trackColor = Color(0x33FFFFFF))
        Text(
          Format.speed(torrent.downloadSpeed),
          color = PlayerColors.Download,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          style = TabularNums,
        )
      }
    }

    hint?.let { GestureHintView(it, playback.duration) }

    // 锁屏：只显示一个解锁按钮
    if (locked) {
      LockedOverlay(onUnlock = {
        locked = false
        controlsVisible = true
      })
    }

    // 字幕面板：右侧滑入，视频加 35% 黑色遮罩，继续播放
    AnimatedVisibility(panelOpen, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
      Box(
        Modifier
          .fillMaxSize()
          .background(Color(0x59000000))
          .clickable(remember { MutableInteractionSource() }, null) { panelOpen = false },
      )
    }
    AnimatedVisibility(
      panelOpen,
      modifier = Modifier.align(Alignment.CenterEnd),
      enter = slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { it },
      exit = slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { it },
    ) {
      SubtitlePanel(
        state = subtitles,
        bilingual = bilingual,
        size = subtitleSize,
        onSelect = actions::selectSubtitle,
        onOff = actions::subtitleOff,
        onOnlineSearch = actions::onlineSubtitleSearch,
        onPickLocal = actions::pickLocalSubtitle,
        onBilingualChange = actions::setBilingual,
        onSizeChange = actions::setSubtitleSize,
        onDelayChange = actions::setSubtitleDelay,
        onClose = { panelOpen = false },
      )
    }

    PlayerDialogs(
      dialog = dialog,
      onDialog = { dialog = it },
      info = info,
      playback = playback,
      torrent = torrent,
      audioTracks = audioTracks,
      aspect = aspect,
      actions = actions,
    )
  }
}

// ---------------------------------------------------------------- 顶栏

@Composable
private fun BoxScope.TopBar(
  info: PlayerInfo,
  playback: PlaybackUi,
  torrent: TorrentUi,
  onBack: () -> Unit,
  onSubtitles: () -> Unit,
  onMore: () -> Unit,
) {
  Row(
    Modifier
      .align(Alignment.TopStart)
      .fillMaxWidth()
      .padding(start = 32.dp, end = 32.dp, top = 24.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    RoundIcon(MagnetIcons.Back, "返回", 48, 24, Color.White, onClick = onBack)
    Column(Modifier.weight(1f)) {
      Text(
        info.title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        mediaMeta(playback, info.fileLength),
        Modifier.padding(top = 2.dp),
        color = Color(0xB8FFFFFF),
        fontSize = 13.sp,
        maxLines = 1,
      )
    }
    if (info.hash != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Status(MagnetIcons.Download, Format.speed(torrent.downloadSpeed), PlayerColors.Download)
        Status(MagnetIcons.Peers, "${torrent.peers} 节点", PlayerColors.Peers)
        Status(
          MagnetIcons.Cache,
          "缓冲 ${torrent.cacheFilled / MB} / ${torrent.cacheCapacity / MB} MB",
          PlayerColors.Buffer,
        )
      }
    }
    RoundIcon(MagnetIcons.Subtitles, "字幕", 48, 24, PlayerColors.SubtitleButton, onClick = onSubtitles)
    RoundIcon(MagnetIcons.More, "更多", 48, 22, Color.White, onClick = onMore)
  }
}

private const val MB = 1024L * 1024

@Composable
private fun Status(icon: ImageVector, text: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Icon(icon, null, Modifier.size(16.dp), tint = color)
    Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, style = TabularNums, maxLines = 1)
  }
}

/** "2160p · HEVC · 8.2 GB" */
private fun mediaMeta(p: PlaybackUi, fileLength: Long): String = listOfNotNull(
  p.videoHeight.takeIf { it > 0 }?.let { "${it}p" },
  p.videoCodec?.let(::codecName),
  fileLength.takeIf { it > 0 }?.let(Format::size),
).joinToString(" · ")

fun codecName(codec: String): String = when (codec.lowercase()) {
  "hevc", "h265" -> "HEVC"
  "h264", "avc" -> "H.264"
  "av1" -> "AV1"
  "vp9" -> "VP9"
  "mpeg4" -> "MPEG-4"
  else -> codec.uppercase()
}

// ---------------------------------------------------------------- 中间

@Composable
private fun BoxScope.CenterControls(paused: Boolean, actions: PlayerActions, poke: () -> Unit) {
  Row(
    Modifier.align(Alignment.Center),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(56.dp),
  ) {
    SkipButton(back = true) {
      actions.seekBy(-10.0)
      poke()
    }
    Box(
      Modifier
        .size(88.dp)
        .clip(CircleShape)
        .background(MagnetColors.Primary)
        .clickable {
          actions.togglePause()
          poke()
        }
        .semantics { contentDescription = if (paused) "播放" else "暂停" },
      contentAlignment = Alignment.Center,
    ) {
      Icon(if (paused) MagnetIcons.Play else MagnetIcons.Pause, null, Modifier.size(34.dp), tint = Color.White)
    }
    SkipButton(back = false) {
      actions.seekBy(10.0)
      poke()
    }
  }
}

@Composable
private fun SkipButton(back: Boolean, onClick: () -> Unit) {
  Box(
    Modifier
      .size(64.dp)
      .clip(CircleShape)
      .background(PlayerColors.RoundButton)
      .clickable(onClick = onClick)
      .semantics { contentDescription = if (back) "后退 10 秒" else "前进 10 秒" },
    contentAlignment = Alignment.Center,
  ) {
    Icon(if (back) MagnetIcons.Replay else MagnetIcons.Forward, null, Modifier.size(32.dp), tint = Color.White)
    Text(
      "10",
      Modifier.padding(start = if (back) 1.dp else 0.dp, end = if (back) 0.dp else 1.dp, top = 1.dp),
      color = Color.White,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

// ---------------------------------------------------------------- 底栏

@Composable
private fun BoxScope.BottomBar(
  playback: PlaybackUi,
  torrent: TorrentUi,
  subtitles: SubtitleUi,
  onSeekStart: () -> Unit,
  onSeek: (Double) -> Unit,
  onSubtitles: () -> Unit,
  onSpeed: () -> Unit,
  onQuality: () -> Unit,
  onLock: () -> Unit,
  onPip: () -> Unit,
) {
  Column(
    Modifier
      .align(Alignment.BottomCenter)
      .fillMaxWidth()
      .padding(start = 40.dp, end = 40.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    DownloadSeekBar(
      position = playback.position,
      duration = playback.duration,
      downloaded = torrent.downloaded,
      onSeekStart = onSeekStart,
      onSeek = onSeek,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(Format.duration(playback.position), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, style = TabularNums)
        Text(
          " / ${Format.duration(playback.duration)}",
          color = Color(0x99FFFFFF),
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          style = TabularNums,
        )
      }
      Row(
        Modifier.padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Legend(MagnetColors.Primary, "已播放")
        Legend(PlayerColors.Downloaded, "已下载片段")
      }
      Spacer(Modifier.weight(1f))
      Pill(subtitlePillLabel(subtitles), Color(0xFFFF3D7F), Color.White, FontWeight.SemiBold, onSubtitles)
      Pill(speedLabel(playback.speed), Color(0xFFFF8A00), Color(0xFF1F0F00), FontWeight.Bold, onSpeed)
      Pill(
        "原画" + (playback.videoHeight.takeIf { it > 0 }?.let { " ${it}p" } ?: ""),
        Color(0xFF7C5CFF),
        Color.White,
        FontWeight.SemiBold,
        onQuality,
      )
      RoundIcon(MagnetIcons.Lock, "锁定屏幕", 44, 22, Color.White, onClick = onLock)
      RoundIcon(MagnetIcons.Pip, "画中画", 44, 22, Color.White, onClick = onPip)
    }
  }
}

@Composable
private fun Legend(color: Color, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(
      Modifier
        .size(12.dp, 4.dp)
        .background(color, RoundedCornerShape(2.dp)),
    )
    Text(text, color = Color(0xB8FFFFFF), fontSize = 12.sp)
  }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, weight: FontWeight, onClick: () -> Unit) {
  Box(
    Modifier
      .height(40.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(bg)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, color = fg, fontSize = 14.sp, fontWeight = weight, maxLines = 1)
  }
}

private fun speedLabel(speed: Double): String =
  if (speed == speed.toInt().toDouble()) "${speed.toInt()}.0×" else "${"%.2f".format(speed).trimEnd('0')}×"

/** "字幕 中/日"、"字幕 中"、"字幕 关" */
private fun subtitlePillLabel(s: SubtitleUi): String {
  fun short(id: Int): String? {
    val t = s.tracks.firstOrNull { it.id == id } ?: return null
    return when (languageName(t.lang) ?: if (isChinese(t.lang, t.title ?: t.externalFilename)) "中文" else null) {
      "中文" -> "中"
      "日语" -> "日"
      "英语" -> "英"
      "韩语" -> "韩"
      else -> "开"
    }
  }
  val a = short(s.sid) ?: return "字幕 关"
  val b = short(s.secondarySid)
  return if (b != null) "字幕 $a/$b" else "字幕 $a"
}

@Composable
private fun RoundIcon(
  icon: ImageVector,
  label: String,
  size: Int,
  iconSize: Int,
  tint: Color,
  background: Color = Color.Transparent,
  onClick: () -> Unit,
) {
  Box(
    Modifier
      .size(size.dp)
      .clip(CircleShape)
      .background(background)
      .clickable(onClick = onClick)
      .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, null, Modifier.size(iconSize.dp), tint = tint)
  }
}

// ---------------------------------------------------------------- 手势

@Composable
private fun GestureLayer(
  enabled: Boolean,
  playback: PlaybackUi,
  onTap: () -> Unit,
  onHint: (GestureHint?) -> Unit,
  actions: PlayerActions,
) {
  var longPress by remember { mutableStateOf(false) }
  // pointerInput 的代码块只在 key 变化时重建，用 rememberUpdatedState 读到最新的播放位置
  val current by rememberUpdatedState(playback)
  Box(
    Modifier
      .fillMaxSize()
      .pointerInput(enabled) {
        detectTapGestures(
          onTap = { onTap() },
          onDoubleTap = { o ->
            if (!enabled) return@detectTapGestures
            val back = o.x < size.width / 2
            actions.seekBy(if (back) -10.0 else 10.0)
            onHint(GestureHint.Skip(if (back) "−10 秒" else "+10 秒"))
          },
          onLongPress = {
            if (!enabled) return@detectTapGestures
            longPress = true
            actions.setSpeed(2.0)
            onHint(GestureHint.FastForward)
          },
          onPress = {
            tryAwaitRelease()
            if (longPress) {
              longPress = false
              actions.setSpeed(1.0)
              onHint(null)
            }
          },
        )
      }
      .pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          val startX = down.position.x
          val slop = viewConfiguration.touchSlop
          var mode = 0 // 0 未定，1 横向调进度，2 亮度，3 音量
          var total = Offset.Zero
          val startPos = current.position
          val duration = current.duration
          var target = startPos
          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val delta = change.positionChange()
            total += delta
            if (mode == 0 && total.getDistance() > slop) {
              mode = if (abs(total.x) > abs(total.y)) 1 else if (startX < size.width / 2) 2 else 3
            }
            when (mode) {
              1 -> {
                // 全屏宽度 ≈ 120 秒
                target = (startPos + total.x / size.width * 120.0).coerceIn(0.0, duration.coerceAtLeast(0.0))
                onHint(GestureHint.Seek(target, target - startPos))
                change.consume()
              }
              2 -> {
                val v = actions.adjustBrightness(-delta.y / size.height * 1.2f)
                onHint(GestureHint.Level("亮度", v))
                change.consume()
              }
              3 -> {
                val v = actions.adjustVolume(-delta.y / size.height * 1.2f)
                onHint(GestureHint.Level("音量", v))
                change.consume()
              }
            }
          }
          if (mode == 1) actions.seekTo(target)
          if (mode != 0) onHint(null)
        }
      },
  )
}

@Composable
private fun BoxScope.GestureHintView(hint: GestureHint, duration: Double) {
  Box(
    Modifier
      .align(Alignment.Center)
      .padding(bottom = 140.dp)
      .background(Color(0xB3121318), RoundedCornerShape(14.dp))
      .padding(horizontal = 18.dp, vertical = 12.dp),
  ) {
    when (hint) {
      is GestureHint.Seek -> Text(
        "${Format.duration(hint.target)} / ${Format.duration(duration)}  " +
          (if (hint.delta >= 0) "+" else "−") + "${abs(hint.delta).toInt()} 秒",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        style = TabularNums,
      )
      is GestureHint.Level -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(hint.icon, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(
          Modifier
            .width(140.dp)
            .height(5.dp)
            .background(PlayerColors.Track, RoundedCornerShape(3.dp)),
        ) {
          Box(
            Modifier
              .fillMaxHeight()
              .fillMaxWidth(hint.value.coerceIn(0f, 1f))
              .background(Color.White, RoundedCornerShape(3.dp)),
          )
        }
        Text("${(hint.value * 100).toInt()}%", color = Color.White, fontSize = 15.sp, style = TabularNums)
      }
      is GestureHint.Skip -> Text(hint.text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
      GestureHint.FastForward -> Text("2 倍速播放中", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun BoxScope.LockedOverlay(onUnlock: () -> Unit) {
  var visible by remember { mutableStateOf(true) }
  var touches by remember { mutableIntStateOf(0) }
  LaunchedEffect(touches) {
    visible = true
    delay(3000)
    visible = false
  }
  Box(
    Modifier
      .fillMaxSize()
      .pointerInput(Unit) { detectTapGestures { touches++ } },
  )
  AnimatedVisibility(
    visible,
    modifier = Modifier
      .align(Alignment.CenterStart)
      .padding(start = 40.dp),
    enter = fadeIn(tween(150)),
    exit = fadeOut(tween(150)),
  ) {
    RoundIcon(MagnetIcons.Unlock, "解锁屏幕", 56, 24, Color.White, PlayerColors.RoundButton, onUnlock)
  }
}

// ---------------------------------------------------------------- 菜单 / 对话框

@Composable
private fun PlayerDialogs(
  dialog: Dialog,
  onDialog: (Dialog) -> Unit,
  info: PlayerInfo,
  playback: PlaybackUi,
  torrent: TorrentUi,
  audioTracks: List<MediaTrack>,
  aspect: AspectMode,
  actions: PlayerActions,
) {
  val close = { onDialog(Dialog.None) }
  when (dialog) {
    Dialog.None -> {}
    Dialog.More -> OptionsCard("更多", close) {
      Option("音轨", detail = audioTracks.firstOrNull { it.selected }?.let(::audioLabel)) { onDialog(Dialog.Audio) }
      Option("画面比例", detail = aspect.label) { onDialog(Dialog.Aspect) }
      Option("解码方式", detail = decoderLabel(playback.hwdec)) { onDialog(Dialog.Decoder) }
      Option("种子信息") { onDialog(Dialog.Torrent) }
    }
    Dialog.Audio -> OptionsCard("音轨", close) {
      if (audioTracks.isEmpty()) Info("没有音轨")
      audioTracks.forEach { t ->
        Option(audioLabel(t), selected = t.selected) {
          actions.selectAudio(t)
          close()
        }
      }
    }
    Dialog.Aspect -> OptionsCard("画面比例", close) {
      AspectMode.entries.forEach { m ->
        Option(m.label, selected = m == aspect) {
          actions.setAspect(m)
          close()
        }
      }
    }
    Dialog.Decoder -> OptionsCard("解码方式", close) {
      val hw = playback.hwdec != null && playback.hwdec != "no"
      Option("硬件解码", detail = "省电，大多数视频推荐", selected = hw) {
        actions.setHardwareDecoding(true)
        close()
      }
      Option("软件解码", detail = "画面异常时再试", selected = !hw) {
        actions.setHardwareDecoding(false)
        close()
      }
    }
    Dialog.Speed -> OptionsCard("倍速", close) {
      listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0).forEach { s ->
        Option(speedLabel(s), selected = abs(playback.speed - s) < 0.01) {
          actions.setSpeed(s)
          close()
        }
      }
    }
    Dialog.Quality -> OptionsCard("原画播放", close) {
      Info("直接播放种子里的原始文件，不转码、不压缩。")
      Info("分辨率：" + (playback.videoHeight.takeIf { it > 0 }?.let { "${playback.videoWidth} × $it" } ?: "未知"))
      Info("编码：" + (playback.videoCodec?.let(::codecName) ?: "未知"))
      Info("解码：" + decoderLabel(playback.hwdec))
      Info("文件大小：" + Format.size(info.fileLength))
    }
    Dialog.Torrent -> OptionsCard("种子信息", close) {
      Info("种子：" + info.torrentTitle.ifBlank { "未知" })
      info.hash?.let { Info("InfoHash：$it") }
      Info("文件：${info.title}（${Format.size(info.fileLength)}）")
      Info("下载速度：${Format.speed(torrent.downloadSpeed)} · ${torrent.peers} 个节点")
      Info("内存缓存：${Format.size(torrent.cacheFilled)} / ${Format.size(torrent.cacheCapacity)}")
    }
  }
}

private fun audioLabel(t: MediaTrack): String =
  listOfNotNull(t.title, languageName(t.lang) ?: t.lang?.takeIf { it != "und" }, t.codec?.uppercase())
    .joinToString(" · ").ifBlank { "音轨 ${t.id}" }

private fun decoderLabel(hwdec: String?): String = when {
  hwdec == null -> "未知"
  hwdec == "no" -> "软件解码"
  else -> "硬件解码"
}

/** 深色选项卡片（播放页永远深色） */
@Composable
private fun OptionsCard(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
  Popup(alignment = Alignment.Center, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
    Column(
      Modifier
        .width(380.dp)
        .background(PlayerColors.Panel, RoundedCornerShape(20.dp))
        .padding(horizontal = 16.dp, vertical = 18.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).padding(start = 8.dp), color = PlayerColors.PanelText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        RoundIcon(MagnetIcons.Close, "关闭", 44, 20, PlayerColors.PanelIcon, onClick = onDismiss)
      }
      content()
    }
  }
}

@Composable
private fun Option(title: String, detail: String? = null, selected: Boolean = false, onClick: () -> Unit) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(if (selected) Color(0x1AFF5C99) else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, color = PlayerColors.PanelText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
      if (detail != null) Text(detail, color = PlayerColors.PanelSecondary, fontSize = 12.sp)
    }
    if (selected) Icon(MagnetIcons.Check, null, Modifier.size(18.dp), tint = Color(0xFFFF8FB8))
  }
}

@Composable
private fun Info(text: String) {
  Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = PlayerColors.PanelText, fontSize = 14.sp)
}

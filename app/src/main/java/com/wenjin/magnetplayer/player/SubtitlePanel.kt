package com.wenjin.magnetplayer.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenjin.magnetplayer.ui.components.MagnetSwitch
import com.wenjin.magnetplayer.ui.icons.MagnetIcons
import com.wenjin.magnetplayer.ui.theme.MagnetColors
import com.wenjin.magnetplayer.ui.theme.PlayerColors
import com.wenjin.magnetplayer.ui.theme.TabularNums
import java.util.Locale

private val Pink = Color(0xFFFF3D7F)
private val PinkBorder = Color(0xFFFF5C99)
private val PinkCheck = Color(0xFFFF8FB8)
private val PinkSelectedBg = Color(0x1AFF5C99)
private val Green = Color(0xFF12B886)
private val Purple = Color(0xFF7C5CFF)
private val Orange = Color(0xFFFF8A00)
private val OnOrange = Color(0xFF1F0F00)
private val Switch = Color(0xFF20B985)
private val Blue = Color(0xFF8FB0FF)
private val GreyBlock = Color(0xFF24272E)

/** 右侧 380dp 字幕面板（设计稿 player-subtitles） */
@Composable
fun SubtitlePanel(
  state: SubtitleUi,
  bilingual: Boolean,
  size: SubtitleSize,
  onSelect: (MediaTrack) -> Unit,
  onOff: () -> Unit,
  onOnlineSearch: () -> Unit,
  onPickLocal: () -> Unit,
  onBilingualChange: (Boolean) -> Unit,
  onSizeChange: (SubtitleSize) -> Unit,
  onDelayChange: (Double) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(modifier.fillMaxHeight()) {
    Box(
      Modifier
        .width(1.dp)
        .fillMaxHeight()
        .background(PlayerColors.PanelDivider),
    )
    Column(
      Modifier
        .width(379.dp)
        .fillMaxHeight()
        .background(PlayerColors.Panel)
        .padding(horizontal = 24.dp, vertical = 28.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("字幕", Modifier.weight(1f), color = PlayerColors.PanelText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Box(
          Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClose)
            .semantics { contentDescription = "关闭面板" },
          contentAlignment = Alignment.Center,
        ) {
          Icon(MagnetIcons.Close, null, Modifier.size(20.dp), tint = PlayerColors.PanelIcon)
        }
      }

      Column(
        Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SectionLabel("可用字幕")
        state.tracks.forEach { track ->
          val primary = track.id == state.sid
          val secondary = track.id == state.secondarySid
          PanelItem(
            icon = MagnetIcons.SubtitleFile,
            iconBg = if (primary || secondary) Pink else GreyBlock,
            iconTint = if (primary || secondary) Color.White else PlayerColors.PanelSecondary,
            title = trackTitle(track),
            detail = trackDetail(track) + if (secondary) " · 双语原文" else "",
            selected = primary,
            onClick = { onSelect(track) },
          ) {
            if (primary) Icon(MagnetIcons.Check, null, Modifier.size(20.dp), tint = PinkCheck)
          }
        }
        if (state.tracks.isEmpty()) {
          Text(
            "没有找到字幕，可以从下面获取",
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = PlayerColors.PanelSecondary,
            fontSize = 13.sp,
          )
        }
        PanelItem(
          icon = MagnetIcons.SubtitleOff,
          iconBg = GreyBlock,
          iconTint = PlayerColors.PanelSecondary,
          title = "关闭字幕",
          titleWeight = FontWeight.Medium,
          selected = state.sid <= 0 && state.tracks.isNotEmpty(),
          onClick = onOff,
        )

        Divider()
        SectionLabel("获取更多")
        PanelItem(
          icon = MagnetIcons.Search,
          iconBg = MagnetColors.Primary,
          title = "在线搜索字幕",
          detail = "伪射手 · 迅雷",
          onClick = onOnlineSearch,
        ) {
          Icon(MagnetIcons.ChevronRight, null, Modifier.size(18.dp), tint = PlayerColors.Chevron)
        }
        // 第二期功能：第一期显示入口但置灰不可点
        PanelItem(
          icon = MagnetIcons.Sparkle,
          iconBg = Purple,
          title = "AI 识别并翻译",
          detail = "日语 → 中文，双语显示",
          enabled = false,
          onClick = {},
        ) {
          Text(
            "第二期",
            Modifier
              .background(Color(0x299B85FF), RoundedCornerShape(6.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp),
            color = Color(0xFFBBAEFF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
          )
        }
        PanelItem(
          icon = MagnetIcons.File,
          iconBg = Green,
          title = "从平板选择字幕文件",
          detail = "SRT · ASS · VTT",
          onClick = onPickLocal,
        )
      }

      Divider()
      SettingRow("双语显示") {
        MagnetSwitch(bilingual, onBilingualChange, Switch, GreyBlock)
      }
      SettingRow("字号") {
        Row(
          Modifier
            .background(PlayerColors.PanelItem, RoundedCornerShape(12.dp))
            .padding(3.dp),
        ) {
          SubtitleSize.entries.forEach { s ->
            val on = s == size
            Box(
              Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(if (on) Orange else Color.Transparent)
                .clickable { onSizeChange(s) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Text(
                s.label,
                color = if (on) OnOrange else PlayerColors.PanelIcon,
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
              )
            }
          }
        }
      }
      SettingRow("字幕时间偏移") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          StepButton("−", "字幕提前 0.5 秒") { onDelayChange(state.delay - 0.5) }
          Text(
            String.format(Locale.US, "%.1f 秒", state.delay),
            Modifier.width(56.dp),
            color = Blue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            style = TabularNums,
          )
          StepButton("+", "字幕延后 0.5 秒") { onDelayChange(state.delay + 0.5) }
        }
      }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text,
    Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
    color = PlayerColors.PanelSecondary,
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun Divider() {
  Box(
    Modifier
      .padding(vertical = 8.dp)
      .fillMaxWidth()
      .height(1.dp)
      .background(PlayerColors.PanelDivider),
  )
}

@Composable
private fun PanelItem(
  icon: ImageVector,
  iconBg: Color,
  title: String,
  onClick: () -> Unit,
  iconTint: Color = Color.White,
  detail: String? = null,
  titleWeight: FontWeight = FontWeight.SemiBold,
  selected: Boolean = false,
  enabled: Boolean = true,
  trailing: @Composable () -> Unit = {},
) {
  val shape = RoundedCornerShape(14.dp)
  Row(
    Modifier
      .fillMaxWidth()
      .alpha(if (enabled) 1f else 0.45f)
      .clip(shape)
      .then(
        if (selected) Modifier.background(PinkSelectedBg).border(1.5.dp, PinkBorder, shape) else Modifier,
      )
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier
        .size(36.dp)
        .background(iconBg, RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, null, Modifier.size(18.dp), tint = iconTint)
    }
    Column(Modifier.weight(1f)) {
      Text(
        title,
        color = PlayerColors.PanelText,
        fontSize = 15.sp,
        fontWeight = titleWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (detail != null) {
        Text(detail, Modifier.padding(top = 2.dp), color = PlayerColors.PanelSecondary, fontSize = 12.sp, maxLines = 1)
      }
    }
    trailing()
  }
}

@Composable
private fun SettingRow(label: String, trailing: @Composable () -> Unit) {
  Row(
    Modifier
      .fillMaxWidth()
      .height(48.dp)
      .padding(horizontal = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, Modifier.weight(1f), color = PlayerColors.PanelText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    trailing()
  }
}

@Composable
private fun StepButton(text: String, label: String, onClick: () -> Unit) {
  Box(
    Modifier
      .size(36.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(PlayerColors.PanelItem)
      .clickable(onClick = onClick)
      .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
  ) {
    Text(text, color = PlayerColors.PanelText, fontSize = 18.sp)
  }
}

/** 字幕显示名 */
fun trackTitle(track: MediaTrack): String =
  track.title?.takeIf { it.isNotBlank() }
    ?: track.externalFilename?.substringAfterLast('/')?.substringBefore('?')?.takeIf { it.isNotBlank() }
    ?: languageName(track.lang)?.let { "${it}字幕" }
    ?: "字幕 ${track.id}"

/** 字幕来源与语言，例如"种子内自带 · 中文 · ASS" */
fun trackDetail(track: MediaTrack): String {
  val source = when {
    !track.external -> "视频内嵌"
    track.externalFilename?.contains("/subs/") == true -> "种子内自带"
    else -> "从平板加载"
  }
  return listOfNotNull(source, languageName(track.lang), track.codec?.uppercase()?.takeIf { it.length <= 8 })
    .joinToString(" · ")
}

/** 常见语言代码转中文名；"und" 或未知返回 null */
fun languageName(lang: String?): String? = when (lang?.lowercase()) {
  "zh", "chi", "zho", "chs", "cht", "sc", "tc", "zh-cn", "zh-tw", "zh-hans", "zh-hant" -> "中文"
  "ja", "jpn", "jp" -> "日语"
  "en", "eng" -> "英语"
  "ko", "kor" -> "韩语"
  else -> null
}

/** 字幕是否是中文（看语言代码，没有时看文件名） */
fun isChinese(lang: String?, name: String?): Boolean {
  if (languageName(lang) == "中文") return true
  val n = name?.lowercase() ?: return false
  return listOf("简", "繁", "中文", "chs", "cht", ".sc.", ".tc.", "zh", "chi", "gb", "big5").any { it in n }
}

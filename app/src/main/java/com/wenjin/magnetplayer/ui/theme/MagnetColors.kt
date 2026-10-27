package com.wenjin.magnetplayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 一种点缀色的几种用法（对应 docs/UI_SPEC.md 1.2）。
 * - [fill]：实色填充（图标块、封面、实心按钮）
 * - [onFill]：实色上的文字 / 图标
 * - [icon]：单独出现的图标、进度条
 * - [tint] / [onTint]：浅底及浅底上的文字
 */
@Immutable
data class Accent(
  val fill: Color,
  val onFill: Color,
  val icon: Color,
  val tint: Color,
  val onTint: Color,
)

/** 颜色 token，与 docs/UI_SPEC.md 第 1 节一一对应 */
@Immutable
data class MagnetColors(
  val isDark: Boolean,
  val background: Color,
  val surface: Color,
  val surfaceItem: Color,
  val outline: Color,
  val divider: Color,
  val onBackground: Color,
  val onSurfaceVariant: Color,
  val blue: Accent,
  val pink: Accent,
  val green: Accent,
  val purple: Accent,
  val orange: Accent,
  /** 开关打开时的颜色 */
  val switchOn: Color,
  /** 输入框底色与描边 */
  val inputBackground: Color,
  val inputBorder: Color,
  /** 导航栏选中项底色 */
  val navSelected: Color,
  /** 导航栏 Logo 方块 */
  val logoBackground: Color,
  val logoForeground: Color,
  /** 提示条 */
  val tipBackground: Color,
  val tipText: Color,
  /** 状态胶囊：失败 */
  val red: Accent,
) {
  /** 最近播放封面、文件列表图标的轮换顺序：粉 → 绿 → 紫 → 蓝 → 橙 */
  val rotation: List<Accent> get() = listOf(pink, green, purple, blue, orange)

  fun rotating(index: Int): Accent = rotation[index.mod(rotation.size)]

  companion object {
    /** 主操作色，两种主题相同 */
    val Primary = Color(0xFF2F6BFF)

    val Light = MagnetColors(
      isDark = false,
      background = Color(0xFFFFFFFF),
      surface = Color(0xFFF6F7F9),
      surfaceItem = Color(0xFFFFFFFF),
      outline = Color(0xFFECEEF2),
      divider = Color(0xFFE6E8ED),
      onBackground = Color(0xFF14161B),
      onSurfaceVariant = Color(0xFF5F6571),
      blue = Accent(Color(0xFF2F6BFF), Color.White, Color(0xFF2F6BFF), Color(0xFFEAF0FF), Color(0xFF1D4FD8)),
      pink = Accent(Color(0xFFFF3D7F), Color.White, Color(0xFFFF3D7F), Color(0xFFFFE8F0), Color(0xFFC2185B)),
      green = Accent(Color(0xFF12B886), Color.White, Color(0xFF12B886), Color(0xFFE3F7EF), Color(0xFF0B7A55)),
      purple = Accent(Color(0xFF7C5CFF), Color.White, Color(0xFF7C5CFF), Color(0xFFEFEBFF), Color(0xFF5B3FD9)),
      orange = Accent(Color(0xFFFF8A00), Color.White, Color(0xFFFF8A00), Color(0xFFFFF1E0), Color(0xFFB35A00)),
      switchOn = Color(0xFF12B886),
      inputBackground = Color(0xFFFFFFFF),
      inputBorder = Color(0xFF2F6BFF),
      navSelected = Color(0xFFEAF0FF),
      logoBackground = Color(0xFF14161B),
      logoForeground = Color(0xFFFFFFFF),
      tipBackground = Color(0xFFFFF1E0),
      tipText = Color(0xFF7A3E00),
      red = Accent(Color(0xFFE5484D), Color.White, Color(0xFFE5484D), Color(0xFFFFEBEC), Color(0xFFC62A2F)),
    )

    val Dark = MagnetColors(
      isDark = true,
      background = Color(0xFF0B0C0F),
      surface = Color(0xFF16181D),
      surfaceItem = Color(0xFF1E2026),
      outline = Color(0xFF1C1F25),
      divider = Color(0xFF24272E),
      onBackground = Color(0xFFF2F3F5),
      onSurfaceVariant = Color(0xFF9AA0AB),
      blue = Accent(Color(0xFF2F6BFF), Color.White, Color(0xFF6E9BFF), Color(0x294C82FF), Color(0xFF8FB0FF)),
      pink = Accent(Color(0xFFE6407F), Color.White, Color(0xFFFF5C99), Color(0x29FF5C99), Color(0xFFFF8FB8)),
      green = Accent(Color(0xFF13A877), Color.White, Color(0xFF2BD49A), Color(0x242BD49A), Color(0xFF6FE3B9)),
      purple = Accent(Color(0xFF7158F0), Color.White, Color(0xFFA592FF), Color(0x299B85FF), Color(0xFFBBAEFF)),
      orange = Accent(Color(0xFFFFA23A), Color(0xFF2A1500), Color(0xFFFFA23A), Color(0x29FFA23A), Color(0xFFFFC27A)),
      switchOn = Color(0xFF20B985),
      inputBackground = Color(0xFF0B0C0F),
      inputBorder = Color(0xFF4C82FF),
      navSelected = Color(0x2E4C82FF),
      logoBackground = Color(0xFFF2F3F5),
      logoForeground = Color(0xFF0B0C0F),
      tipBackground = Color(0x1FFFA23A),
      tipText = Color(0xFFFFD3A0),
      red = Accent(Color(0xFFE5484D), Color.White, Color(0xFFFF6B70), Color(0x29FF6B70), Color(0xFFFF9EA1)),
    )
  }
}

val LocalMagnetColors = staticCompositionLocalOf { MagnetColors.Light }

/** 播放页永远深色（docs/UI_SPEC.md 3.2 / 3.3） */
object PlayerColors {
  val Download = Color(0xFF3DE0A6)
  val Peers = Color(0xFFB6A6FF)
  val Buffer = Color(0xFFFFC27A)
  val SubtitleButton = Color(0xFFFF6FA3)
  val Panel = Color(0xFF121318)
  val PanelDivider = Color(0xFF1E2026)
  val PanelItem = Color(0xFF1E2026)
  val PanelText = Color(0xFFF2F3F5)
  val PanelSecondary = Color(0xFF9AA0AB)
  val PanelIcon = Color(0xFFC9CDD4)
  val Chevron = Color(0xFF6B717C)
  val Track = Color(0x33FFFFFF)
  val Downloaded = Color(0x73FFFFFF)
  val TopScrim = Color(0xB3000000)
  val BottomScrim = Color(0xCC000000)
  val RoundButton = Color(0x59000000)
}

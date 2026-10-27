package com.wenjin.magnetplayer.player

import com.wenjin.magnetplayer.preferences.preference.PreferenceStore
import com.wenjin.magnetplayer.preferences.preference.getEnum
import `is`.xyz.mpv.MPVLib

/** 字幕字号（小 / 标准 / 大），值为 mpv sub-font-size（720 高度下的缩放像素） */
enum class SubtitleSize(val label: String, val fontSize: Int) {
  Small("小", 26),
  Normal("标准", 30),
  Large("大", 36),
}

/** 画面比例 */
enum class AspectMode(val label: String) {
  Fit("适应屏幕"),
  Crop("裁剪铺满"),
  Stretch("拉伸铺满"),
  Ratio16x9("16:9"),
  Ratio4x3("4:3"),
}

class PlayerUiPreferences(store: PreferenceStore) {
  val subtitleSize = store.getEnum("player_subtitle_size", SubtitleSize.Normal)
  val bilingual = store.getBoolean("player_subtitle_bilingual", true)
}

/**
 * mpv 命令与属性（名称以 mpv 官方手册 DOCS/man/input.rst、options.rst 为准，
 * 对应 libmpv v0.41 之后的版本）。
 */
object Mpv {
  fun setPause(paused: Boolean) = MPVLib.setPropertyBoolean("pause", paused)

  /** seek <target> [relative|absolute] */
  fun seekBy(seconds: Double) = MPVLib.command("seek", seconds.toString(), "relative")

  fun seekTo(seconds: Double) = MPVLib.command("seek", seconds.toString(), "absolute")

  fun setSpeed(speed: Double) = MPVLib.setPropertyDouble("speed", speed)

  /** sid / secondary-sid：轨道 ID，或 "no" 关闭 */
  fun selectSubtitle(id: Int) = MPVLib.setPropertyString("sid", if (id > 0) id.toString() else "no")

  fun selectSecondarySubtitle(id: Int) =
    MPVLib.setPropertyString("secondary-sid", if (id > 0) id.toString() else "no")

  fun selectAudio(id: Int) = MPVLib.setPropertyString("aid", id.toString())

  /** 字幕时间偏移（秒），主副字幕一起调 */
  fun setSubtitleDelay(seconds: Double) {
    MPVLib.setPropertyDouble("sub-delay", seconds)
    MPVLib.setPropertyDouble("secondary-sub-delay", seconds)
  }

  /** sub-add <url> [<flags> [<title> [<lang>]]]，flags: select / auto / cached */
  fun addSubtitle(url: String, select: Boolean, title: String, lang: String?) {
    val args = mutableListOf("sub-add", url, if (select) "select" else "auto", title)
    if (!lang.isNullOrBlank()) args += lang
    MPVLib.command(*args.toTypedArray())
  }

  fun setHardwareDecoding(enabled: Boolean) =
    MPVLib.setPropertyString("hwdec", if (enabled) "mediacodec,mediacodec-copy,no" else "no")

  fun setAspect(mode: AspectMode) {
    MPVLib.setPropertyString("keepaspect", if (mode == AspectMode.Stretch) "no" else "yes")
    MPVLib.setPropertyDouble("panscan", if (mode == AspectMode.Crop) 1.0 else 0.0)
    MPVLib.setPropertyString(
      "video-aspect-override",
      when (mode) {
        AspectMode.Ratio16x9 -> "16:9"
        AspectMode.Ratio4x3 -> "4:3"
        else -> "no"
      },
    )
  }

  /**
   * 字幕样式（设计稿：中文 26sp / 600、原文 17sp 85% 白，黑色柔和阴影，双语时原文在中文上方）。
   * 只影响文本字幕和样式被覆盖的部分，ASS 字幕自带的特效样式保持不变。
   */
  fun applySubtitleStyle(size: SubtitleSize) {
    val font = size.fontSize
    MPVLib.setPropertyString("sub-font-size", font.toString())
    MPVLib.setPropertyString("sub-bold", "yes")
    MPVLib.setPropertyString("sub-color", "#FFFFFFFF")
    MPVLib.setPropertyString("sub-border-style", "outline-and-shadow")
    MPVLib.setPropertyString("sub-outline-size", "1.2")
    MPVLib.setPropertyString("sub-outline-color", "#99000000")
    MPVLib.setPropertyString("sub-shadow-offset", "1.5")
    MPVLib.setPropertyString("sub-back-color", "#CC000000")
    MPVLib.setPropertyString("sub-blur", "0.8")
    MPVLib.setPropertyString("sub-scale", "1")
    // 底边距：设计稿控件隐藏时字幕底部距屏幕 64dp（800dp 高 → 720 缩放下约 58）
    MPVLib.setPropertyString("sub-margin-y", "58")
    MPVLib.setPropertyString("sub-pos", "100")
    // 原文字号 17 / 26 ≈ 0.65；secondary-sub-pos 默认 0（顶部），改为紧贴在中文上方
    MPVLib.setPropertyString("secondary-sub-scale", "0.65")
    val lineFraction = font * 1.3 / 720.0 * 100
    MPVLib.setPropertyString("secondary-sub-pos", "%.1f".format(java.util.Locale.US, 100 - lineFraction))
  }

  /** 控件显示时把字幕抬高到控件上方（设计稿 138dp），隐藏时恢复；sub-margin-y-offset 专为运行时调整设计 */
  fun setSubtitleRaised(raised: Boolean) {
    MPVLib.setPropertyString("sub-margin-y-offset", if (raised) "66" else "0")
  }
}

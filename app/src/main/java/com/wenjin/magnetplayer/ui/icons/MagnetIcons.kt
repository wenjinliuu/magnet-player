package com.wenjin.magnetplayer.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 设计稿（docs/design 下的 HTML）里的 SVG 图标，路径原样照搬，viewBox 24×24。
 * 颜色由 Icon 的 tint 决定。
 */
object MagnetIcons {
  val Play = filled("play", "M8 5.5v13l11-6.5z")
  val Pause = filled("pause", rect(6.5f, 5f, 3.8f, 14f, 1.2f) + rect(13.7f, 5f, 3.8f, 14f, 1.2f))
  val Home = stroked("home", 2f, "M3 10.5 12 3l9 7.5", "M5 9.5V20h14V9.5")
  val Recent = stroked("recent", 2f, circle(12f, 12f, 9f), "M12 7v5l3 2")
  val Cache = stroked("cache", 2f, rect(3f, 5f, 18f, 14f, 3f), "M7 15h.01M11 15h6")
  val Settings = stroked("settings", 2f, "M4 7h10M18 7h2M4 17h2M10 17h10", circle(16f, 7f, 2f), circle(8f, 17f, 2f))
  val Link = stroked(
    "link",
    2f,
    "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7L11.5 6.8",
    "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1.5-1.5",
  )
  val Paste = stroked("paste", 2f, rect(6f, 4f, 12f, 17f, 2f), "M9 4h6v3H9z")
  val File = stroked("file", 2f, "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z", "M14 3v5h5")
  val Bolt = stroked("bolt", 2f, "M13 3 5 13h6l-1 8 8-10h-6z")
  val VideoFile = stroked("video_file", 2f, rect(4f, 3f, 16f, 18f, 3f), "M10 9.5v5l4-2.5z")
  val SubtitleFile = stroked("subtitle_file", 2f, rect(4f, 3f, 16f, 18f, 3f), "M8 12h8M8 16h5")
  val OtherFile = stroked("other_file", 2f, rect(4f, 3f, 16f, 18f, 3f), "M8 9h8M8 13h8M8 17h4")
  val ChevronRight = stroked("chevron_right", 2f, "M9 5l7 7-7 7")
  val ChevronDown = stroked("chevron_down", 2f, "M5 9l7 7 7-7")
  val Back = stroked("back", 2f, "M15 5l-7 7 7 7")
  val Download = stroked("download", 2.2f, "M12 4v13M6 12l6 6 6-6")
  val Peers = stroked(
    "peers",
    2f,
    circle(9f, 8f, 3.2f),
    "M3 19c.8-3.2 3.2-5 6-5s5.2 1.8 6 5",
    "M16 5a3 3 0 0 1 0 6M18 14c1.6.6 2.6 2.2 3 5",
  )
  val Subtitles = stroked("subtitles", 2f, rect(3f, 5f, 18f, 14f, 3f), "M10 10.5a2 2 0 1 0 0 3M16 10.5a2 2 0 1 0 0 3")
  val More = filled("more", circle(12f, 5f, 1.8f) + circle(12f, 12f, 1.8f) + circle(12f, 19f, 1.8f))
  val Replay = stroked("replay", 1.8f, "M4 12a8 8 0 1 0 2.3-5.6", "M4 4v4h4")
  val Forward = stroked("forward", 1.8f, "M20 12a8 8 0 1 1-2.3-5.6", "M20 4v4h-4")
  val Lock = stroked("lock", 1.8f, rect(5f, 11f, 14f, 10f, 2.5f), "M8 11V8a4 4 0 0 1 8 0v3")
  val Unlock = stroked("unlock", 1.8f, rect(5f, 11f, 14f, 10f, 2.5f), "M8 11V8a4 4 0 0 1 7.8-1.2")
  val Pip = stroked("pip", 1.8f, rect(3f, 5f, 18f, 14f, 3f), rect(12f, 12f, 6f, 4.5f, 1f))
  val Close = stroked("close", 2f, "M6 6l12 12M18 6 6 18")
  val Check = stroked("check", 2.4f, "M5 12.5l4.5 4.5L19 7.5")
  val SubtitleOff = stroked("subtitle_off", 2f, circle(12f, 12f, 8f), "M6.5 17.5l11-11")
  val Search = stroked("search", 2f, circle(11f, 11f, 6.5f), "M16 16l4.5 4.5")
  val Sparkle = stroked(
    "sparkle",
    2f,
    "M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M18 6l-2.5 2.5M8.5 15.5 6 18",
  )
  val Trash = stroked("trash", 2f, "M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13")
  val Info = stroked("info", 2f, circle(12f, 12f, 9f), "M12 11v5M12 8h.01")

  private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

  private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
    "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}a$rx $rx 0 0 1 ${-rx} ${rx}" +
      "h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"

  private fun stroked(name: String, width: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
      paths.forEach {
        addPath(
          pathData = addPathNodes(it),
          fill = null,
          stroke = SolidColor(Color.Black),
          strokeLineWidth = width,
          strokeLineCap = StrokeCap.Round,
          strokeLineJoin = StrokeJoin.Round,
        )
      }
    }.build()

  private fun filled(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
      addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    }.build()
}

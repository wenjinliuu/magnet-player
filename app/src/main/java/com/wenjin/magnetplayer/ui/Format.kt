package com.wenjin.magnetplayer.ui

import java.util.Locale

object Format {
  /** 文件大小：86 KB、8.2 GB */
  fun size(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
      value /= 1024
      i++
    }
    return when {
      i <= 1 -> "${value.toLong()} ${units[i]}"
      value >= 100 -> "${value.toLong()} ${units[i]}"
      else -> String.format(Locale.US, "%.1f %s", value, units[i])
    }
  }

  /** 速度：8.4 MB/s */
  fun speed(bytesPerSecond: Double): String = "${size(bytesPerSecond.toLong())}/s"

  /** 时长：42:10、1:58:00 */
  fun duration(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toLong()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
  }

  /** 去掉扩展名的标题 */
  fun title(fileName: String): String = fileName.substringAfterLast('/').substringBeforeLast('.')
}

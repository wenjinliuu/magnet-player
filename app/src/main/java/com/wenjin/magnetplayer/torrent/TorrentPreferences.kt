package com.wenjin.magnetplayer.torrent

import com.wenjin.magnetplayer.preferences.preference.PreferenceStore

class TorrentPreferences(
  preferenceStore: PreferenceStore,
) {
  /** TorrServer 内存缓存上限（MB） */
  val cacheSizeMb = preferenceStore.getInt("torrent_cache_size_mb", DEFAULT_CACHE_MB)

  /** 关闭播放后立即移除种子、清空缓存 */
  val autoClear = preferenceStore.getBoolean("torrent_auto_clear", true)

  /** 首页"拖动进度条优先下载"提示条是否已关闭 */
  val seekTipDismissed = preferenceStore.getBoolean("home_seek_tip_dismissed", false)

  companion object {
    const val DEFAULT_CACHE_MB = 300
    val CACHE_OPTIONS_MB = listOf(100, 200, 300, 500, 1000)
  }
}

package com.wenjin.magnetplayer.torrent

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class TorrentRepository(
  private val manager: TorrServerManager,
  private val preferences: TorrentPreferences,
) {
  sealed interface ResolveState {
    /** 正在启动服务或获取元数据；status 里有当前节点数等信息 */
    data class Loading(val status: TorrentStatus?) : ResolveState
    data class Ready(val info: TorrentInfo) : ResolveState
    data class Failed(val message: String) : ResolveState
  }

  /** 不随页面销毁的作用域，用于播放页关闭后的清理 */
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** 当前首页上解析出的种子；换新种子时移除旧的，避免占用内存 */
  private var currentHash: String? = null

  /** 当前种子的来源（磁力链接或 .torrent 内容），种子被自动清空后再次播放时用来重新添加 */
  private var currentSource: (suspend (TorrServerApi) -> TorrentStatus)? = null

  val api: TorrServerApi get() = manager.api

  /** 解析磁力链接（也接受 40 位 / 32 位 infohash） */
  fun resolveLink(link: String): Flow<ResolveState> {
    val magnet = normalize(link)
    return resolve(magnet) { api -> api.addLink(magnet) }
  }

  /** 解析 .torrent 文件 */
  fun resolveTorrentFile(bytes: ByteArray, fileName: String): Flow<ResolveState> =
    resolve(null) { api -> api.uploadTorrent(bytes, fileName) }

  private fun resolve(
    link: String?,
    add: suspend (TorrServerApi) -> TorrentStatus,
  ): Flow<ResolveState> = flow {
    emit(ResolveState.Loading(null))
    val api = try {
      manager.ensureRunning()
    } catch (e: Exception) {
      emit(ResolveState.Failed("边下边播服务启动失败：${e.message}"))
      return@flow
    }

    val added = try {
      add(api)
    } catch (e: Exception) {
      Log.w(TAG, "添加种子失败", e)
      emit(ResolveState.Failed(friendlyError(e)))
      return@flow
    }
    val hash = added.hash
    currentHash?.takeIf { it != hash }?.let { old -> runCatching { api.remove(old) } }
    currentHash = hash
    currentSource = add

    // 等待元数据：每秒查询一次，最多 3 分钟
    var status = added
    var waited = 0
    while (!status.hasInfo) {
      emit(ResolveState.Loading(status))
      if (waited >= METADATA_TIMEOUT_SECONDS) {
        emit(ResolveState.Failed("获取种子信息超时，可能没有可用节点"))
        return@flow
      }
      delay(1000)
      waited++
      status = runCatching { api.get(hash) }.getOrNull() ?: status
    }
    emit(ResolveState.Ready(TorrentInfo.from(status, link)))
  }

  /** 播放前调用：种子已被移除（例如看完自动清空）时重新添加 */
  suspend fun ensureAdded(hash: String) {
    val api = manager.ensureRunning()
    if (api.get(hash) == null && currentHash == hash) {
      currentSource?.let { add -> runCatching { add(api) } }
    }
  }

  /** 实时状态（速度、节点） */
  suspend fun status(hash: String): TorrentStatus? = runCatching { api.get(hash) }.getOrNull()

  /** 关闭播放页时调用：开启"看完自动清空"时移除种子，内存缓存随之释放 */
  suspend fun onPlaybackClosed(hash: String) {
    if (preferences.autoClear.get()) {
      runCatching { api.remove(hash) }
    }
  }

  fun onPlaybackClosedAsync(hash: String) {
    scope.launch { onPlaybackClosed(hash) }
  }

  /** 缓存页：TorrServer 里当前的种子及各自占用的内存缓存 */
  suspend fun activeTorrents(): List<Pair<TorrentStatus, Long>> = runCatching {
    val api = manager.ensureRunning()
    api.list().map { it to (api.cache(it.hash)?.filled ?: 0L) }
  }.getOrDefault(emptyList())

  /** 立即清空全部缓存 */
  suspend fun clearAll() {
    runCatching { manager.ensureRunning().wipe() }
  }

  private fun normalize(input: String): String {
    val link = input.trim()
    return if (HASH_REGEX.matches(link)) "magnet:?xt=urn:btih:$link" else link
  }

  private fun friendlyError(e: Exception): String {
    val msg = e.message.orEmpty()
    return when {
      "parse link" in msg -> "链接格式不对，请粘贴 magnet: 开头的磁力链接"
      "timeout" in msg.lowercase() -> "获取种子信息超时，可能没有可用节点"
      else -> "解析失败：$msg"
    }
  }

  companion object {
    private const val TAG = "TorrentRepository"
    private const val METADATA_TIMEOUT_SECONDS = 180
    private val HASH_REGEX = Regex("^([0-9a-fA-F]{40}|[A-Za-z2-7]{32})$")

    fun isTorrentLink(text: String): Boolean {
      val t = text.trim()
      return t.startsWith("magnet:?", ignoreCase = true) || HASH_REGEX.matches(t)
    }
  }
}

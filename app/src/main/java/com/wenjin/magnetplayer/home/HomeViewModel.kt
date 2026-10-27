package com.wenjin.magnetplayer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenjin.magnetplayer.data.RecentPlay
import com.wenjin.magnetplayer.data.RecentRepository
import com.wenjin.magnetplayer.player.PlayRequest
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.torrent.TorrentRepository
import com.wenjin.magnetplayer.torrent.TorrentRepository.ResolveState
import com.wenjin.magnetplayer.torrent.TorrentStatus
import com.wenjin.magnetplayer.torrent.TorrServerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ResolveUi {
  data object Idle : ResolveUi
  data class Loading(val peers: Int, val message: String) : ResolveUi
  data class Ready(val info: TorrentInfo, val peers: Int) : ResolveUi
  data class Failed(val message: String) : ResolveUi
}

class HomeViewModel(
  private val repository: TorrentRepository,
  private val manager: TorrServerManager,
  private val recentRepository: RecentRepository,
  val preferences: TorrentPreferences,
) : ViewModel() {
  private val _link = MutableStateFlow("")
  val link: StateFlow<String> = _link.asStateFlow()

  private val _resolve = MutableStateFlow<ResolveUi>(ResolveUi.Idle)
  val resolve: StateFlow<ResolveUi> = _resolve.asStateFlow()

  val recent: StateFlow<List<RecentPlay>> =
    recentRepository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val serverState: StateFlow<TorrServerManager.State> = manager.state

  /** 一次性提示（Toast） */
  private val _messages = Channel<String>(Channel.BUFFERED)
  val messages: Flow<String> = _messages.receiveAsFlow()

  /** 播放请求，由页面打开播放页 */
  private val _playRequests = Channel<PlayRequest>(Channel.BUFFERED)
  val playRequests: Flow<PlayRequest> = _playRequests.receiveAsFlow()

  private var resolveJob: Job? = null
  private var peersJob: Job? = null

  /** 从最近播放继续时，解析完成后自动播放这个文件 */
  private var pendingResume: RecentPlay? = null

  /** 已经提示过的剪贴板内容，避免每次回到前台都重复填入 */
  private var lastClipboard: String? = null

  init {
    // 提前启动 TorrServer，用户点"解析"时就不用再等
    viewModelScope.launch { runCatching { manager.ensureRunning() } }
  }

  fun onLinkChange(value: String) {
    _link.value = value
  }

  fun showMessage(message: String) {
    _messages.trySend(message)
  }

  /** App 启动或回到前台时检查剪贴板：有磁力链接就自动填入并提示 */
  fun onClipboard(text: String?) {
    val t = text?.trim().orEmpty()
    if (!TorrentRepository.isTorrentLink(t) || t == lastClipboard || t == _link.value.trim()) return
    lastClipboard = t
    _link.value = t
    showMessage("已从剪贴板填入磁力链接")
  }

  /** 从浏览器点 magnet: 链接或打开 .torrent 文件进入 App */
  fun openIncomingLink(link: String) {
    lastClipboard = link.trim()
    _link.value = link.trim()
    resolveLink()
  }

  fun resolveLink() {
    val text = _link.value.trim()
    if (text.isEmpty()) {
      showMessage("先粘贴一个磁力链接")
      return
    }
    if (!TorrentRepository.isTorrentLink(text)) {
      _resolve.value = ResolveUi.Failed("链接格式不对，请粘贴 magnet: 开头的磁力链接")
      return
    }
    pendingResume = null
    collect(repository.resolveLink(text))
  }

  fun resolveTorrentFile(bytes: ByteArray, fileName: String) {
    pendingResume = null
    _link.value = fileName
    collect(repository.resolveTorrentFile(bytes, fileName))
  }

  /** 点最近播放：重新解析种子，完成后从上次位置继续 */
  fun resumeRecent(item: RecentPlay) {
    _link.value = item.link
    collect(repository.resolveLink(item.link))
    pendingResume = item
  }

  fun deleteRecent(item: RecentPlay) {
    viewModelScope.launch { recentRepository.delete(item) }
  }

  fun clearRecent() {
    viewModelScope.launch { recentRepository.clear() }
  }

  private fun collect(flow: Flow<ResolveState>) {
    resolveJob?.cancel()
    peersJob?.cancel()
    resolveJob = viewModelScope.launch {
      flow.collect { state ->
        _resolve.value = when (state) {
          is ResolveState.Loading -> ResolveUi.Loading(
            peers = state.status?.totalPeers ?: 0,
            message = if (state.status == null) "正在启动…" else "正在获取元数据…",
          )
          is ResolveState.Ready -> {
            watchPeers(state.info.hash)
            autoResume(state.info)
            ResolveUi.Ready(state.info, state.info.peers)
          }
          is ResolveState.Failed -> ResolveUi.Failed(state.message)
        }
      }
    }
  }

  private fun autoResume(info: TorrentInfo) {
    val item = pendingResume ?: return
    pendingResume = null
    if (item.hash != info.hash) return
    val file = info.files.firstOrNull { it.id == item.fileId } ?: return
    // 快看完了就从头开始
    val start = if (item.duration > 0 && item.remaining < 30) 0.0 else item.position
    play(info, file, start)
  }

  /** 解析完成后每 2 秒刷新一次节点数 */
  private fun watchPeers(hash: String) {
    peersJob?.cancel()
    peersJob = viewModelScope.launch {
      while (isActive) {
        delay(2000)
        val status = repository.status(hash) ?: continue
        _resolve.update { if (it is ResolveUi.Ready && it.info.hash == hash) it.copy(peers = status.totalPeers) else it }
      }
    }
  }

  fun setAutoClear(enabled: Boolean) = preferences.autoClear.set(enabled)

  fun setCacheSize(mb: Int) {
    preferences.cacheSizeMb.set(mb)
    manager.onSettingsChanged()
  }

  fun dismissSeekTip() = preferences.seekTipDismissed.set(true)

  /** 播放：必要时先重新添加种子；没有指定位置时从最近播放记录里取上次的位置 */
  fun play(info: TorrentInfo, file: TorrentFile, startPosition: Double? = null) {
    viewModelScope.launch {
      runCatching { repository.ensureAdded(info.hash) }
        .onSuccess {
          val start = startPosition ?: recentRepository.get(info.hash, file.id)
            ?.takeIf { it.duration <= 0 || it.remaining >= 30 }?.position ?: 0.0
          _playRequests.send(PlayRequest(info, file, repository.api.streamUrl(info.hash, file), start))
        }
        .onFailure { showMessage("边下边播服务没有就绪：${it.message}") }
    }
  }

  // ---------- 缓存页 ----------

  suspend fun activeTorrents(): List<Pair<TorrentStatus, Long>> = repository.activeTorrents()

  fun clearAllCache(onDone: () -> Unit) {
    viewModelScope.launch {
      repository.clearAll()
      _resolve.value = ResolveUi.Idle
      showMessage("已清空全部缓存")
      onDone()
    }
  }
}

package com.wenjin.magnetplayer.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenjin.magnetplayer.torrent.TorrentFile
import com.wenjin.magnetplayer.torrent.TorrentInfo
import com.wenjin.magnetplayer.torrent.TorrentPreferences
import com.wenjin.magnetplayer.torrent.TorrentRepository
import com.wenjin.magnetplayer.torrent.TorrentRepository.ResolveState
import com.wenjin.magnetplayer.torrent.TorrServerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  val preferences: TorrentPreferences,
) : ViewModel() {
  private val _link = MutableStateFlow("")
  val link: StateFlow<String> = _link.asStateFlow()

  private val _resolve = MutableStateFlow<ResolveUi>(ResolveUi.Idle)
  val resolve: StateFlow<ResolveUi> = _resolve.asStateFlow()

  /** 一次性提示（Toast） */
  private val _messages = MutableStateFlow<String?>(null)
  val messages: StateFlow<String?> = _messages.asStateFlow()

  private var resolveJob: Job? = null
  private var peersJob: Job? = null

  init {
    // 提前启动 TorrServer，用户点"解析"时就不用再等
    viewModelScope.launch { runCatching { manager.ensureRunning() } }
  }

  fun onLinkChange(value: String) {
    _link.value = value
  }

  fun showMessage(message: String) {
    _messages.value = message
  }

  fun consumeMessage() {
    _messages.value = null
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
    collect(repository.resolveLink(text))
  }

  fun resolveTorrentFile(bytes: ByteArray, fileName: String) {
    _link.value = fileName
    collect(repository.resolveTorrentFile(bytes, fileName))
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
          is ResolveState.Ready -> ResolveUi.Ready(state.info, state.info.peers).also { watchPeers(state.info.hash) }
          is ResolveState.Failed -> ResolveUi.Failed(state.message)
        }
      }
    }
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

  /** 播放：必要时先重新添加种子，再回调播放地址 */
  fun play(info: TorrentInfo, file: TorrentFile, onReady: (String) -> Unit) {
    viewModelScope.launch {
      runCatching { repository.ensureAdded(info.hash) }
        .onSuccess { onReady(repository.api.streamUrl(info.hash, file)) }
        .onFailure { showMessage("边下边播服务没有就绪：${it.message}") }
    }
  }
}

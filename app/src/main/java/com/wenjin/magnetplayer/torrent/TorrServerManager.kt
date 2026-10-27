package com.wenjin.magnetplayer.torrent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * 管理 TorrServer 子进程：启动、等待就绪、写入设置、停止。
 *
 * 二进制随 APK 以 libtorrserver.so 的名字放在 jniLibs，安装时解压到 nativeLibraryDir，
 * 这个目录允许执行（Android 10+ 不允许执行应用数据目录里的文件）。
 */
class TorrServerManager(
  private val context: Context,
  private val preferences: TorrentPreferences,
) {
  sealed interface State {
    data object Stopped : State
    data object Starting : State
    data class Running(val version: String) : State
    data class Failed(val message: String) : State
  }

  val api = TorrServerApi("http://$HOST:$PORT")

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val mutex = Mutex()
  private var process: Process? = null

  private val _state = MutableStateFlow<State>(State.Stopped)
  val state: StateFlow<State> = _state.asStateFlow()

  /** 最近的 TorrServer 日志，出问题时方便排查 */
  private val logLines = ArrayDeque<String>()
  val recentLog: String get() = synchronized(logLines) { logLines.joinToString("\n") }

  /** 确保 TorrServer 正在运行并已写入设置，失败时抛出异常 */
  suspend fun ensureRunning(): TorrServerApi = mutex.withLock {
    val current = _state.value
    if (current is State.Running && api.echo() != null) return api

    _state.value = State.Starting
    try {
      val version = api.echo() ?: startProcess()
      applySettings()
      _state.value = State.Running(version)
      TorrServerService.start(context)
      return api
    } catch (e: Exception) {
      Log.e(TAG, "TorrServer 启动失败", e)
      _state.value = State.Failed(e.message ?: "未知错误")
      throw e
    }
  }

  private suspend fun startProcess(): String = withContext(Dispatchers.IO) {
    val binary = File(context.applicationInfo.nativeLibraryDir, "libtorrserver.so")
    if (!binary.exists()) error("找不到 TorrServer（${binary.path}）")

    val dataDir = File(context.filesDir, "torrserver").apply { mkdirs() }
    val tmpDir = File(context.cacheDir, "torrserver-tmp").apply { mkdirs() }

    // 参数见 TorrServer server/cmd/main.go：--port 端口、--ip 绑定地址、--path 配置目录
    val pb = ProcessBuilder(
      binary.path,
      "--port", PORT.toString(),
      "--ip", HOST,
      "--path", dataDir.path,
    ).directory(dataDir).redirectErrorStream(true)
    pb.environment()["TMPDIR"] = tmpDir.path
    pb.environment()["HOME"] = dataDir.path

    Log.i(TAG, "启动 TorrServer: ${pb.command().joinToString(" ")}")
    val p = pb.start()
    process = p
    scope.launch {
      runCatching {
        p.inputStream.bufferedReader().forEachLine { line ->
          Log.d(TAG, line)
          synchronized(logLines) {
            logLines.addLast(line)
            while (logLines.size > 200) logLines.removeFirst()
          }
        }
      }
    }

    // 轮询 /echo 等待就绪，最多 20 秒
    repeat(100) {
      api.echo()?.let { return@withContext it }
      if (!p.isAlive) {
        error("TorrServer 进程已退出（代码 ${p.exitValue()}）\n$recentLog")
      }
      delay(200)
    }
    error("TorrServer 启动超时\n$recentLog")
  }

  /**
   * 写入我们需要的设置：内存缓存（不落盘）、缓存大小、关闭 DLNA / 搜索等用不到的功能。
   * 先读取现有设置，只有有变化时才写入（写入设置会断开所有种子）。
   */
  suspend fun applySettings() {
    val current = api.getSettings()
    val wanted: Map<String, JsonElement> = mapOf(
      "CacheSize" to JsonPrimitive(preferences.cacheSizeMb.get().toLong() * 1024 * 1024),
      "UseDisk" to JsonPrimitive(false),
      "RemoveCacheOnDrop" to JsonPrimitive(true),
      "EnableDLNA" to JsonPrimitive(false),
      "EnableBonjour" to JsonPrimitive(false),
      "EnableRutorSearch" to JsonPrimitive(false),
      "EnableTorznabSearch" to JsonPrimitive(false),
      "ResponsiveMode" to JsonPrimitive(true),
      // 下载速度相关（见 server/torr/btserver.go configure）：
      // 每个种子最多连接的节点数，TorrServer 默认 25，太少
      "ConnectionsLimit" to JsonPrimitive(CONNECTIONS_LIMIT),
      // 打开 IPv6：国内家庭宽带普遍有公网 IPv6，很多节点只能通过 IPv6 连上
      "EnableIPv6" to JsonPrimitive(true),
      // 每个种子都追加内置 tracker，国内拉不到 GitHub 上的列表时也有足够的 tracker 找节点
      "RetrackersMode" to JsonPrimitive(1),
      "DefaultTrackers" to JsonPrimitive(Trackers.text),
    )
    val changed = wanted.any { (k, v) -> current[k]?.toString() != v.toString() }
    if (changed) {
      Log.i(TAG, "更新 TorrServer 设置")
      api.setSettings(JsonObject(current + wanted))
    }
  }

  /** 改了缓存上限后调用 */
  fun onSettingsChanged() {
    scope.launch {
      if (_state.value is State.Running) runCatching { mutex.withLock { applySettings() } }
    }
  }

  /** 退出 App 时停止 TorrServer，内存缓存随之清空 */
  fun stop() {
    scope.launch {
      mutex.withLock {
        runCatching { api.wipe() }
        api.shutdown()
        process?.let { p ->
          delay(500)
          if (p.isAlive) p.destroy()
        }
        process = null
        _state.value = State.Stopped
        TorrServerService.stop(context)
      }
    }
  }

  companion object {
    const val TAG = "TorrServer"
    const val HOST = "127.0.0.1"

    /** 不用 TorrServer 默认的 8090，避免和平板上另装的 TorrServe 冲突 */
    const val PORT = 18090

    const val CONNECTIONS_LIMIT = 100
  }
}

package com.wenjin.magnetplayer.torrent

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * TorrServer 的 HTTP 接口（以 MatriX.147.1 源码 server/web/api 与 docs/swagger.json 为准）：
 * - POST /torrents        action: add / get / rem / drop / list / wipe
 * - POST /torrent/upload  multipart 上传 .torrent 文件
 * - POST /settings        action: get / set
 * - POST /cache           action: get
 * - GET  /stream/{fname}?link=<hash>&index=<id>&play   播放流
 * - GET  /subs/{hash}/{id}[?track=<n>&format=original] 种子内外挂字幕
 * - GET  /echo            返回版本号
 * - GET  /shutdown        关闭服务
 */
class TorrServerApi(
  val baseUrl: String,
) {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }
  private val jsonType = "application/json; charset=utf-8".toMediaType()

  private val client = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    // add 会等待元数据，某些接口可能需要较长时间
    .readTimeout(90, TimeUnit.SECONDS)
    .build()

  private val quickClient = client.newBuilder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .readTimeout(2, TimeUnit.SECONDS)
    .build()

  class ApiException(val code: Int, message: String) : IOException(message)

  private suspend fun post(path: String, body: JsonObject, http: OkHttpClient = client): String? =
    execute(Request.Builder().url(baseUrl + path).post(body.toString().toRequestBody(jsonType)).build(), http)

  private suspend fun execute(request: Request, http: OkHttpClient = client): String? =
    withContext(Dispatchers.IO) {
      http.newCall(request).execute().use { resp ->
        when {
          resp.code == 404 -> null
          !resp.isSuccessful -> throw ApiException(resp.code, resp.body.string().ifBlank { "HTTP ${resp.code}" })
          else -> resp.body.string()
        }
      }
    }

  /** 服务是否就绪，返回 TorrServer 版本号 */
  suspend fun echo(): String? = runCatching {
    execute(Request.Builder().url("$baseUrl/echo").build(), quickClient)?.trim()
  }.getOrNull()

  suspend fun shutdown() {
    runCatching { execute(Request.Builder().url("$baseUrl/shutdown").build(), quickClient) }
  }

  suspend fun addLink(link: String): TorrentStatus {
    val body = buildJsonObject {
      put("action", "add")
      put("link", link)
    }
    return json.decodeFromString(post("/torrents", body) ?: throw ApiException(404, "添加失败"))
  }

  suspend fun uploadTorrent(bytes: ByteArray, fileName: String): TorrentStatus {
    val form = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart(
        "file",
        fileName,
        bytes.toRequestBody("application/x-bittorrent".toMediaType()),
      )
      .build()
    val text = execute(Request.Builder().url("$baseUrl/torrent/upload").post(form).build())
      ?: throw ApiException(404, "上传失败")
    // 只传一个文件时返回单个对象，多个时返回数组
    return if (text.trimStart().startsWith("[")) {
      json.decodeFromString<List<TorrentStatus>>(text).firstOrNull() ?: throw ApiException(400, "不是有效的种子文件")
    } else {
      json.decodeFromString(text)
    }
  }

  suspend fun get(hash: String): TorrentStatus? {
    val body = buildJsonObject {
      put("action", "get")
      put("hash", hash)
    }
    return post("/torrents", body)?.let { json.decodeFromString<TorrentStatus>(it) }
  }

  /** 关闭种子，释放连接和内存缓存（种子记录保留在列表里） */
  suspend fun drop(hash: String) {
    post("/torrents", buildJsonObject { put("action", "drop"); put("hash", hash) })
  }

  /** 移除种子：释放内存并删除数据库记录及磁盘残留 */
  suspend fun remove(hash: String) {
    post("/torrents", buildJsonObject { put("action", "rem"); put("hash", hash) })
  }

  /** 移除全部种子 */
  suspend fun wipe() {
    post("/torrents", buildJsonObject { put("action", "wipe") })
  }

  suspend fun getSettings(): JsonObject =
    json.decodeFromString(post("/settings", buildJsonObject { put("action", "get") }) ?: "{}")

  suspend fun setSettings(sets: JsonObject) {
    post(
      "/settings",
      buildJsonObject {
        put("action", "set")
        put("sets", sets)
      },
    )
  }

  suspend fun cache(hash: String): CacheState? = runCatching {
    val body = buildJsonObject {
      put("action", "get")
      put("hash", hash)
    }
    post("/cache", body, quickClient)?.let { json.decodeFromString<CacheState>(it) }
  }.getOrNull()

  suspend fun subtitles(hash: String, fileId: Int): List<SubTrack> = runCatching {
    execute(Request.Builder().url("$baseUrl/subs/$hash/$fileId").build())
      ?.let { json.decodeFromString<List<SubTrack>>(it) }
  }.getOrNull().orEmpty()

  /** 播放地址；文件名放在路径里，方便 mpv 按扩展名识别格式 */
  fun streamUrl(hash: String, file: TorrentFile): String =
    "$baseUrl/stream/${Uri.encode(file.name)}?link=$hash&index=${file.id}&play"

  /** 字幕地址：format=original 返回原始 ASS / SRT（保留 ASS 特效），编码统一转为 UTF-8 */
  fun subtitleUrl(track: SubTrack): String = "$baseUrl${track.url}&format=original"

  /** 直接读取种子内某个文件（例如字幕），不经过视频匹配 */
  fun fileUrl(hash: String, file: TorrentFile): String = streamUrl(hash, file)
}

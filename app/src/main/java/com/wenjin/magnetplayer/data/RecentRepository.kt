package com.wenjin.magnetplayer.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RecentRepository(
  private val dao: RecentPlayDao,
) {
  val all: Flow<List<RecentPlay>> = dao.observeAll()

  suspend fun get(hash: String, fileId: Int): RecentPlay? = dao.get(hash, fileId)

  /** 播放页退出时保存，不随页面销毁取消 */
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  suspend fun save(item: RecentPlay) = dao.upsert(item)

  fun saveAsync(item: RecentPlay) {
    scope.launch { dao.upsert(item) }
  }

  suspend fun delete(item: RecentPlay) = dao.delete(item.hash, item.fileId)

  suspend fun clear() = dao.clear()
}

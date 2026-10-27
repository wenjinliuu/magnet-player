package com.wenjin.magnetplayer.data

import kotlinx.coroutines.flow.Flow

class RecentRepository(
  private val dao: RecentPlayDao,
) {
  val all: Flow<List<RecentPlay>> = dao.observeAll()

  suspend fun get(hash: String, fileId: Int): RecentPlay? = dao.get(hash, fileId)

  suspend fun save(item: RecentPlay) = dao.upsert(item)

  suspend fun delete(item: RecentPlay) = dao.delete(item.hash, item.fileId)

  suspend fun clear() = dao.clear()
}

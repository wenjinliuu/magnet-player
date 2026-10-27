package com.wenjin.magnetplayer.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 最近播放：只存 infohash、磁力链接、文件序号、标题和进度，不保存种子元数据和视频数据。
 */
@Entity(tableName = "recent_play", primaryKeys = ["hash", "fileId"])
data class RecentPlay(
  val hash: String,
  val fileId: Int,
  /** 重新打开用的磁力链接（.torrent 文件来源时为只含 infohash 的磁力链接） */
  val link: String,
  val torrentTitle: String,
  val fileName: String,
  val fileLength: Long,
  /** 播放位置（秒） */
  val position: Double,
  /** 总时长（秒），未知时为 0 */
  val duration: Double,
  val updatedAt: Long,
) {
  val progress: Float get() = if (duration > 0) (position / duration).toFloat().coerceIn(0f, 1f) else 0f
  val remaining: Double get() = (duration - position).coerceAtLeast(0.0)
}

@Dao
interface RecentPlayDao {
  @Query("SELECT * FROM recent_play ORDER BY updatedAt DESC")
  fun observeAll(): Flow<List<RecentPlay>>

  @Query("SELECT * FROM recent_play WHERE hash = :hash AND fileId = :fileId")
  suspend fun get(hash: String, fileId: Int): RecentPlay?

  @Upsert
  suspend fun upsert(item: RecentPlay)

  @Query("DELETE FROM recent_play WHERE hash = :hash AND fileId = :fileId")
  suspend fun delete(hash: String, fileId: Int)

  @Query("DELETE FROM recent_play")
  suspend fun clear()
}

@Database(entities = [RecentPlay::class], version = 1, exportSchema = true)
abstract class MagnetDatabase : RoomDatabase() {
  abstract fun recentPlayDao(): RecentPlayDao
}

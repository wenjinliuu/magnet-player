package com.wenjin.magnetplayer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wenjin.magnetplayer.database.converters.NetworkProtocolConverter
import com.wenjin.magnetplayer.database.dao.NetworkConnectionDao
import com.wenjin.magnetplayer.database.dao.PlaybackStateDao
import com.wenjin.magnetplayer.database.dao.PlaylistDao
import com.wenjin.magnetplayer.database.dao.RecentlyPlayedDao
import com.wenjin.magnetplayer.database.dao.VideoMetadataDao
import com.wenjin.magnetplayer.database.entities.PlaybackStateEntity
import com.wenjin.magnetplayer.database.entities.PlaylistEntity
import com.wenjin.magnetplayer.database.entities.PlaylistItemEntity
import com.wenjin.magnetplayer.database.entities.RecentlyPlayedEntity
import com.wenjin.magnetplayer.database.entities.VideoMetadataEntity
import com.wenjin.magnetplayer.domain.network.NetworkConnection

@Database(
  entities = [
    PlaybackStateEntity::class,
    RecentlyPlayedEntity::class,
    VideoMetadataEntity::class,
    NetworkConnection::class,
    PlaylistEntity::class,
    PlaylistItemEntity::class,
  ],
  version = 8,
  exportSchema = true,
)
@TypeConverters(NetworkProtocolConverter::class)
abstract class MpvExDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao

  abstract fun recentlyPlayedDao(): RecentlyPlayedDao

  abstract fun videoMetadataDao(): VideoMetadataDao

  abstract fun networkConnectionDao(): NetworkConnectionDao

  abstract fun playlistDao(): PlaylistDao
}

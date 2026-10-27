package com.wenjin.magnetplayer.torrent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wenjin.magnetplayer.MainActivity
import com.wenjin.magnetplayer.R

/**
 * 前台服务：TorrServer 子进程运行期间保持 App 处于前台优先级，
 * 避免切到画中画或后台时被系统回收。进程本身由 [TorrServerManager] 启停。
 */
class TorrServerService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    createChannel(this)
    val open = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.baseline_play_arrow_24)
      .setContentTitle("磁力播放器")
      .setContentText("边下边播服务运行中，退出 App 后自动停止")
      .setContentIntent(open)
      .setOngoing(true)
      .setSilent(true)
      .build()
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      notification,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
    )
    return START_NOT_STICKY
  }

  companion object {
    private const val CHANNEL_ID = "torrserver"
    private const val NOTIFICATION_ID = 18090

    private fun createChannel(context: Context) {
      val channel = NotificationChannel(CHANNEL_ID, "边下边播服务", NotificationManager.IMPORTANCE_MIN)
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun start(context: Context) {
      runCatching {
        ContextCompat.startForegroundService(context, Intent(context, TorrServerService::class.java))
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, TorrServerService::class.java))
    }
  }
}

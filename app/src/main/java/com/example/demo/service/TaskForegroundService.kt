package com.example.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.demo.App
import com.example.demo.MainActivity
import com.example.demo.R
import com.example.demo.engine.EngineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TaskForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var engineJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(null)
        startForeground(NOTIFICATION_ID, notification)

        val engine = App.instance.executionEngine
        engineJob?.cancel()
        engineJob = serviceScope.launch {
            engine.engineState.collectLatest { state ->
                val updated = buildNotification(state)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, updated)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        engineJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(state: EngineState?): Notification {
        val title = getString(R.string.notification_running)
        val content = if (state != null && state.isRunning) {
            val taskCount = state.activeTaskIds.size
            val totalProgress = state.currentProgress.values.sum()
            "活跃任务: $taskCount | 已完成请求: $totalProgress"
        } else {
            "任务执行中..."
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_task_execution),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示任务执行状态"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "task_execution"
        private const val NOTIFICATION_ID = 1001
    }
}

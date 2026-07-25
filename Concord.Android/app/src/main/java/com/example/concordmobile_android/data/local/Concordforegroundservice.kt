package com.example.concordmobile_android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.concordmobile_android.MainActivity
import com.example.concordmobile_android.R
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConcordForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
        startForeground(SERVICE_NOTIF_ID, buildServiceNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        listenForMessages()
    }

    private fun listenForMessages() {
        // inicializa o singleton se o serviço foi reiniciado pelo Android antes do app abrir
        val repository = ConcordRepository.init(applicationContext)

        scope.launch {
            repository.incomingMessage.collect { message ->
                val session = repository.session.value ?: return@collect
                if (message.fromUserId == session.userId) return@collect

                val sender = repository.cachedFriend(message.fromUserId)
                val senderName = sender?.username ?: "Concord"
                val senderPhoto = sender?.profileImageBase64
                val messageText = message.text ?: return@collect // ignora mensagens apagadas

                NotificationHelper.showMessageNotification(
                    context = applicationContext,
                    notifId = message.fromUserId,
                    senderName = senderName,
                    message = messageText,
                    profileImageBase64 = senderPhoto
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Concord")
            .setContentText("Aguardando mensagens...")
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun createServiceChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(SERVICE_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Conexão Concord",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantém a conexão ativa em segundo plano"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val SERVICE_NOTIF_ID = 1
        const val SERVICE_CHANNEL_ID = "concord_service"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ConcordForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConcordForegroundService::class.java))
        }
    }
}
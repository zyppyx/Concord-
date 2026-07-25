package com.example.concordmobile_android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.concordmobile_android.MainActivity
import com.example.concordmobile_android.R

object NotificationHelper {
    private const val CHANNEL_ID = "concord_messages"
    private const val CHANNEL_NAME = "Mensagens"

    // acumula mensagens por remetente para o MessagingStyle
    private val messageHistory = mutableMapOf<Int, MutableList<NotificationCompat.MessagingStyle.Message>>()

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Novas mensagens do Concord"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showMessageNotification(
        context: Context,
        notifId: Int,          // usa fromUserId — mesmo remetente = mesma notificação
        senderName: String,
        message: String,
        profileImageBase64: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val avatarBitmap = buildAvatar(senderName, profileImageBase64)
        val avatarIcon = IconCompat.createWithBitmap(avatarBitmap)

        val sender = Person.Builder()
            .setName(senderName)
            .setIcon(avatarIcon)
            .build()

        val newMsg = NotificationCompat.MessagingStyle.Message(
            message,
            System.currentTimeMillis(),
            sender
        )

        // acumula histórico por remetente (máx 5 mensagens)
        val history = messageHistory.getOrPut(notifId) { mutableListOf() }
        history.add(newMsg)
        if (history.size > 5) history.removeAt(0)

        val me = Person.Builder().setName("Você").build()
        val style = NotificationCompat.MessagingStyle(me)
        history.forEach { style.addMessage(it) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun clearHistory(senderId: Int) {
        messageHistory.remove(senderId)
    }

    fun clearAllHistory() {
        messageHistory.clear()
    }

    private fun buildAvatar(name: String, profileImageBase64: String?): Bitmap {
        val size = 128
        val photo: Bitmap? = profileImageBase64?.takeIf { it.isNotBlank() }?.let {
            runCatching {
                val base64 = it.substringAfter(",", it)
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        if (photo != null) {
            val scaled = Bitmap.createScaledBitmap(photo, size, size, true)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
        } else {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#5865F2")
            }
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = size * 0.4f
                textAlign = Paint.Align.CENTER
            }
            val bounds = Rect()
            val letter = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            textPaint.getTextBounds(letter, 0, letter.length, bounds)
            canvas.drawText(letter, size / 2f, size / 2f + bounds.height() / 2f, textPaint)
        }

        return output
    }
}
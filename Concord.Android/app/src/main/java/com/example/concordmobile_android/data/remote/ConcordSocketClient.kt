package com.example.concordmobile_android.data.remote

import com.example.concordmobile_android.data.model.ChatMessage
import com.example.concordmobile_android.data.model.Session
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ConcordSocketClient {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)   // keepalive: manda ping a cada 20s
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)      // sem timeout de leitura (stream contínuo)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var socket: WebSocket? = null
    private val connected = AtomicBoolean(false)
    private val intentionalDisconnect = AtomicBoolean(false)
    private val retryCount = AtomicInteger(0)

    private var currentSession: Session? = null
    private var reconnectJob: java.util.concurrent.ScheduledFuture<*>? = null
    private val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "concord-socket-reconnect").also { it.isDaemon = true }
    }

    var onMessage: (ChatMessage) -> Unit = {}
    var onStatus: (String) -> Unit = {}

    fun connect(session: Session) {
        intentionalDisconnect.set(false)
        retryCount.set(0)
        currentSession = session
        doConnect(session)
    }

    private fun doConnect(session: Session) {
        cancelPendingReconnect()
        socket?.cancel() // fecha sem esperar handshake de fechamento
        socket = null
        connected.set(false)

        val token = URLEncoder.encode(session.token, StandardCharsets.UTF_8.name())
        val request = Request.Builder()
            .url("${ConcordApiClient.WS_URL}/chat?token=$token")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected.set(true)
                retryCount.set(0)
                onStatus("Chat conectado")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    ChatMessage(
                        id = json.optIntAny("id", "Id"),
                        fromUserId = json.optIntAny("fromUserId", "FromUserId"),
                        toUserId = json.optIntAny("toUserId", "ToUserId"),
                        text = json.optStringAny("text", "Text").takeIf { it.isNotBlank() },
                        mediaBase64 = json.optStringAny("mediaBase64", "MediaBase64").takeIf { it.isNotBlank() },
                        mediaType = json.optStringAny("mediaType", "MediaType").takeIf { it.isNotBlank() },
                        fileName = json.optStringAny("fileName", "FileName", "attachmentName", "AttachmentName").takeIf { it.isNotBlank() },
                        fileUrl = json.optStringAny("fileUrl", "FileUrl", "attachmentUrl", "AttachmentUrl", "url", "Url").takeIf { it.isNotBlank() },
                        fileMimeType = json.optStringAny("fileMimeType", "FileMimeType", "mimeType", "MimeType", "contentType", "ContentType", "mediaType", "MediaType").takeIf { it.isNotBlank() },
                        fileSize = json.optLongAny("fileSize", "FileSize", "size", "Size"),
                        sentAt = json.optStringAny("sentAt", "SentAt"),
                        type = json.optStringAny("type", "Type")
                    )
                }.onSuccess { msg ->
                    onMessage(msg)
                }
                // se o JSON vier malformado, ignora silenciosamente em vez de crashar
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected.set(false)
                if (!intentionalDisconnect.get()) {
                    scheduleReconnect()
                } else {
                    onStatus("Chat desconectado")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected.set(false)
                // código 1000 = fechamento limpo (logout); qualquer outro = reconectar
                if (!intentionalDisconnect.get() && code != 1000) {
                    scheduleReconnect()
                } else {
                    onStatus("Chat desconectado")
                }
            }
        })
    }

    private fun scheduleReconnect() {
        val session = currentSession ?: run {
            onStatus("Chat desconectado")
            return
        }

        val attempt = retryCount.incrementAndGet()
        // backoff exponencial: 2s, 4s, 8s, 16s, máximo 30s
        val delaySeconds = minOf(2L * (1L shl minOf(attempt - 1, 4)), 30L)

        onStatus("Reconectando em ${delaySeconds}s...")

        reconnectJob = scheduler.schedule({
            if (!intentionalDisconnect.get()) {
                onStatus("Reconectando...")
                doConnect(session)
            }
        }, delaySeconds, TimeUnit.SECONDS)
    }

    private fun cancelPendingReconnect() {
        reconnectJob?.cancel(false)
        reconnectJob = null
    }

    fun send(toUserId: Int, text: String): Boolean {
        val current = socket ?: throw IllegalStateException("Chat desconectado.")
        if (!connected.get()) throw IllegalStateException("Chat ainda está conectando. Tente novamente em instantes.")
        return current.send(JSONObject().put("toUserId", toUserId).put("text", text).toString())
    }

    fun sendFile(
        toUserId: Int,
        mediaBase64: String,
        mediaType: String,
        fileName: String,
        caption: String?
    ): Boolean {
        val current = socket ?: throw IllegalStateException("Chat desconectado.")
        if (!connected.get()) throw IllegalStateException("Chat ainda está conectando. Tente novamente em instantes.")
        val payload = JSONObject()
            .put("toUserId", toUserId)
            .put("mediaBase64", mediaBase64)
            .put("mediaType", mediaType)
            .put("fileName", fileName)
        if (!caption.isNullOrBlank()) payload.put("text", caption)
        return current.send(payload.toString())
    }

    fun disconnect() {
        intentionalDisconnect.set(true)
        cancelPendingReconnect()
        connected.set(false)
        socket?.close(1000, "Logout")
        socket = null
        currentSession = null
        onStatus("Chat desconectado")
    }
}
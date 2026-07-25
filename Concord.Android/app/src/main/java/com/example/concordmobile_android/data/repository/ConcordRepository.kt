package com.example.concordmobile_android.data.repository

import android.content.Context
import com.example.concordmobile_android.data.model.ChatMessage
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.model.Conversation
import com.example.concordmobile_android.data.model.FriendRequest
import com.example.concordmobile_android.data.model.Session
import com.example.concordmobile_android.data.model.UserProfile
import com.example.concordmobile_android.data.local.ConversationCache
import com.example.concordmobile_android.data.local.SessionStore
import com.example.concordmobile_android.data.remote.ConcordApiClient
import com.example.concordmobile_android.data.remote.ConcordSocketClient
import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class ConcordRepository private constructor(
    private val api: ConcordApiClient = ConcordApiClient(),
    private val socket: ConcordSocketClient = ConcordSocketClient(),
    private val sessionStore: SessionStore? = null,
    private val conversationCache: ConversationCache? = null
) {
    private val _session = MutableStateFlow(sessionStore?.read())
    val session: StateFlow<Session?> = _session

    private val _socketStatus = MutableStateFlow("Offline")
    val socketStatus: StateFlow<String> = _socketStatus

    private val _incomingMessage = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessage: SharedFlow<ChatMessage> = _incomingMessage

    private val friendCache = MutableStateFlow<List<Contact>>(emptyList())

    init {
        socket.onStatus = { _socketStatus.value = it }
        socket.onMessage = { message ->
            if (message.type != "status") {
                _incomingMessage.tryEmit(message)
            }
        }
        _session.value?.let { savedSession ->
            socket.connect(savedSession)
        }
    }

    suspend fun login(identifier: String, password: String): Session {
        val session = api.login(identifier, password)
        _session.value = session
        sessionStore?.save(session)
        socket.connect(session)
        return session
    }

    suspend fun register(username: String, email: String, password: String): String {
        return api.register(username, email, password)
    }

    fun logout() {
        socket.disconnect()
        val userId = _session.value?.userId
        sessionStore?.clear()
        if (userId != null) conversationCache?.clear(userId)
        _session.value = null
        friendCache.value = emptyList()
        _socketStatus.value = "Offline"
    }

    suspend fun friends(): List<Contact> {
        val current = requireSession()
        return api.getFriends(current).also { friendCache.value = it }
    }

    suspend fun conversations(): List<Conversation> {
        val current = requireSession()
        val friends = friends()
        val result = friends.map { friend ->
            val last = runCatching { api.getMessages(current, friend.id).lastOrNull() }.getOrNull()
            Conversation(
                contact = friend,
                lastMessage = last?.text ?: "Toque para conversar",
                lastMessageTime = formatShortTime(last?.sentAt),
                unreadCount = 0
            )
        }
        conversationCache?.save(current.userId, result)
        return result
    }

    fun cachedConversations(): List<Conversation> {
        val userId = _session.value?.userId ?: return emptyList()
        return conversationCache?.load(userId) ?: emptyList()
    }

    suspend fun messages(friendId: Int): List<ChatMessage> {
        return api.getMessages(requireSession(), friendId)
    }

    fun sendMessage(friendId: Int, text: String): ChatMessage {
        val current = requireSession()
        if (!socket.send(friendId, text)) {
            throw IllegalStateException("Nao foi possivel enviar a mensagem agora.")
        }
        return ChatMessage(
            fromUserId = current.userId,
            toUserId = friendId,
            text = text,
            sentAt = OffsetDateTime.now().toString(),
            localId = UUID.randomUUID().toString()
        )
    }

    suspend fun sendFile(
        friendId: Int,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        caption: String?
    ): ChatMessage {
        val current = requireSession()
        val mediaBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (!socket.sendFile(friendId, mediaBase64, mimeType, fileName, caption)) {
            throw IllegalStateException("Nao foi possivel enviar o arquivo agora.")
        }
        return ChatMessage(
            fromUserId = current.userId,
            toUserId = friendId,
            text = caption,
            mediaBase64 = mediaBase64,
            mediaType = mimeType,
            fileName = fileName,
            fileMimeType = mimeType,
            fileSize = bytes.size.toLong(),
            sentAt = OffsetDateTime.now().toString(),
            type = "message",
            localId = UUID.randomUUID().toString()
        )
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        return api.searchUsers(query)
    }

    suspend fun sendFriendRequest(friendId: Int): String {
        return api.sendFriendRequest(requireSession(), friendId)
    }

    suspend fun incomingRequests(): List<FriendRequest> {
        return api.incomingRequests(requireSession())
    }

    suspend fun acceptRequest(requestId: Int): String {
        return api.acceptFriendRequest(requireSession(), requestId)
    }

    suspend fun declineRequest(requestId: Int): String {
        return api.declineFriendRequest(requireSession(), requestId)
    }

    suspend fun removeFriend(friendId: Int): String {
        val result = api.removeFriend(requireSession(), friendId)
        friendCache.update { friends -> friends.filterNot { it.id == friendId } }
        return result
    }

    suspend fun uploadProfilePhoto(imageBytes: ByteArray, mimeType: String): String {
        return api.uploadProfilePhoto(requireSession(), imageBytes, mimeType)
    }

    suspend fun deleteAccount() {
        api.deleteAccount(requireSession())
        logout()
    }

    suspend fun deleteMessage(messageId: Int, forEveryone: Boolean) {
        api.deleteMessage(requireSession(), messageId, forEveryone)
    }

    suspend fun ownProfile(): UserProfile? {
        val session = _session.value ?: return null
        return api.getProfile(session)
    }

    fun cachedFriend(friendId: Int): Contact? {
        return friendCache.value.firstOrNull { it.id == friendId }
    }

    private fun requireSession(): Session {
        return _session.value ?: throw IllegalStateException("Entre na conta para continuar.")
    }

    private fun formatShortTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val odt = OffsetDateTime.parse(raw)
            val local = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime()
            String.format("%02d:%02d", local.hour, local.minute)
        } catch (_: Exception) {
            raw.substringAfter("T", raw).take(5)
        }
    }

    companion object {
        @Volatile
        private var _instance: ConcordRepository? = null

        // retorna a instância existente — usada pelo ForegroundService
        val instance: ConcordRepository
            get() = _instance ?: error("ConcordRepository não foi inicializado. Chame init() primeiro.")

        // inicializa uma única vez — chamado pelo ConcordApp/ConcordContainer
        fun init(context: Context): ConcordRepository {
            return _instance ?: synchronized(this) {
                _instance ?: ConcordRepository(
                    sessionStore = SessionStore(context.applicationContext),
                    conversationCache = ConversationCache(context.applicationContext)
                ).also { _instance = it }
            }
        }
    }
}

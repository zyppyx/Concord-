package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concordmobile_android.data.model.ChatMessage
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val currentUserId: Int = 0,
    val contact: Contact? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val loading: Boolean = false,
    val sendingFile: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val repository: ConcordRepository,
    private val friendId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState(currentUserId = repository.session.value?.userId ?: 0))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            repository.incomingMessage.collectLatest { message ->
                val belongsHere = message.fromUserId == friendId || message.toUserId == friendId
                if (belongsHere) {
                    _uiState.update { state ->
                        val existingIndex = state.messages.indexOfFirst { existing ->
                            isSameMessage(existing, message)
                        }
                        if (existingIndex == -1) {
                            state.copy(messages = state.messages + message)
                        } else {
                            // já existe localmente (otimista) — só atualiza com o ID real do servidor
                            val updated = state.messages.toMutableList()
                            updated[existingIndex] = message
                            state.copy(messages = updated)
                        }
                    }
                }
            }
        }
    }

    fun updateInput(value: String) = _uiState.update { it.copy(input = value) }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    contact = repository.cachedFriend(friendId)
                )
            }
            runCatching { repository.messages(friendId) }
                .onSuccess { messages ->
                    _uiState.update { it.copy(loading = false, messages = messages) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Erro ao carregar mensagens.")) }
                }
        }
    }

    fun send() {
        val text = _uiState.value.input.trim()
        if (text.isBlank()) return

        runCatching {
            repository.sendMessage(friendId, text)
        }.onSuccess { sent ->
            _uiState.update { it.copy(input = "", messages = it.messages + sent, error = null) }
        }.onFailure { error ->
            _uiState.update { it.copy(error = error.friendlyMessage("Erro ao enviar mensagem.")) }
        }
    }

    fun sendFile(fileName: String, mimeType: String, bytes: ByteArray) {
        if (bytes.isEmpty() || _uiState.value.sendingFile) return
        val caption = _uiState.value.input.trim().takeIf { it.isNotBlank() }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(sendingFile = true, error = null) }
            runCatching {
                repository.sendFile(friendId, fileName, mimeType, bytes, caption)
            }.onSuccess { sent ->
                _uiState.update {
                    it.copy(
                        input = if (caption != null) "" else it.input,
                        sendingFile = false,
                        messages = it.messages + sent,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        sendingFile = false,
                        error = error.friendlyMessage("Erro ao enviar arquivo.")
                    )
                }
            }
        }
    }

    fun deleteMessage(messageId: Int, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.deleteMessage(messageId, forEveryone) }
                .onSuccess {
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == messageId) {
                                if (forEveryone) msg.copy(deletedForEveryone = true, text = null)
                                else msg.copy(deletedForMe = true)
                            } else msg
                        }.filterNot { it.deletedForMe }
                        state.copy(messages = updated)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.friendlyMessage("Não foi possível apagar a mensagem.")) }
                }
        }
    }

    // Compara a mensagem otimista local (enviada pelo próprio app, ainda sem id real)
    // com a mensagem que voltou do servidor pelo socket — evita duplicar texto, imagem,
    // áudio ou qualquer arquivo quando o servidor "ecoa" a mensagem de volta pro remetente.
    private fun isSameMessage(local: ChatMessage, incoming: ChatMessage): Boolean {
        // já tem id real dos dois lados — compara direto
        if (local.id != 0 && incoming.id != 0) return local.id == incoming.id

        // local ainda é otimista (id == 0): só pode ser duplicata de algo que EU enviei
        if (local.id != 0) return false
        if (local.fromUserId != incoming.fromUserId || local.toUserId != incoming.toUserId) return false

        // normaliza null/"" como equivalentes, pra não depender de como cada lado serializa
        fun String?.norm() = this?.takeIf { it.isNotBlank() }

        val sameText = local.text.norm() == incoming.text.norm()
        val localMedia = local.mediaBase64.norm()
        val incomingMedia = incoming.mediaBase64.norm()
        val sameMedia = when {
            localMedia == null && incomingMedia == null -> true
            (localMedia == null) != (incomingMedia == null) -> false
            else -> localMedia == incomingMedia
        }

        return sameText && sameMedia
    }
}
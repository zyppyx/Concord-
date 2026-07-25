package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concordmobile_android.data.model.Conversation
import com.example.concordmobile_android.data.model.Session
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val session: Session? = null,
    val conversations: List<Conversation> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val socketStatus: String = "Offline",
    val photoMessage: String? = null,
    val ownProfileImage: String? = null
)

class HomeViewModel(private val repository: ConcordRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(session = repository.session.value))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.socketStatus.collectLatest { status ->
                _uiState.update { it.copy(socketStatus = status) }
            }
        }
        viewModelScope.launch {
            repository.incomingMessage.collectLatest { message ->
                _uiState.update { state ->
                    val updated = state.conversations.map { conversation ->
                        if (conversation.contact.id == message.fromUserId || conversation.contact.id == message.toUserId) {
                            conversation.copy(
                                lastMessage = message.text ?: "",
                                lastMessageTime = "agora"
                            )
                        } else {
                            conversation
                        }
                    }
                    state.copy(conversations = updated)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            // mostra cache imediatamente se tiver
            val cached = repository.cachedConversations()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(conversations = cached, session = repository.session.value) }
            } else {
                _uiState.update { it.copy(loading = true, error = null, session = repository.session.value) }
            }

            // busca da API em segundo plano
            runCatching { repository.conversations() }
                .onSuccess { conversations ->
                    _uiState.update { it.copy(loading = false, conversations = conversations) }
                }
                .onFailure { error ->
                    // se já tem cache, não mostra erro — o usuário já está vendo dados
                    if (cached.isEmpty()) {
                        _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Erro ao carregar conversas.")) }
                    } else {
                        _uiState.update { it.copy(loading = false) }
                    }
                }

            // busca foto do próprio usuário em paralelo
            runCatching { repository.ownProfile() }
                .onSuccess { profile ->
                    _uiState.update { it.copy(ownProfileImage = profile?.profileImageBase64) }
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = HomeUiState()
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.deleteAccount() }
                .onSuccess {
                    _uiState.value = HomeUiState()
                    onDeleted()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Não foi possível excluir a conta.")) }
                }
        }
    }

    fun uploadPhoto(imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(photoMessage = null, error = null) }
            runCatching { repository.uploadProfilePhoto(imageBytes, mimeType) }
                .onSuccess {
                    _uiState.update { it.copy(photoMessage = "Foto atualizada!") }
                    // recarrega conversas para refletir novo avatar em cache
                    refresh()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.friendlyMessage("Não foi possível atualizar a foto.")) }
                }
        }
    }
}
package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.model.UserProfile
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactsUiState(
    val friends: List<Contact> = emptyList(),
    val query: String = "",
    val searchResults: List<UserProfile> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ContactsViewModel(private val repository: ConcordRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        refreshFriends()
    }

    fun updateQuery(value: String) = _uiState.update { it.copy(query = value, error = null) }

    fun refreshFriends() {
        if (repository.session.value == null) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.friends() }
                .onSuccess { friends -> _uiState.update { it.copy(loading = false, friends = friends) } }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Erro ao carregar amigos.")) } }
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null, message = null, searchResults = emptyList()) }
            runCatching { repository.searchUsers(query) }
                .onSuccess { users ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            searchResults = users,
                            message = if (users.isEmpty()) "Nenhum usuário encontrado." else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Busca nao encontrou usuarios.")) }
                }
        }
    }

    fun addFriend(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.sendFriendRequest(userId) }
                .onSuccess { message -> _uiState.update { it.copy(message = message.ifBlank { "Pedido enviado!" }) } }
                .onFailure { error -> _uiState.update { it.copy(error = error.friendlyMessage("Nao foi possivel enviar o pedido.")) } }
        }
    }

    fun removeFriend(friendId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.removeFriend(friendId) }
                .onSuccess {
                    _uiState.update { state -> state.copy(friends = state.friends.filterNot { it.id == friendId }, message = "Amigo removido.") }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.friendlyMessage("Remocao ainda nao esta disponivel na API.")) } }
        }
    }
}
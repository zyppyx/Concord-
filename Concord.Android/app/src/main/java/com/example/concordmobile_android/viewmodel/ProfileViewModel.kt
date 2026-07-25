package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val contact: Contact? = null,
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val repository: ConcordRepository,
    friendId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(contact = repository.cachedFriend(friendId)))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun removeFriend() {
        val friend = _uiState.value.contact ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.removeFriend(friend.id) }
                .onSuccess { _uiState.update { it.copy(loading = false, message = "Contato removido.") } }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Remocao ainda nao esta disponivel na API.")) } }
        }
    }
}

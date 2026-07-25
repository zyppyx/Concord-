package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concordmobile_android.data.model.FriendRequest
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestsUiState(
    val requests: List<FriendRequest> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class RequestsViewModel(private val repository: ConcordRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repository.incomingRequests() }
                .onSuccess { requests -> _uiState.update { it.copy(loading = false, requests = requests) } }
                .onFailure { error -> _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Erro ao carregar solicitacoes.")) } }
        }
    }

    fun accept(requestId: Int) = answer(requestId, accept = true)
    fun decline(requestId: Int) = answer(requestId, accept = false)

    private fun answer(requestId: Int, accept: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (accept) repository.acceptRequest(requestId) else repository.declineRequest(requestId)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        requests = state.requests.filterNot { it.id == requestId },
                        message = if (accept) "Pedido aceito!" else "Pedido recusado."
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.friendlyMessage("Nao foi possivel responder ao pedido.")) }
            }
        }
    }
}

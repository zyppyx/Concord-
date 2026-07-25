package com.example.concordmobile_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concordmobile_android.data.repository.ConcordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val identifier: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val loggedIn: Boolean = false,
    val registered: Boolean = false
)

class AuthViewModel(private val repository: ConcordRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateIdentifier(value: String) = _uiState.update { it.copy(identifier = value, error = null) }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, error = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun updateConfirmPassword(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }

    fun login() {
        val state = _uiState.value
        if (state.identifier.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Preencha usuario/e-mail e senha.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                repository.login(state.identifier.trim(), state.password)
            }.onSuccess {
                _uiState.update { it.copy(loading = false, loggedIn = true, message = "Bem-vindo ao Concord!") }
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Nao foi possivel entrar.")) }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Preencha todos os campos para criar sua conta.") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "As senhas nao conferem.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                repository.register(state.username.trim(), state.email.trim(), state.password)
            }.onSuccess { message ->
                _uiState.update { it.copy(loading = false, registered = true, message = message.ifBlank { "Conta criada!" }) }
            }.onFailure { error ->
                _uiState.update { it.copy(loading = false, error = error.friendlyMessage("Nao foi possivel criar a conta.")) }
            }
        }
    }
}

internal fun Throwable.friendlyMessage(fallback: String): String {
    val raw = message?.lowercase() ?: ""
    return when {
        // sem internet / DNS
        raw.contains("unable to resolve host") ||
                raw.contains("failed to connect") ||
                raw.contains("no address associated") ||
                raw.contains("network is unreachable") ||
                raw.contains("econnrefused") ||
                raw.contains("nodename nor servname") ||
                this is java.net.UnknownHostException ||
                this is java.net.ConnectException ->
            "Sem conexão com a internet. Verifique sua rede e tente novamente."

        // timeout
        raw.contains("timeout") ||
                raw.contains("timed out") ||
                this is java.net.SocketTimeoutException ->
            "O servidor demorou para responder. Tente novamente."

        // socket fechado inesperadamente
        raw.contains("socket") ||
                raw.contains("connection reset") ||
                raw.contains("broken pipe") ->
            "A conexão foi interrompida. Tente novamente."

        // erros da API (IllegalStateException com mensagem do servidor)
        this is IllegalStateException && message?.isNotBlank() == true ->
            message!!

        // fallback
        else -> fallback
    }
}
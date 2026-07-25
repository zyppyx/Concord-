package com.example.concordmobile_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.concordmobile_android.ui.components.ConcordTextField
import com.example.concordmobile_android.ui.components.ErrorMessage
import com.example.concordmobile_android.ui.components.PrimaryActionButton
import com.example.concordmobile_android.ui.theme.ConcordPanel
import com.example.concordmobile_android.ui.theme.ConcordSurface
import com.example.concordmobile_android.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onCreateAccount: () -> Unit,
    onLoggedIn: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = ConcordSurface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Entrar no Concord!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mensagens instantâneas!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(Modifier.height(28.dp))
                    ConcordTextField(
                        value = state.identifier,
                        onValueChange = viewModel::updateIdentifier,
                        label = "Usuário ou E-Mail"
                    )
                    Spacer(Modifier.height(12.dp))
                    ConcordTextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = "Senha",
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(16.dp))
                    ErrorMessage(state.error)
                    Spacer(Modifier.height(10.dp))
                    PrimaryActionButton(
                        text = if (state.loading) "Entrando..." else "Entrar",
                        onClick = viewModel::login,
                        enabled = !state.loading
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onCreateAccount) {
                        Text("Criar Conta")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Danilo Tavares",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Copyright © 2026 Danilo Tavares, Todos os direitos reservados",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Versão 2.6.1",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.registered) {
        if (state.registered) onBackToLogin()
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().background(ConcordPanel).padding(22.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Criar Conta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Entre no Concord! e converse com seus amigos.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f))
        Spacer(Modifier.height(22.dp))
        ConcordTextField(state.username, viewModel::updateUsername, "Nome de usuário")
        Spacer(Modifier.height(10.dp))
        ConcordTextField(state.email, viewModel::updateEmail, "E-Mail")
        Spacer(Modifier.height(10.dp))
        ConcordTextField(state.password, viewModel::updatePassword, "Senha", visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        ConcordTextField(state.confirmPassword, viewModel::updateConfirmPassword, "Confirmar senha", visualTransformation = PasswordVisualTransformation())
        ErrorMessage(state.error)
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }
        PrimaryActionButton(
            text = if (state.loading) "Criando..." else "Criar Conta",
            onClick = viewModel::register,
            enabled = !state.loading
        )
        TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Voltar para login")
        }
    }
}

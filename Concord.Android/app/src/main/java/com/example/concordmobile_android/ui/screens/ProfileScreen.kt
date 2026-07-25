package com.example.concordmobile_android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.concordmobile_android.ui.components.ErrorMessage
import com.example.concordmobile_android.ui.components.ProfileAvatar
import com.example.concordmobile_android.ui.theme.ConcordPrimary
import com.example.concordmobile_android.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contact = state.contact

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ConcordPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Carregando perfil...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                contact == null -> {
                    Text("Perfil não encontrado.")
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ProfileAvatar(
                            name = contact.username,
                            modifier = Modifier.size(96.dp),
                            online = contact.isOnline,
                            imageBase64 = contact.profileImageBase64
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(contact.username, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            if (contact.isOnline) "Online" else "Offline",
                            color = if (contact.isOnline) ConcordPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (contact.email.isNotBlank()) Text(contact.email)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onBack) { Text("Enviar mensagem") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = viewModel::removeFriend, enabled = !state.loading) {
                            Text("Remover amigo")
                        }
                        ErrorMessage(state.error)
                        state.message?.let { Text(it, color = ConcordPrimary) }
                    }
                }
            }
        }
    }
}
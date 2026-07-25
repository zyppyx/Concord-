package com.example.concordmobile_android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.concordmobile_android.viewmodel.RequestsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(viewModel: RequestsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitações") },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
                actions = { TextButton(onClick = viewModel::refresh) { Text("Atualizar") } },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ErrorMessage(state.error)
            state.message?.let { Text(it, color = ConcordPrimary, modifier = Modifier.padding(16.dp)) }

            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ConcordPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Carregando solicitações...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (state.requests.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum pedido pendente.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn {
                    items(state.requests, key = { it.id }) { request ->
                        Row(Modifier.fillMaxWidth().padding(16.dp)) {
                            ProfileAvatar(request.fromUsername, imageBase64 = request.profileImageBase64)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(request.fromUsername, style = MaterialTheme.typography.titleMedium)
                                Text("Quer adicionar você.", style = MaterialTheme.typography.bodyMedium)
                                Row {
                                    Button(onClick = { viewModel.accept(request.id) }) { Text("Aceitar") }
                                    Spacer(Modifier.padding(4.dp))
                                    OutlinedButton(onClick = { viewModel.decline(request.id) }) { Text("Recusar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
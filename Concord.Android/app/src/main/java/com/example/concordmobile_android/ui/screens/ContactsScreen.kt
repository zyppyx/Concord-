package com.example.concordmobile_android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.example.concordmobile_android.ui.components.ConcordTextField
import com.example.concordmobile_android.ui.components.ContactRow
import com.example.concordmobile_android.ui.components.ErrorMessage
import com.example.concordmobile_android.ui.components.ProfileAvatar
import com.example.concordmobile_android.ui.theme.ConcordPrimary
import com.example.concordmobile_android.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contatos") },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
                actions = { TextButton(onClick = viewModel::refreshFriends) { Text("Atualizar") } },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Pesquisar usuários", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        ConcordTextField(
                            value = state.query,
                            onValueChange = viewModel::updateQuery,
                            label = "Nome do usuário",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = viewModel::search,
                        enabled = state.query.isNotBlank() && !state.loading
                    ) {
                        if (state.loading && state.friends.isNotEmpty()) {
                            // está buscando
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Buscando...")
                        } else {
                            Text("Buscar")
                        }
                    }
                    ErrorMessage(state.error)
                    state.message?.let { Text(it, color = ConcordPrimary) }
                }
            }

            items(state.searchResults, key = { "search_${it.id}" }) { user ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ProfileAvatar(user.username, imageBase64 = user.profileImageBase64)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(user.username, style = MaterialTheme.typography.titleMedium)
                        Text("ID ${user.id}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { viewModel.addFriend(user.id) }) { Text("Adicionar") }
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Amigos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(4.dp))
            }

            // loading dos amigos
            if (state.loading && state.friends.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ConcordPrimary)
                            Spacer(Modifier.height(12.dp))
                            Text("Carregando seus contatos...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else if (state.friends.isEmpty() && !state.loading) {
                item {
                    Text("Nenhum amigo ainda. Pesquise um usuário e adicione!", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            items(state.friends, key = { "friend_${it.id}" }) { contact ->
                ContactRow(
                    contact = contact,
                    trailing = {
                        TextButton(onClick = { viewModel.removeFriend(contact.id) }) { Text("Remover") }
                    },
                    onClick = { onOpenProfile(contact.id) }
                )
            }
        }
    }
}
package com.example.concordmobile_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.concordmobile_android.ui.components.ConversationRow
import com.example.concordmobile_android.ui.components.ProfileAvatar
import com.example.concordmobile_android.ui.theme.ConcordField
import com.example.concordmobile_android.ui.theme.ConcordPanel
import com.example.concordmobile_android.ui.theme.ConcordPrimary
import com.example.concordmobile_android.ui.theme.ConcordSurface
import com.example.concordmobile_android.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenChat: (Int) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAccountSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        viewModel.uploadPhoto(bytes, mimeType)
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = ConcordSurface,
            title = { Text("Excluir conta", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tem certeza? Essa ação é irreversível. Sua conta, mensagens e amizades serão apagados permanentemente.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    showAccountSheet = false
                    viewModel.deleteAccount(onLoggedOut)
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = ConcordSurface,
            contentColor = Color.White
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        name = state.session?.username ?: "C",
                        online = state.socketStatus.contains("conectado", ignoreCase = true),
                        imageBase64 = state.ownProfileImage
                    )
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(state.session?.username ?: "Concord!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.socketStatus, color = Color.White.copy(alpha = 0.62f))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Gostou do App ou Encontrou algum bug? Envie-me um FeedBack! (Email: danilo.atavares@gmail.com)",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
                SheetAction("Solicitações de amizade") { showAccountSheet = false; onOpenRequests() }
                Spacer(Modifier.height(10.dp))
                SheetAction("Alterar foto de perfil") { photoLauncher.launch("image/*") }
                state.photoMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ConcordPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                SheetAction("Sair da conta", danger = true) { showAccountSheet = false; viewModel.logout(); onLoggedOut() }
                Spacer(Modifier.height(10.dp))
                SheetAction("Excluir conta", danger = true) { showDeleteDialog = true }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        containerColor = ConcordPanel,
        bottomBar = {
            ConcordBottomBar(
                onChats = viewModel::refresh,
                onContacts = onOpenContacts,
                onAccount = { showAccountSheet = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(padding).background(ConcordPanel),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)
        ) {
            item {
                HomeHeader(onOpenContacts = onOpenContacts)
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(start = 92.dp))
            }
            state.error?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                }
            }
            // loading só aparece quando não tem cache (primeira vez)
            if (state.loading && state.conversations.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ConcordPrimary)
                            Spacer(Modifier.height(16.dp))
                            Text("Carregando conversas...", color = Color.White.copy(alpha = 0.72f))
                        }
                    }
                }
            } else if (state.conversations.isEmpty() && !state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Escolha uma conversa", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Toque em Contatos para encontrar amigos.", color = Color.White.copy(alpha = 0.62f))
                        }
                    }
                }
            } else {
                items(state.conversations, key = { it.contact.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onOpenChat(conversation.contact.id) },
                        onProfileClick = { onOpenProfile(conversation.contact.id) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(start = 92.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenContacts: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { ConcordLogo() }
        Spacer(Modifier.height(22.dp))
        Text("Conversas", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConcordLogo() {
    Surface(color = ConcordSurface.copy(alpha = 0.72f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, ConcordPrimary.copy(alpha = 0.82f))) {
        Text("Concord!", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
    }
}

@Composable
private fun SheetAction(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, color = ConcordField, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = text, modifier = Modifier.padding(16.dp), color = if (danger) Color(0xFFFF9A9A) else Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConcordBottomBar(onChats: () -> Unit, onContacts: () -> Unit, onAccount: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(color = ConcordSurface.copy(alpha = 0.96f), shape = RoundedCornerShape(36.dp), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem("Conversas", Icons.Default.Chat, onChats, selected = true)
                BottomNavItem("Contatos", Icons.Default.People, onContacts)
                BottomNavItem("Você", Icons.Default.Person, onAccount)
            }
        }
    }
}

@Composable
private fun BottomNavItem(label: String, icon: ImageVector, onClick: () -> Unit, selected: Boolean = false) {
    Surface(onClick = onClick, color = if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent, shape = RoundedCornerShape(28.dp), modifier = Modifier.widthIn(min = 96.dp)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
        }
    }
}
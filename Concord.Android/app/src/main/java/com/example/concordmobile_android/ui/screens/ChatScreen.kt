package com.example.concordmobile_android.ui.screens

import android.Manifest
import android.database.Cursor
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.content.Context
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.concordmobile_android.data.model.ChatMessage
import com.example.concordmobile_android.ui.components.MessageBubble
import com.example.concordmobile_android.ui.components.ProfileAvatar
import com.example.concordmobile_android.ui.theme.ConcordField
import com.example.concordmobile_android.ui.theme.ConcordPanel
import androidx.compose.material3.CircularProgressIndicator
import com.example.concordmobile_android.ui.theme.ConcordPrimary
import com.example.concordmobile_android.ui.theme.ConcordSidebar
import com.example.concordmobile_android.ui.theme.ConcordSurface
import com.example.concordmobile_android.viewmodel.ChatViewModel
import androidx.core.content.ContextCompat
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    val currentRecorder by rememberUpdatedState(recorder)
    val currentRecordingFile by rememberUpdatedState(recordingFile)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val fileName = resolver.displayName(uri) ?: "arquivo"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        viewModel.sendFile(fileName, mimeType, bytes)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                val (nextRecorder, nextFile) = startAudioRecording(context)
                recorder = nextRecorder
                recordingFile = nextFile
                recording = true
            }.onFailure {
                Toast.makeText(context, "Nao foi possivel iniciar o audio.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permissao de microfone negada.", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleRecording() {
        if (recording) {
            val currentRecorder = recorder
            val currentFile = recordingFile
            recorder = null
            recordingFile = null
            recording = false
            runCatching {
                currentRecorder?.stop()
                currentRecorder?.release()
                val bytes = currentFile?.readBytes() ?: ByteArray(0)
                if (bytes.isNotEmpty()) {
                    viewModel.sendFile(
                        fileName = currentFile?.name ?: "audio.m4a",
                        mimeType = "audio/mp4",
                        bytes = bytes
                    )
                }
                currentFile?.delete()
            }.onFailure {
                currentRecorder?.release()
                currentFile?.delete()
                Toast.makeText(context, "Nao foi possivel enviar o audio.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            runCatching {
                val (nextRecorder, nextFile) = startAudioRecording(context)
                recorder = nextRecorder
                recordingFile = nextFile
                recording = true
            }.onFailure {
                Toast.makeText(context, "Nao foi possivel iniciar o audio.", Toast.LENGTH_SHORT).show()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentRecorder?.release()
            currentRecordingFile?.delete()
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    // bottom sheet de opções ao segurar mensagem
    selectedMessage?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessage = null },
            containerColor = ConcordSurface,
            contentColor = Color.White
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
                Text(
                    "Apagar mensagem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // apagar para mim — qualquer mensagem
                Surface(
                    onClick = {
                        viewModel.deleteMessage(msg.id, forEveryone = false)
                        selectedMessage = null
                    },
                    color = ConcordField,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🗑 Apagar para mim", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.size(10.dp))
                // apagar para todos — só mensagens próprias
                if (msg.fromUserId == state.currentUserId) {
                    Surface(
                        onClick = {
                            viewModel.deleteMessage(msg.id, forEveryone = true)
                            selectedMessage = null
                        },
                        color = ConcordField,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑 Apagar para todos", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = Color(0xFFFF9A9A))
                    }
                    Spacer(Modifier.size(10.dp))
                }
                Surface(
                    onClick = { selectedMessage = null },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", modifier = Modifier.padding(16.dp), color = Color.White.copy(alpha = 0.6f))
                }
                Spacer(Modifier.size(24.dp))
            }
        }
    }

    Scaffold(
        containerColor = ConcordPanel,
        topBar = {
            ChatTopBar(
                title = state.contact?.username ?: "Conversa",
                subtitle = if (state.contact?.isOnline == true) "online" else "Concord!",
                imageBase64 = state.contact?.profileImageBase64,
                onBack = onBack,
                onOpenProfile = onOpenProfile
            )
        },
        bottomBar = {
            ChatComposer(
                input = state.input,
                sendingFile = state.sendingFile,
                recording = recording,
                onInputChange = viewModel::updateInput,
                onAttach = { fileLauncher.launch("*/*") },
                onAudio = ::toggleRecording,
                onSend = viewModel::send
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3A2B26), ConcordPanel, ConcordPanel)
                )
            )
        ) {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.size(12.dp))
                DatePill()
                EncryptionNotice()
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
                if (state.loading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ConcordPrimary)
                            Spacer(Modifier.size(12.dp))
                            Text("Carregando mensagens...", color = Color.White.copy(alpha = 0.72f))
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)
                ) {
                    items(state.messages.size) { index ->
                        val message = state.messages[index]
                        val mine = message.fromUserId == state.currentUserId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        // só permite apagar se tiver ID real (não local)
                                        if (message.id != 0 && !message.deletedForEveryone) {
                                            selectedMessage = message
                                        }
                                    }
                                )
                        ) {
                            MessageBubble(message = message, mine = mine)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String,
    imageBase64: String?,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ConcordSidebar).statusBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(onClick = onBack, shape = CircleShape, color = ConcordSurface, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("<", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.size(8.dp))
        Surface(onClick = onOpenProfile, color = Color.Transparent) {
            ProfileAvatar(
                name = title,
                modifier = Modifier.size(46.dp),
                online = subtitle == "online",
                imageBase64 = imageBase64
            )
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DatePill() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp)) {
            Text("Hoje", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun EncryptionNotice() {
    Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.78f)) {
            Text(
                text = "As mensagens do Concord! aparecem somente para as pessoas desta conversa.",
                color = Color(0xFFFFE0A3),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun ChatComposer(
    input: String,
    sendingFile: Boolean,
    recording: Boolean,
    onInputChange: (String) -> Unit,
    onAttach: () -> Unit,
    onAudio: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(ConcordSidebar).navigationBarsPadding().imePadding().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onAttach,
            enabled = !sendingFile,
            color = ConcordSurface,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (sendingFile) {
                    CircularProgressIndicator(color = ConcordPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.AttachFile, contentDescription = "Anexar arquivo", tint = Color.White)
                }
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ConcordField,
                unfocusedContainerColor = ConcordField,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = ConcordPrimary
            )
        )
        Surface(
            onClick = onAudio,
            enabled = !sendingFile,
            color = if (recording) Color(0xFFE25B5B) else ConcordSurface,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (recording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (recording) "Parar gravacao" else "Gravar audio",
                    tint = Color.White
                )
            }
        }
        Surface(
            onClick = onSend,
            enabled = input.isNotBlank(),
            color = if (input.isNotBlank()) ConcordPrimary else ConcordSurface,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.size(width = 72.dp, height = 44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Enviar", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun android.content.ContentResolver.displayName(uri: Uri): String? {
    val cursor: Cursor? = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
}

private fun startAudioRecording(context: Context): Pair<MediaRecorder, File> {
    val audioFile = File(context.cacheDir, "concord-audio-${System.currentTimeMillis()}.m4a")
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }

    recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(96_000)
        setAudioSamplingRate(44_100)
        setOutputFile(audioFile.absolutePath)
        prepare()
        start()
    }

    return recorder to audioFile
}
package com.example.concordmobile_android.ui.components

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.concordmobile_android.data.model.ChatMessage
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.model.Conversation
import com.example.concordmobile_android.ui.theme.ConcordBubbleMine
import com.example.concordmobile_android.ui.theme.ConcordBubbleOther
import com.example.concordmobile_android.ui.theme.ConcordField
import com.example.concordmobile_android.ui.theme.ConcordPrimary
import java.io.File

@Composable
fun ConcordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ConcordField,
            unfocusedContainerColor = ConcordField,
            focusedBorderColor = ConcordPrimary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color.White.copy(alpha = 0.76f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.56f),
            cursorColor = ConcordPrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PrimaryActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ConcordPrimary,
            contentColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ErrorMessage(message: String?) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
    }
}

@Composable
fun ProfileAvatar(
    name: String,
    modifier: Modifier = Modifier,
    online: Boolean = false,
    imageBase64: String? = null
) {
    Box(modifier = modifier.size(52.dp)) {
        val bitmap = imageBase64?.let { decodeProfileImage(it) }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(ConcordPrimary),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto de perfil de $name",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (online) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF39D98A))
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun ConversationRow(conversation: Conversation, onClick: () -> Unit, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clickable(onClick = onProfileClick)) {
            ProfileAvatar(
                name = conversation.contact.username,
                online = conversation.contact.isOnline,
                imageBase64 = conversation.contact.profileImageBase64
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = conversation.contact.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conversation.lastMessageTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unreadCount > 0) {
                    Badge(containerColor = ConcordPrimary) {
                        Text(conversation.unreadCount.toString(), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRow(contact: Contact, trailing: @Composable () -> Unit = {}, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(contact.username, online = contact.isOnline, imageBase64 = contact.profileImageBase64)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.username, fontWeight = FontWeight.SemiBold)
            Text(if (contact.isOnline) "Online" else "Offline", style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
fun MessageBubble(message: ChatMessage, mine: Boolean) {
    val hasFile = !message.fileName.isNullOrBlank() || !message.mediaBase64.isNullOrBlank()
    val isDeleted = message.deletedForEveryone || (message.text == null && !hasFile)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (mine) ConcordBubbleMine else ConcordBubbleOther,
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = if (mine) 10.dp else 3.dp,
                bottomEnd = if (mine) 3.dp else 10.dp
            ),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                if (isDeleted) {
                    Text(
                        text = "🚫 Mensagem apagada",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.5f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    if (hasFile) {
                        MediaAttachment(message = message)
                    }
                    if (!message.text.isNullOrBlank()) {
                        if (hasFile) Spacer(Modifier.height(6.dp))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = shortTime(message.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MediaAttachment(message: ChatMessage) {
    val type = message.mediaKind()
    when {
        type.startsWith("image/") -> ImageAttachment(message)
        type.startsWith("audio/") -> AudioAttachment(message)
        else -> FileAttachment(message)
    }
}

@Composable
private fun ImageAttachment(message: ChatMessage) {
    val bitmap = remember(message.mediaBase64) {
        message.mediaBase64?.let { decodeBase64Bitmap(it) }
    }

    if (bitmap == null) {
        FileAttachment(message)
        return
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = message.fileName ?: "Imagem enviada",
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun AudioAttachment(message: ChatMessage) {
    val context = LocalContext.current
    var player by remember(message.mediaBase64) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(message.mediaBase64) { mutableStateOf(false) }

    DisposableEffect(message.mediaBase64) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Surface(
        color = Color.Black.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    val current = player
                    if (isPlaying && current != null) {
                        current.stop()
                        current.release()
                        player = null
                        isPlaying = false
                        return@Surface
                    }

                    val bytes = message.mediaBase64?.let { decodeBase64Bytes(it) } ?: return@Surface
                    val audioFile = File.createTempFile("concord-audio-", ".m4a", context.cacheDir)
                    audioFile.writeBytes(bytes)
                    val nextPlayer = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        setOnCompletionListener {
                            it.release()
                            audioFile.delete()
                            player = null
                            isPlaying = false
                        }
                        prepare()
                        start()
                    }
                    player = nextPlayer
                    isPlaying = true
                },
                color = ConcordPrimary,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Parar audio" else "Tocar audio",
                        tint = Color.White
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message.fileName ?: "Audio",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isPlaying) "Reproduzindo" else "Toque para ouvir",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FileAttachment(message: ChatMessage) {
    Surface(
        color = Color.Black.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message.fileName ?: "Arquivo",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fileMeta(message),
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun fileMeta(message: ChatMessage): String {
    val size = formatBytes(message.fileSize)
    val type = message.mediaKind().substringAfterLast("/").uppercase()
    return listOf(type, size).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Arquivo" }
}

private fun ChatMessage.mediaKind(): String {
    return (fileMimeType ?: mediaType).orEmpty()
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return ""
    val units = listOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${bytes} B" else String.format(java.util.Locale.US, "%.1f %s", size, units[unitIndex])
}

fun shortTime(raw: String): String {
    if (raw.isBlank()) return "agora"
    return runCatching {
        java.time.OffsetDateTime.parse(raw)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrElse {
        raw.substringAfter("T", raw).take(5)
    }
}

private fun decodeProfileImage(value: String): android.graphics.Bitmap? {
    return runCatching {
        val bytes = decodeBase64Bytes(value)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val exif = ExifInterface(bytes.inputStream())
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (rotation == 0f) bitmap
        else {
            val matrix = Matrix().apply { postRotate(rotation) }
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }.getOrNull()
}

private fun decodeBase64Bitmap(value: String): android.graphics.Bitmap? {
    return runCatching {
        val bytes = decodeBase64Bytes(value)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun decodeBase64Bytes(value: String): ByteArray {
    val base64 = value.substringAfter(",", value)
    return Base64.decode(base64, Base64.DEFAULT)
}
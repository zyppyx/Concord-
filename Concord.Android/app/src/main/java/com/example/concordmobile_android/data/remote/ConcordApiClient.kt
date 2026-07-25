package com.example.concordmobile_android.data.remote

import com.example.concordmobile_android.data.model.ChatMessage
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.model.FriendRequest
import com.example.concordmobile_android.data.model.Session
import com.example.concordmobile_android.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ConcordApiClient {
    fun login(identifier: String, password: String): Session {
        val response = postJson(
            "$BASE_URL/login",
            JSONObject()
                .put("username", identifier)
                .put("password", password)
                .toString()
        )
        val json = JSONObject(response)
        return Session(
            userId = json.optIntAny("id", "Id"),
            username = json.optStringAny("username", "Username"),
            token = json.optStringAny("tokenString", "TokenString")
        )
    }

    fun register(username: String, email: String, password: String): String {
        // A API do WPF atual aceita username/senha; o e-mail fica preparado para evolução do backend.
        val body = JSONObject()
            .put("Username", username)
            .put("Email", email)
            .put("Password", password)
            .toString()
        val response = postJson("$BASE_URL/register", body)
        return response.ifBlank { "Conta criada com sucesso!" }
    }

    fun getFriends(session: Session): List<Contact> {
        val array = JSONArray(get("$BASE_URL/friends/${session.userId}", session.token))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Contact(
                id = item.optIntAny("id", "Id"),
                username = item.optStringAny("username", "Username"),
                profileImageBase64 = item.optStringAny("profileImageBase64", "ProfileImageBase64").takeIf { it.isNotBlank() },
                isOnline = item.optBoolean("isOnline") || item.optBoolean("IsOnline")
            )
        }
    }

    fun getMessages(session: Session, friendId: Int): List<ChatMessage> {
        val array = JSONArray(get("$BASE_URL/messages/$friendId?take=50", session.token))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ChatMessage(
                id = item.optIntAny("id", "Id"),
                fromUserId = item.optIntAny("fromUserId", "FromUserId"),
                toUserId = item.optIntAny("toUserId", "ToUserId"),
                text = item.optStringAny("text", "Text"),
                mediaBase64 = item.optStringAny("mediaBase64", "MediaBase64").takeIf { it.isNotBlank() },
                mediaType = item.optStringAny("mediaType", "MediaType").takeIf { it.isNotBlank() },
                fileName = item.optStringAny("fileName", "FileName", "attachmentName", "AttachmentName").takeIf { it.isNotBlank() },
                fileUrl = item.optStringAny("fileUrl", "FileUrl", "attachmentUrl", "AttachmentUrl", "url", "Url").takeIf { it.isNotBlank() },
                fileMimeType = item.optStringAny("fileMimeType", "FileMimeType", "mimeType", "MimeType", "contentType", "ContentType", "mediaType", "MediaType").takeIf { it.isNotBlank() },
                fileSize = item.optLongAny("fileSize", "FileSize", "size", "Size"),
                sentAt = item.optStringAny("sentAt", "SentAt"),
                type = item.optStringAny("type", "Type")
            )
        }
    }

    fun searchUsers(query: String): List<UserProfile> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name()).replace("+", "%20")
        val response = get("$BASE_URL/users/search/$encoded")
        val array = try {
            JSONArray(response)
        } catch (_: Exception) {
            // backend retornou objeto único ou string — tenta empacotar em array
            try { JSONArray("[$response]") } catch (_: Exception) { JSONArray() }
        }
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            UserProfile(
                id = item.optIntAny("id", "Id"),
                username = item.optStringAny("username", "Username"),
                accountCreationDate = item.optStringAny("accountCreationDate", "AccountCreationDate"),
                profileImageBase64 = item.optStringAny("profileImageBase64", "ProfileImageBase64").takeIf { it.isNotBlank() }
            )
        }
    }

    fun sendFriendRequest(session: Session, friendId: Int): String {
        val response = postJson(
            "$BASE_URL/friendrequests/$friendId",
            "",
            session.token
        )

        return try {
            JSONObject(response).optString(
                "message",
                "Solicitação enviada!"
            )
        } catch (_: Exception) {
            "Solicitação enviada!"
        }
    }

    fun incomingRequests(session: Session): List<FriendRequest> {
        val array = JSONArray(get("$BASE_URL/friendrequests/incoming", session.token))
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            FriendRequest(
                id = item.optIntAny("id", "Id"),
                fromUserId = item.optIntAny("fromUserId", "FromUserId"),
                fromUsername = item.optStringAny("fromUsername", "FromUsername"),
                createdAt = item.optStringAny("createdAt", "CreatedAt"),
                profileImageBase64 = item.optStringAny("fromProfileImageBase64", "FromProfileImageBase64").takeIf { it.isNotBlank() }
            )
        }
    }

    fun acceptFriendRequest(session: Session, requestId: Int): String {
        return postJson("$BASE_URL/friendrequests/$requestId/accept", "", session.token)
    }

    fun declineFriendRequest(session: Session, requestId: Int): String {
        return delete("$BASE_URL/friendrequests/$requestId", session.token)
    }

    fun removeFriend(session: Session, friendId: Int): String {
        return delete("$BASE_URL/friends/$friendId", session.token)
    }

    fun deleteAccount(session: Session): String {
        return delete("$BASE_URL/deleteuser/${session.userId}", session.token)
    }

    fun deleteMessage(session: Session, messageId: Int, forEveryone: Boolean): String {
        return delete("$BASE_URL/messages/$messageId?forEveryone=$forEveryone", session.token)
    }

    fun getProfile(session: Session): UserProfile {
        val json = org.json.JSONObject(get("$BASE_URL/profile/${session.userId}", session.token))
        return UserProfile(
            id = json.optIntAny("id", "Id"),
            username = json.optStringAny("username", "Username"),
            accountCreationDate = json.optStringAny("accountCreationDate", "AccountCreationDate"),
            profileImageBase64 = json.optStringAny("profileImageBase64", "ProfileImageBase64").takeIf { it.isNotBlank() }
        )
    }

    fun uploadProfilePhoto(session: Session, imageBytes: ByteArray, mimeType: String): String {
        val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
        val profileImageBase64 = "data:$mimeType;base64,$base64"
        val body = JSONObject().put("profileImageBase64", profileImageBase64).toString()
        return putJson("$BASE_URL/profile/photo", body, session.token)
    }

    private fun get(url: String, token: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        return read(connection)
    }

    private fun postJson(url: String, body: String, token: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        if (body.isNotEmpty()) {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
        return read(connection)
    }

    private fun putJson(url: String, body: String, token: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        return read(connection)
    }

    private fun delete(url: String, token: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Authorization", "Bearer $token")
        return read(connection)
    }

    private fun read(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(text.ifBlank { "Nao foi possivel concluir a operacao." })
        }
        return text
    }

    companion object {
        const val BASE_URL = "https://concord-api-v4xu.onrender.com"
        const val WS_URL = "wss://concord-api-v4xu.onrender.com"
    }
}

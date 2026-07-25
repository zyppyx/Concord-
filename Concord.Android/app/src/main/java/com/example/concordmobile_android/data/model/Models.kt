package com.example.concordmobile_android.data.model

data class Session(
    val userId: Int,
    val username: String,
    val token: String
)

data class Contact(
    val id: Int,
    val username: String,
    val email: String = "",
    val profileImageBase64: String? = null,
    val isOnline: Boolean = false
)

data class ChatMessage(
    val id: Int = 0,
    val fromUserId: Int,
    val toUserId: Int,
    val text: String? = null,
    val mediaBase64: String? = null,
    val mediaType: String? = null,
    val fileName: String? = null,
    val fileUrl: String? = null,
    val fileMimeType: String? = null,
    val fileSize: Long = 0L,
    val sentAt: String = "",
    val type: String = "",
    val localId: String = "",
    val deletedForEveryone: Boolean = false,
    val deletedForMe: Boolean = false
)

data class Conversation(
    val contact: Contact,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0
)

data class FriendRequest(
    val id: Int,
    val fromUserId: Int,
    val fromUsername: String,
    val createdAt: String = "",
    val profileImageBase64: String? = null
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String = "",
    val accountCreationDate: String = "",
    val profileImageBase64: String? = null
)

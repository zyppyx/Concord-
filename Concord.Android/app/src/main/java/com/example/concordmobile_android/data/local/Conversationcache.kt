package com.example.concordmobile_android.data.local

import android.content.Context
import com.example.concordmobile_android.data.model.Contact
import com.example.concordmobile_android.data.model.Conversation
import org.json.JSONArray
import org.json.JSONObject

class ConversationCache(context: Context) {
    private val prefs = context.getSharedPreferences("concord_conversations", Context.MODE_PRIVATE)

    fun save(userId: Int, conversations: List<Conversation>) {
        val array = JSONArray()
        for (conv in conversations) {
            val contact = JSONObject().apply {
                put("id", conv.contact.id)
                put("username", conv.contact.username)
                put("email", conv.contact.email)
                put("profileImageBase64", conv.contact.profileImageBase64 ?: "")
                put("isOnline", conv.contact.isOnline)
            }
            val obj = JSONObject().apply {
                put("contact", contact)
                put("lastMessage", conv.lastMessage)
                put("lastMessageTime", conv.lastMessageTime)
                put("unreadCount", conv.unreadCount)
            }
            array.put(obj)
        }
        prefs.edit().putString(key(userId), array.toString()).apply()
    }

    fun load(userId: Int): List<Conversation> {
        val raw = prefs.getString(key(userId), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val contact = obj.getJSONObject("contact")
                Conversation(
                    contact = Contact(
                        id = contact.getInt("id"),
                        username = contact.getString("username"),
                        email = contact.optString("email", ""),
                        profileImageBase64 = contact.optString("profileImageBase64", "").takeIf { it.isNotBlank() },
                        isOnline = contact.optBoolean("isOnline", false)
                    ),
                    lastMessage = obj.getString("lastMessage"),
                    lastMessageTime = obj.getString("lastMessageTime"),
                    unreadCount = obj.optInt("unreadCount", 0)
                )
            }
        }.getOrElse { emptyList() }
    }

    fun clear(userId: Int) {
        prefs.edit().remove(key(userId)).apply()
    }

    private fun key(userId: Int) = "conversations_$userId"
}
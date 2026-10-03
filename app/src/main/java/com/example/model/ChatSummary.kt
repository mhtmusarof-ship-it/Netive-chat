package com.example.model

data class ChatSummary(
    val chatId: String = "",
    val isGroup: Boolean = false,
    val title: String = "",
    val photoUrl: String? = null,
    val otherUserId: String? = null,
    val lastMessage: String = "",
    val lastMessageType: String = Message.TYPE_TEXT,
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val lastMessageStatus: String = Message.STATUS_SENT,
    val pinned: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "chatId" to chatId,
            "isGroup" to isGroup,
            "title" to title,
            "photoUrl" to photoUrl,
            "otherUserId" to otherUserId,
            "lastMessage" to lastMessage,
            "lastMessageType" to lastMessageType,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageTimestamp" to lastMessageTimestamp,
            "unreadCount" to unreadCount,
            "lastMessageStatus" to lastMessageStatus,
            "pinned" to pinned
        )
    }
}

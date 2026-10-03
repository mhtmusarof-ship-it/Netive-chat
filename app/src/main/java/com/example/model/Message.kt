package com.example.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val type: String = TYPE_TEXT, // "text", "image", "video", "audio", "document"
    val mediaUrl: String? = null,
    val mediaDurationMs: Long? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val timestamp: Long = 0L,
    val status: String = STATUS_SENT, // "sending", "sent", "delivered", "read"
    val replyToMessageId: String? = null,
    val replyToText: String? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_DOCUMENT = "document"

        const val STATUS_SENDING = "sending"
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_READ = "read"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "chatId" to chatId,
            "senderId" to senderId,
            "senderName" to senderName,
            "receiverId" to receiverId,
            "text" to text,
            "type" to type,
            "mediaUrl" to mediaUrl,
            "mediaDurationMs" to mediaDurationMs,
            "fileName" to fileName,
            "fileSize" to fileSize,
            "timestamp" to timestamp,
            "status" to status,
            "replyToMessageId" to replyToMessageId,
            "replyToText" to replyToText
        )
    }
}

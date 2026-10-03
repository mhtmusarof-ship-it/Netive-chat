package com.example.model

data class StatusItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val type: String = TYPE_TEXT, // "text", "image", "video"
    val content: String = "", // text string or media download URL
    val backgroundColorHex: String = "#075E54",
    val caption: String? = null,
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val viewerIds: List<String> = emptyList()
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "type" to type,
            "content" to content,
            "backgroundColorHex" to backgroundColorHex,
            "caption" to caption,
            "timestamp" to timestamp,
            "expiresAt" to expiresAt,
            "viewerIds" to viewerIds
        )
    }
}

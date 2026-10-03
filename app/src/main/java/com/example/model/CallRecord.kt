package com.example.model

data class CallRecord(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhotoUrl: String? = null,
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPhotoUrl: String? = null,
    val callType: String = TYPE_AUDIO, // "audio" or "video"
    val status: String = STATUS_RINGING, // "calling", "ringing", "accepted", "rejected", "missed", "ended"
    val timestamp: Long = 0L,
    val durationSeconds: Int = 0
) {
    companion object {
        const val TYPE_AUDIO = "audio"
        const val TYPE_VIDEO = "video"

        const val STATUS_CALLING = "calling"
        const val STATUS_RINGING = "ringing"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_MISSED = "missed"
        const val STATUS_ENDED = "ended"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "callId" to callId,
            "callerId" to callerId,
            "callerName" to callerName,
            "callerPhotoUrl" to callerPhotoUrl,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "receiverPhotoUrl" to receiverPhotoUrl,
            "callType" to callType,
            "status" to status,
            "timestamp" to timestamp,
            "durationSeconds" to durationSeconds
        )
    }
}

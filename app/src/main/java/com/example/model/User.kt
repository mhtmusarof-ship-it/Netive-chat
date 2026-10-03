package com.example.model

data class User(
    val uid: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val about: String = "Hey there! I am using Netice Chat.",
    val photoUrl: String? = null,
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    val fcmToken: String? = null,
    val createdAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "name" to name,
            "about" to about,
            "photoUrl" to photoUrl,
            "online" to online,
            "lastSeen" to lastSeen,
            "fcmToken" to fcmToken,
            "createdAt" to createdAt
        )
    }
}

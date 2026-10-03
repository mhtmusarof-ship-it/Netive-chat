package com.example.ui.navigation

object Routes {
    const val AUTH = "auth"
    const val PROFILE_SETUP = "profile_setup"
    const val HOME = "home"
    const val USER_SEARCH = "user_search"
    const val CREATE_GROUP = "create_group"
    const val GROUP_INFO = "group_info/{groupId}"
    const val CREATE_STATUS = "create_status"
    const val STATUS_VIEWER = "status_viewer/{userId}"
    const val SETTINGS = "settings"
    const val USER_PROFILE = "user_profile"

    // Chat route helper
    const val CHAT = "chat/{chatId}/{otherUserId}/{otherUserName}/{isGroup}?photoUrl={photoUrl}"
    fun chatRoute(
        chatId: String,
        otherUserId: String,
        otherUserName: String,
        isGroup: Boolean,
        photoUrl: String? = null
    ): String {
        val safeName = java.net.URLEncoder.encode(otherUserName, "UTF-8")
        val safePhoto = photoUrl?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
        return "chat/$chatId/$otherUserId/$safeName/$isGroup?photoUrl=$safePhoto"
    }

    // Call route helper
    const val CALL = "call/{callId}/{otherUserId}/{otherUserName}/{callType}/{isIncoming}?photoUrl={photoUrl}"
    fun callRoute(
        callId: String,
        otherUserId: String,
        otherUserName: String,
        callType: String,
        isIncoming: Boolean,
        photoUrl: String? = null
    ): String {
        val safeName = java.net.URLEncoder.encode(otherUserName, "UTF-8")
        val safePhoto = photoUrl?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
        return "call/$callId/$otherUserId/$safeName/$callType/$isIncoming?photoUrl=$safePhoto"
    }

    fun groupInfoRoute(groupId: String): String = "group_info/$groupId"
    fun statusViewerRoute(userId: String): String = "status_viewer/$userId"
}

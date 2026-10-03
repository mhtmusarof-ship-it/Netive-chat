package com.example.model

data class GroupInfo(
    val groupId: String = "",
    val groupName: String = "",
    val groupDescription: String = "",
    val groupIconUrl: String? = null,
    val createdById: String = "",
    val createdAt: Long = 0L,
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "groupId" to groupId,
            "groupName" to groupName,
            "groupDescription" to groupDescription,
            "groupIconUrl" to groupIconUrl,
            "createdById" to createdById,
            "createdAt" to createdAt,
            "adminIds" to adminIds,
            "memberIds" to memberIds
        )
    }
}

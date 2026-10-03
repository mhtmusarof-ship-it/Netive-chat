package com.example.data

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.example.model.CallRecord
import com.example.model.ChatSummary
import com.example.model.GroupInfo
import com.example.model.Message
import com.example.model.StatusItem
import com.example.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class FirebaseRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance("https://chat-app-97353-default-rtdb.firebaseio.com")
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }
    private val storage: FirebaseStorage by lazy {
        try {
            FirebaseStorage.getInstance("gs://chat-app-97353.firebasestorage.app")
        } catch (e: Exception) {
            FirebaseStorage.getInstance()
        }
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        auth.currentUser?.let { firebaseUser ->
            listenToUser(firebaseUser.uid)
            setupPresence(firebaseUser.uid)
        }
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                listenToUser(user.uid)
                setupPresence(user.uid)
            } else {
                _currentUser.value = null
            }
        }
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    // Email & Password Authentication with Email Verification
    suspend fun registerWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw IllegalStateException("User creation failed")
            // Send verification email immediately
            user.sendEmailVerification().await()
            setupPresence(user.uid)
            Result.success(user.uid)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Register error", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw IllegalStateException("Sign in failed")
            user.reload().await()
            setupPresence(user.uid)
            Result.success(user.isEmailVerified)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Sign in error", e)
            Result.failure(e)
        }
    }

    suspend fun checkEmailVerificationStatus(): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("No current user"))
            user.reload().await()
            Result.success(user.isEmailVerified)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Check verification error", e)
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("No current user"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Resend verification error", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Password reset error", e)
            Result.failure(e)
        }
    }

    suspend fun signInAnonymouslyForTesting(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: throw IllegalStateException("User ID not returned")
            setupPresence(uid)
            Result.success(uid)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Sign in anonymous error", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        currentUserId?.let { uid ->
            database.getReference("users").child(uid).child("online").setValue(false)
            database.getReference("users").child(uid).child("lastSeen").setValue(ServerValue.TIMESTAMP)
        }
        auth.signOut()
        _currentUser.value = null
    }

    // Presence management
    fun setupPresence(uid: String) {
        val connectedRef = database.getReference(".info/connected")
        val userStatusRef = database.getReference("users").child(uid)

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    val statusMap = mapOf(
                        "online" to false,
                        "lastSeen" to ServerValue.TIMESTAMP
                    )
                    userStatusRef.onDisconnect().updateChildren(statusMap)

                    userStatusRef.child("online").setValue(true)
                    userStatusRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Presence", "Presence listener cancelled: ${error.message}")
            }
        })
    }

    private fun listenToUser(uid: String) {
        database.getReference("users").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    _currentUser.value = user
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseRepo", "Failed to listen to user", error.toException())
                }
            })
    }

    suspend fun saveUserProfile(
        name: String,
        about: String,
        photoUri: Uri?,
        phoneNumber: String? = null,
        email: String? = null
    ): Result<User> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            var photoUrl: String? = _currentUser.value?.photoUrl
            if (photoUri != null) {
                photoUrl = uploadFileToStorage("profile_photos/$uid/${UUID.randomUUID()}.jpg", photoUri)
            }

            val user = User(
                uid = uid,
                email = email ?: auth.currentUser?.email ?: _currentUser.value?.email ?: "",
                phoneNumber = phoneNumber ?: auth.currentUser?.phoneNumber ?: _currentUser.value?.phoneNumber ?: "",
                name = name.ifBlank { "User" },
                about = about.ifBlank { "Hey there! I am using Netice Chat." },
                photoUrl = photoUrl,
                online = true,
                lastSeen = System.currentTimeMillis(),
                createdAt = _currentUser.value?.createdAt ?: System.currentTimeMillis()
            )

            database.getReference("users").child(uid).setValue(user.toMap()).await()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Save profile error", e)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        try {
            database.getReference("users").child(uid).child("fcmToken").setValue(token).await()
        } catch (e: Exception) {
            Log.w("FirebaseRepo", "Could not update FCM token: ${e.message}")
        }
    }

    // User Directory & Search
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val myUid = currentUserId
        val ref = database.getReference("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<User>()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null && user.uid != myUid) {
                        list.add(user)
                    }
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getUser(uid: String): Flow<User?> = callbackFlow {
        val ref = database.getReference("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(User::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Chats and Messaging
    fun getUserChats(): Flow<List<ChatSummary>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.getReference("user_chats").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatSummary>()
                for (child in snapshot.children) {
                    val chat = child.getValue(ChatSummary::class.java)
                    if (chat != null) {
                        list.add(chat)
                    }
                }
                list.sortByDescending { it.lastMessageTimestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getChatMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val ref = database.getReference("messages").child(chatId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Message>()
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null) {
                        list.add(msg)
                    }
                }
                list.sortBy { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendMessage(
        receiverId: String,
        receiverName: String,
        receiverPhoto: String?,
        text: String,
        type: String = Message.TYPE_TEXT,
        mediaFileUri: Uri? = null,
        localFile: File? = null,
        fileName: String? = null,
        durationMs: Long? = null,
        replyToMessage: Message? = null
    ): Result<Message> {
        val sender = _currentUser.value ?: return Result.failure(IllegalStateException("Sender not loaded"))
        val senderId = sender.uid
        val chatId = getChatId(senderId, receiverId)

        return try {
            val messageId = database.getReference("messages").child(chatId).push().key
                ?: UUID.randomUUID().toString()

            var mediaUrl: String? = null
            var finalFileSize: Long? = null

            if (localFile != null) {
                finalFileSize = localFile.length()
                mediaUrl = uploadLocalFileToStorage("chat_media/$chatId/$messageId.m4a", localFile)
            } else if (mediaFileUri != null) {
                val ext = when (type) {
                    Message.TYPE_IMAGE -> "jpg"
                    Message.TYPE_VIDEO -> "mp4"
                    Message.TYPE_AUDIO -> "m4a"
                    else -> "dat"
                }
                mediaUrl = uploadFileToStorage("chat_media/$chatId/$messageId.$ext", mediaFileUri)
            }

            val previewText = when (type) {
                Message.TYPE_IMAGE -> "📷 Photo"
                Message.TYPE_VIDEO -> "🎥 Video"
                Message.TYPE_AUDIO -> "🎤 Voice message"
                Message.TYPE_DOCUMENT -> "📄 Document"
                else -> text
            }

            val timestamp = System.currentTimeMillis()
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                senderName = sender.name,
                receiverId = receiverId,
                text = text,
                type = type,
                mediaUrl = mediaUrl,
                mediaDurationMs = durationMs,
                fileName = fileName,
                fileSize = finalFileSize,
                timestamp = timestamp,
                status = Message.STATUS_SENT,
                replyToMessageId = replyToMessage?.id,
                replyToText = replyToMessage?.let { "${it.senderName}: ${it.text.ifBlank { it.type }}" }
            )

            // Save message to /messages/{chatId}/{messageId}
            database.getReference("messages").child(chatId).child(messageId).setValue(message.toMap()).await()

            // Update user_chats for sender
            val senderSummary = ChatSummary(
                chatId = chatId,
                isGroup = false,
                title = receiverName,
                photoUrl = receiverPhoto,
                otherUserId = receiverId,
                lastMessage = previewText,
                lastMessageType = type,
                lastMessageSenderId = senderId,
                lastMessageTimestamp = timestamp,
                unreadCount = 0,
                lastMessageStatus = Message.STATUS_SENT
            )
            database.getReference("user_chats").child(senderId).child(chatId).setValue(senderSummary.toMap())

            // Update user_chats for receiver
            val receiverSummary = ChatSummary(
                chatId = chatId,
                isGroup = false,
                title = sender.name,
                photoUrl = sender.photoUrl,
                otherUserId = senderId,
                lastMessage = previewText,
                lastMessageType = type,
                lastMessageSenderId = senderId,
                lastMessageTimestamp = timestamp,
                unreadCount = 1,
                lastMessageStatus = Message.STATUS_DELIVERED
            )
            database.getReference("user_chats").child(receiverId).child(chatId).setValue(receiverSummary.toMap())

            Result.success(message)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Failed to send message", e)
            Result.failure(e)
        }
    }

    fun markMessagesAsRead(chatId: String, currentUid: String) {
        // Reset unread count for current user
        database.getReference("user_chats").child(currentUid).child(chatId).child("unreadCount").setValue(0)

        // Mark messages from other user as read
        val messagesRef = database.getReference("messages").child(chatId)
        messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null && msg.senderId != currentUid && msg.status != Message.STATUS_READ) {
                        child.ref.child("status").setValue(Message.STATUS_READ)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = currentUserId ?: return
        database.getReference("typing").child(chatId).child(uid).setValue(isTyping)
    }

    fun getTypingStatus(chatId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        val ref = database.getReference("typing").child(chatId).child(otherUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Group Chats
    suspend fun createGroup(
        name: String,
        description: String,
        iconUri: Uri?,
        memberIds: List<String>
    ): Result<String> {
        val myUid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            val groupId = database.getReference("groups").push().key ?: UUID.randomUUID().toString()
            var iconUrl: String? = null
            if (iconUri != null) {
                iconUrl = uploadFileToStorage("group_icons/$groupId.jpg", iconUri)
            }

            val allMembers = (memberIds + myUid).distinct()
            val groupInfo = GroupInfo(
                groupId = groupId,
                groupName = name,
                groupDescription = description,
                groupIconUrl = iconUrl,
                createdById = myUid,
                createdAt = System.currentTimeMillis(),
                adminIds = listOf(myUid),
                memberIds = allMembers
            )

            database.getReference("groups").child(groupId).setValue(groupInfo.toMap()).await()

            // Add to user_chats for each member
            val chatSummary = ChatSummary(
                chatId = groupId,
                isGroup = true,
                title = name,
                photoUrl = iconUrl,
                otherUserId = null,
                lastMessage = "Group created",
                lastMessageType = Message.TYPE_TEXT,
                lastMessageSenderId = myUid,
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                lastMessageStatus = Message.STATUS_READ
            )

            for (mId in allMembers) {
                database.getReference("user_chats").child(mId).child(groupId).setValue(chatSummary.toMap())
            }

            Result.success(groupId)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Create group error", e)
            Result.failure(e)
        }
    }

    fun getGroupInfo(groupId: String): Flow<GroupInfo?> = callbackFlow {
        val ref = database.getReference("groups").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(GroupInfo::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendGroupMessage(
        groupId: String,
        groupName: String,
        groupIcon: String?,
        text: String,
        type: String = Message.TYPE_TEXT,
        mediaFileUri: Uri? = null,
        localFile: File? = null,
        fileName: String? = null,
        durationMs: Long? = null
    ): Result<Message> {
        val sender = _currentUser.value ?: return Result.failure(IllegalStateException("Sender not loaded"))
        return try {
            val messageId = database.getReference("messages").child(groupId).push().key
                ?: UUID.randomUUID().toString()

            var mediaUrl: String? = null
            var finalFileSize: Long? = null

            if (localFile != null) {
                finalFileSize = localFile.length()
                mediaUrl = uploadLocalFileToStorage("group_media/$groupId/$messageId.m4a", localFile)
            } else if (mediaFileUri != null) {
                val ext = when (type) {
                    Message.TYPE_IMAGE -> "jpg"
                    Message.TYPE_VIDEO -> "mp4"
                    Message.TYPE_AUDIO -> "m4a"
                    else -> "dat"
                }
                mediaUrl = uploadFileToStorage("group_media/$groupId/$messageId.$ext", mediaFileUri)
            }

            val timestamp = System.currentTimeMillis()
            val message = Message(
                id = messageId,
                chatId = groupId,
                senderId = sender.uid,
                senderName = sender.name,
                receiverId = groupId,
                text = text,
                type = type,
                mediaUrl = mediaUrl,
                mediaDurationMs = durationMs,
                fileName = fileName,
                fileSize = finalFileSize,
                timestamp = timestamp,
                status = Message.STATUS_SENT
            )

            database.getReference("messages").child(groupId).child(messageId).setValue(message.toMap()).await()

            // Update user_chats for all group members
            val groupSnapshot = database.getReference("groups").child(groupId).get().await()
            val group = groupSnapshot.getValue(GroupInfo::class.java)
            val previewText = "${sender.name}: ${if (type == Message.TYPE_TEXT) text else type}"

            group?.memberIds?.forEach { mId ->
                val summary = ChatSummary(
                    chatId = groupId,
                    isGroup = true,
                    title = groupName,
                    photoUrl = groupIcon,
                    otherUserId = null,
                    lastMessage = previewText,
                    lastMessageType = type,
                    lastMessageSenderId = sender.uid,
                    lastMessageTimestamp = timestamp,
                    unreadCount = if (mId == sender.uid) 0 else 1,
                    lastMessageStatus = Message.STATUS_DELIVERED
                )
                database.getReference("user_chats").child(mId).child(groupId).setValue(summary.toMap())
            }

            Result.success(message)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Group message send error", e)
            Result.failure(e)
        }
    }

    // Status Updates (24-Hour Expiration)
    suspend fun postStatus(
        type: String,
        content: String,
        mediaUri: Uri? = null,
        backgroundColorHex: String = "#075E54",
        caption: String? = null
    ): Result<StatusItem> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("User not loaded"))
        return try {
            val statusId = database.getReference("statuses").child(user.uid).push().key
                ?: UUID.randomUUID().toString()

            var finalContent = content
            if (mediaUri != null) {
                val ext = if (type == StatusItem.TYPE_VIDEO) "mp4" else "jpg"
                finalContent = uploadFileToStorage("status_media/${user.uid}/$statusId.$ext", mediaUri)
            }

            val now = System.currentTimeMillis()
            val expiresAt = now + (24 * 60 * 60 * 1000L) // 24 hours

            val statusItem = StatusItem(
                id = statusId,
                userId = user.uid,
                userName = user.name,
                userPhotoUrl = user.photoUrl,
                type = type,
                content = finalContent,
                backgroundColorHex = backgroundColorHex,
                caption = caption,
                timestamp = now,
                expiresAt = expiresAt,
                viewerIds = emptyList()
            )

            database.getReference("statuses").child(user.uid).child(statusId).setValue(statusItem.toMap()).await()
            Result.success(statusItem)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Post status error", e)
            Result.failure(e)
        }
    }

    fun getAllActiveStatuses(): Flow<List<StatusItem>> = callbackFlow {
        val ref = database.getReference("statuses")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val list = mutableListOf<StatusItem>()
                for (userNode in snapshot.children) {
                    for (statusNode in userNode.children) {
                        val status = statusNode.getValue(StatusItem::class.java)
                        if (status != null && status.expiresAt > now) {
                            list.add(status)
                        } else if (status != null && status.expiresAt <= now) {
                            // Purge expired status
                            statusNode.ref.removeValue()
                        }
                    }
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun markStatusViewed(statusOwnerUid: String, statusId: String) {
        val myUid = currentUserId ?: return
        val viewersRef = database.getReference("statuses").child(statusOwnerUid).child(statusId).child("viewerIds")
        viewersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (child in snapshot.children) {
                    child.getValue(String::class.java)?.let { list.add(it) }
                }
                if (!list.contains(myUid)) {
                    list.add(myUid)
                    viewersRef.setValue(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Voice and Video Calls
    suspend fun startCall(
        receiverId: String,
        receiverName: String,
        receiverPhoto: String?,
        callType: String
    ): Result<CallRecord> {
        val caller = _currentUser.value ?: return Result.failure(IllegalStateException("Caller not loaded"))
        return try {
            val callId = database.getReference("calls").push().key ?: UUID.randomUUID().toString()
            val record = CallRecord(
                callId = callId,
                callerId = caller.uid,
                callerName = caller.name,
                callerPhotoUrl = caller.photoUrl,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverPhotoUrl = receiverPhoto,
                callType = callType,
                status = CallRecord.STATUS_RINGING,
                timestamp = System.currentTimeMillis(),
                durationSeconds = 0
            )

            // Save active call
            database.getReference("calls").child(callId).setValue(record.toMap()).await()

            // Save to call history of both
            database.getReference("user_calls").child(caller.uid).child(callId).setValue(record.toMap())
            database.getReference("user_calls").child(receiverId).child(callId).setValue(record.toMap())

            Result.success(record)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Start call error", e)
            Result.failure(e)
        }
    }

    fun listenForActiveCall(callId: String): Flow<CallRecord?> = callbackFlow {
        val ref = database.getReference("calls").child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(CallRecord::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun listenForIncomingCalls(): Flow<CallRecord?> = callbackFlow {
        val myUid = currentUserId
        if (myUid == null) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }

        val ref = database.getReference("calls")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found: CallRecord? = null
                val now = System.currentTimeMillis()
                for (child in snapshot.children) {
                    val call = child.getValue(CallRecord::class.java)
                    if (call != null &&
                        call.receiverId == myUid &&
                        call.status == CallRecord.STATUS_RINGING &&
                        (now - call.timestamp) < 60_000 // Ringing within 60s
                    ) {
                        found = call
                        break
                    }
                }
                trySend(found)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun answerCall(callId: String) {
        database.getReference("calls").child(callId).child("status").setValue(CallRecord.STATUS_ACCEPTED)
    }

    fun rejectOrEndCall(callId: String, status: String = CallRecord.STATUS_ENDED, durationSec: Int = 0) {
        val ref = database.getReference("calls").child(callId)
        ref.child("status").setValue(status)
        ref.child("durationSeconds").setValue(durationSec)

        // Also update both call histories
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val call = snapshot.getValue(CallRecord::class.java) ?: return
                database.getReference("user_calls").child(call.callerId).child(callId).child("status").setValue(status)
                database.getReference("user_calls").child(call.callerId).child(callId).child("durationSeconds").setValue(durationSec)
                database.getReference("user_calls").child(call.receiverId).child(callId).child("status").setValue(status)
                database.getReference("user_calls").child(call.receiverId).child(callId).child("durationSeconds").setValue(durationSec)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun getCallHistory(): Flow<List<CallRecord>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val ref = database.getReference("user_calls").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<CallRecord>()
                for (child in snapshot.children) {
                    val record = child.getValue(CallRecord::class.java)
                    if (record != null) {
                        list.add(record)
                    }
                }
                list.sortByDescending { it.timestamp }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // WebRTC Signaling methods
    fun sendSignalingOffer(callId: String, sdp: String) {
        database.getReference("calls").child(callId).child("offer").setValue(sdp)
    }

    fun sendSignalingAnswer(callId: String, sdp: String) {
        database.getReference("calls").child(callId).child("answer").setValue(sdp)
    }

    fun addIceCandidate(callId: String, isCaller: Boolean, candidate: String) {
        val path = if (isCaller) "callerCandidates" else "receiverCandidates"
        database.getReference("calls").child(callId).child(path).push().setValue(candidate)
    }

    // Helpers
    private suspend fun uploadFileToStorage(path: String, uri: Uri): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private suspend fun uploadLocalFileToStorage(path: String, file: File): String {
        val ref = storage.reference.child(path)
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    private fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
}

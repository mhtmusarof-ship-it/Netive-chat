package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.NeticeApplication
import com.example.model.CallRecord
import com.example.model.Message
import com.example.ui.home.formatChatTimestamp
import com.example.ui.home.formatStatusTime
import com.example.ui.theme.WhatsAppBlueReceipt
import com.example.ui.theme.WhatsAppDarkBackground
import com.example.ui.theme.WhatsAppDarkIncomingBubble
import com.example.ui.theme.WhatsAppDarkOutgoingBubble
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppGrayReceipt
import com.example.ui.theme.WhatsAppLightChatBackground
import com.example.ui.theme.WhatsAppLightIncomingBubble
import com.example.ui.theme.WhatsAppLightOutgoingBubble
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    otherUserId: String,
    otherUserName: String,
    isGroup: Boolean,
    photoUrl: String?,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (groupId: String) -> Unit,
    onStartCall: (callId: String, otherUserId: String, otherUserName: String, callType: String, photoUrl: String?) -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val audioManager = remember { NeticeApplication.instance.audioManager }
    val currentUser by repository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    val messages by repository.getChatMessages(chatId).collectAsState(initial = emptyList())
    val otherUser by repository.getUser(otherUserId).collectAsState(initial = null)
    val isOtherTyping by repository.getTypingStatus(chatId, otherUserId).collectAsState(initial = false)

    val isRecording by audioManager.isRecording.collectAsState()
    val playingMessageId by audioManager.playingMessageId.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSendingMedia by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Mark messages as read and clear unread count
    LaunchedEffect(chatId) {
        currentUser?.uid?.let { uid ->
            repository.markMessagesAsRead(chatId, uid)
        }
    }

    // Cleanup audio on dispose
    DisposableEffect(Unit) {
        onDispose {
            audioManager.stopPlayback()
            audioManager.cancelRecording()
            repository.setTyping(chatId, false)
        }
    }

    // Media pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isSendingMedia = true
            scope.launch {
                if (isGroup) {
                    repository.sendGroupMessage(
                        groupId = chatId,
                        groupName = otherUserName,
                        groupIcon = photoUrl,
                        text = "",
                        type = Message.TYPE_IMAGE,
                        mediaFileUri = uri
                    )
                } else {
                    repository.sendMessage(
                        receiverId = otherUserId,
                        receiverName = otherUserName,
                        receiverPhoto = photoUrl,
                        text = "",
                        type = Message.TYPE_IMAGE,
                        mediaFileUri = uri
                    )
                }
                isSendingMedia = false
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isSendingMedia = true
            scope.launch {
                if (isGroup) {
                    repository.sendGroupMessage(
                        groupId = chatId,
                        groupName = otherUserName,
                        groupIcon = photoUrl,
                        text = "",
                        type = Message.TYPE_VIDEO,
                        mediaFileUri = uri
                    )
                } else {
                    repository.sendMessage(
                        receiverId = otherUserId,
                        receiverName = otherUserName,
                        receiverPhoto = photoUrl,
                        text = "",
                        type = Message.TYPE_VIDEO,
                        mediaFileUri = uri
                    )
                }
                isSendingMedia = false
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isSendingMedia = true
            scope.launch {
                if (isGroup) {
                    repository.sendGroupMessage(
                        groupId = chatId,
                        groupName = otherUserName,
                        groupIcon = photoUrl,
                        text = "Document",
                        type = Message.TYPE_DOCUMENT,
                        mediaFileUri = uri,
                        fileName = uri.lastPathSegment ?: "document"
                    )
                } else {
                    repository.sendMessage(
                        receiverId = otherUserId,
                        receiverName = otherUserName,
                        receiverPhoto = photoUrl,
                        text = "Document",
                        type = Message.TYPE_DOCUMENT,
                        mediaFileUri = uri,
                        fileName = uri.lastPathSegment ?: "document"
                    )
                }
                isSendingMedia = false
            }
        }
    }

    fun sendTextMessage() {
        if (textInput.trim().isBlank()) return
        val textToSend = textInput.trim()
        val reply = replyingToMessage
        textInput = ""
        replyingToMessage = null
        repository.setTyping(chatId, false)

        scope.launch {
            if (isGroup) {
                repository.sendGroupMessage(
                    groupId = chatId,
                    groupName = otherUserName,
                    groupIcon = photoUrl,
                    text = textToSend,
                    type = Message.TYPE_TEXT
                )
            } else {
                repository.sendMessage(
                    receiverId = otherUserId,
                    receiverName = otherUserName,
                    receiverPhoto = photoUrl,
                    text = textToSend,
                    type = Message.TYPE_TEXT,
                    replyToMessage = reply
                )
            }
        }
    }

    fun finishVoiceRecording() {
        val (audioFile, duration) = audioManager.stopRecording()
        if (audioFile != null && duration > 800) {
            isSendingMedia = true
            scope.launch {
                if (isGroup) {
                    repository.sendGroupMessage(
                        groupId = chatId,
                        groupName = otherUserName,
                        groupIcon = photoUrl,
                        text = "",
                        type = Message.TYPE_AUDIO,
                        localFile = audioFile,
                        durationMs = duration
                    )
                } else {
                    repository.sendMessage(
                        receiverId = otherUserId,
                        receiverName = otherUserName,
                        receiverPhoto = photoUrl,
                        text = "",
                        type = Message.TYPE_AUDIO,
                        localFile = audioFile,
                        durationMs = duration
                    )
                }
                isSendingMedia = false
            }
        } else {
            audioFile?.delete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (isGroup) onNavigateToGroupInfo(chatId)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayPhoto = photoUrl ?: otherUser?.photoUrl
                            if (!displayPhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = displayPhoto,
                                    contentDescription = otherUserName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = if (isGroup) Icons.Default.Group else Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = otherUserName.ifBlank { "Chat" },
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Subtitle: Typing... / Online / Last Seen
                            val subtitle = when {
                                isOtherTyping -> "typing..."
                                isGroup -> "Tap for group info"
                                otherUser?.online == true -> "online"
                                (otherUser?.lastSeen ?: 0L) > 0L -> "last seen ${formatStatusTime(otherUser!!.lastSeen)}"
                                else -> ""
                            }
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = if (isOtherTyping) WhatsAppTeal else Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isOtherTyping) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (!isGroup) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val result = repository.startCall(otherUserId, otherUserName, photoUrl, CallRecord.TYPE_VIDEO)
                                    result.getOrNull()?.let { call ->
                                        onStartCall(call.callId, otherUserId, otherUserName, CallRecord.TYPE_VIDEO, photoUrl)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("chat_action_video_call")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val result = repository.startCall(otherUserId, otherUserName, photoUrl, CallRecord.TYPE_AUDIO)
                                    result.getOrNull()?.let { call ->
                                        onStartCall(call.callId, otherUserId, otherUserName, CallRecord.TYPE_AUDIO, photoUrl)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("chat_action_voice_call")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Group info") },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToGroupInfo(chatId)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Search messages") },
                                onClick = { menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear chat") },
                                onClick = { menuExpanded = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhatsAppDarkTeal)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(WhatsAppDarkBackground)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMyMsg = msg.senderId == currentUser?.uid
                    MessageBubble(
                        message = msg,
                        isMyMessage = isMyMsg,
                        isGroup = isGroup,
                        isPlaying = playingMessageId == msg.id,
                        onPlayAudio = {
                            msg.mediaUrl?.let { url ->
                                audioManager.playAudio(msg.id, url)
                            }
                        },
                        onReplyClick = {
                            replyingToMessage = msg
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (isSendingMedia) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = WhatsAppTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading media...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Reply Preview Banner
            AnimatedVisibility(visible = replyingToMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(36.dp)
                                .background(WhatsAppTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = replyingToMessage?.senderName ?: "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = WhatsAppTeal
                            )
                            Text(
                                text = replyingToMessage?.text?.ifBlank { replyingToMessage?.type } ?: "",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecording) {
                    // Recording Indicator
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Recording",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Recording voice note...", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Cancel Recording button
                    IconButton(
                        onClick = { audioManager.cancelRecording() },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                    }

                    // Send Recording FAB
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(WhatsAppTeal)
                            .clickable { finishVoiceRecording() }
                            .testTag("send_voice_note_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Send voice note", tint = Color.White)
                    }
                } else {
                    // Normal Input Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = {
                                    textInput = it
                                    repository.setTyping(chatId, it.isNotBlank())
                                },
                                placeholder = { Text("Message") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                maxLines = 4
                            )

                            // Attachment Clip button
                            IconButton(
                                onClick = { showAttachmentSheet = true },
                                modifier = Modifier.testTag("chat_attachment_button")
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = "Attach",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Camera button
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("chat_camera_button")
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send or Mic Button
                    if (textInput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WhatsAppTeal)
                                .clickable { sendTextMessage() }
                                .testTag("chat_send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WhatsAppTeal)
                                .clickable {
                                    audioManager.startRecording()
                                }
                                .testTag("chat_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Record voice note",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Attachment Sheet
        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("Share Content", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttachmentOption(
                            icon = Icons.Default.CameraAlt,
                            title = "Photo",
                            backgroundColor = Color(0xFFD3396D)
                        ) {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        AttachmentOption(
                            icon = Icons.Default.Videocam,
                            title = "Video",
                            backgroundColor = Color(0xFF8B5CF6)
                        ) {
                            showAttachmentSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        AttachmentOption(
                            icon = Icons.Default.Description,
                            title = "Document",
                            backgroundColor = Color(0xFF5F66CD)
                        ) {
                            showAttachmentSheet = false
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp)
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMyMessage: Boolean,
    isGroup: Boolean,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onReplyClick: () -> Unit
) {
    val bubbleColor = if (isMyMessage) WhatsAppDarkOutgoingBubble else WhatsAppDarkIncomingBubble
    val bubbleShape = if (isMyMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            modifier = Modifier
                .widthIn(min = 80.dp, max = 300.dp)
                .clickable { onReplyClick() }
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Sender name in group
                if (isGroup && !isMyMessage) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = WhatsAppTeal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Reply to preview inside bubble
                if (!message.replyToText.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = message.replyToText,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(6.dp),
                            maxLines = 2
                        )
                    }
                }

                // Content based on type
                when (message.type) {
                    Message.TYPE_IMAGE -> {
                        if (!message.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Image attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(message.text, fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                    Message.TYPE_VIDEO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Message.TYPE_AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppTeal)
                                    .clickable { onPlayAudio() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Voice message", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                val durationText = message.mediaDurationMs?.let { "${it / 1000}s" } ?: "Audio"
                                Text(durationText, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                    Message.TYPE_DOCUMENT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.fileName ?: "Document",
                                fontSize = 13.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.text,
                            fontSize = 15.sp,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Time and delivery ticks
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val timeStr = if (message.timestamp > 0) timeFormat.format(Date(message.timestamp)) else ""
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    if (isMyMessage) {
                        Spacer(modifier = Modifier.width(3.dp))
                        when (message.status) {
                            Message.STATUS_READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    tint = WhatsAppBlueReceipt,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Message.STATUS_DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = WhatsAppGrayReceipt,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = WhatsAppGrayReceipt,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

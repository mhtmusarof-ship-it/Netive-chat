package com.example.ui.status

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.NeticeApplication
import com.example.model.Message
import com.example.model.StatusItem
import com.example.ui.home.formatStatusTime
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StatusViewerScreen(
    targetUserId: String,
    onNavigateBack: () -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val allStatuses by repository.getAllActiveStatuses().collectAsState(initial = emptyList())

    val userStatuses = remember(allStatuses, targetUserId) {
        allStatuses.filter { it.userId == targetUserId }.sortedBy { it.timestamp }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var replyText by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }

    if (userStatuses.isEmpty()) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    val currentStatus = userStatuses.getOrNull(currentIndex) ?: userStatuses.first()
    val isOwner = currentStatus.userId == currentUser?.uid

    // Mark viewed
    LaunchedEffect(currentStatus.id) {
        repository.markStatusViewed(currentStatus.userId, currentStatus.id)
    }

    // Auto advance timer
    val progressAnim = remember(currentIndex) { Animatable(0f) }
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ((1f - progressAnim.value) * 5000).toInt(),
                    easing = LinearEasing
                )
            )
            if (currentIndex < userStatuses.size - 1) {
                currentIndex++
            } else {
                onNavigateBack()
            }
        }
    }

    val bgColor = remember(currentStatus.backgroundColorHex) {
        try {
            Color(android.graphics.Color.parseColor(currentStatus.backgroundColorHex))
        } catch (e: Exception) {
            Color(0xFF075E54)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (currentStatus.type == StatusItem.TYPE_TEXT) bgColor else Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            if (currentIndex > 0) currentIndex--
                        } else {
                            if (currentIndex < userStatuses.size - 1) currentIndex++ else onNavigateBack()
                        }
                    }
                )
            }
    ) {
        // Status Content
        if (currentStatus.type == StatusItem.TYPE_TEXT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentStatus.content,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            AsyncImage(
                model = currentStatus.content,
                contentDescription = "Status media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (!currentStatus.caption.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = currentStatus.caption,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Top Overlay: Progress Bars + User Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            // Segmented Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                userStatuses.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progressAnim.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentStatus.userPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentStatus.userPhotoUrl,
                            contentDescription = currentStatus.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = currentStatus.userName.ifBlank { "User" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatStatusTime(currentStatus.timestamp),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Bottom Bar: Viewer count or Reply
        if (isOwner) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${currentStatus.viewerIds.size} views",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        } else {
            // Reply Input
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply...", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("status_reply_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                if (replyText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WhatsAppTeal)
                            .clickable {
                                val replyToSend = replyText.trim()
                                replyText = ""
                                scope.launch {
                                    repository.sendMessage(
                                        receiverId = currentStatus.userId,
                                        receiverName = currentStatus.userName,
                                        receiverPhoto = currentStatus.userPhotoUrl,
                                        text = "Replied to status: \"${currentStatus.content.take(20)}\"\n$replyToSend"
                                    )
                                }
                            }
                            .testTag("send_status_reply_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

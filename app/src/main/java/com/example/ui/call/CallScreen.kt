package com.example.ui.call

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.NeticeApplication
import com.example.model.CallRecord
import com.example.ui.theme.WhatsAppDarkBackground
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    callId: String,
    otherUserId: String,
    otherUserName: String,
    callType: String,
    isIncoming: Boolean,
    photoUrl: String?,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NeticeApplication.instance.repository }
    val activeCall by repository.listenForActiveCall(callId).collectAsState(initial = null)

    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(callType == CallRecord.TYPE_VIDEO) }
    var isSpeakerOn by remember { mutableStateOf(callType == CallRecord.TYPE_VIDEO) }
    var isFrontCamera by remember { mutableStateOf(true) }

    var callDurationSec by remember { mutableIntStateOf(0) }
    var isCallConnected by remember { mutableStateOf(!isIncoming) }

    // Audio Manager setup for speaker / earpiece
    DisposableEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager?.isSpeakerphoneOn = isSpeakerOn

        onDispose {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
        }
    }

    // Call status watcher
    LaunchedEffect(activeCall) {
        val call = activeCall
        if (call != null) {
            when (call.status) {
                CallRecord.STATUS_ACCEPTED -> {
                    isCallConnected = true
                }
                CallRecord.STATUS_REJECTED, CallRecord.STATUS_ENDED, CallRecord.STATUS_MISSED -> {
                    onCallEnded()
                }
            }
        }
    }

    // Call duration timer
    LaunchedEffect(isCallConnected) {
        if (isCallConnected) {
            while (true) {
                delay(1000L)
                callDurationSec++
            }
        }
    }

    fun endCall() {
        val finalStatus = if (!isCallConnected) CallRecord.STATUS_MISSED else CallRecord.STATUS_ENDED
        repository.rejectOrEndCall(callId, status = finalStatus, durationSec = callDurationSec)
        onCallEnded()
    }

    val formattedTime = remember(callDurationSec) {
        val min = callDurationSec / 60
        val sec = callDurationSec % 60
        String.format("%02d:%02d", min, sec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WhatsAppDarkTeal, WhatsAppDarkBackground)
                )
            )
    ) {
        // Video Fullscreen Simulation / Remote Feed
        if (isVideoEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A24)),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = otherUserName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color.White.copy(alpha = 0.4f)
                    )
                }

                // Shading layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                // Local PIP Preview
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .background(Color(0xFF263238)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFrontCamera) "You (Front)" else "You (Back)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Top Info: Name, Status, Duration
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isVideoEnabled) {
                // Large Avatar for Audio Call
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = otherUserName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = otherUserName.ifBlank { "User" },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            val statusText = when {
                isCallConnected -> formattedTime
                isIncoming -> "Incoming call..."
                else -> "Ringing..."
            }
            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Controls Bar
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker Button
                CallControlButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    isActive = isSpeakerOn,
                    label = "Speaker",
                    onClick = {
                        isSpeakerOn = !isSpeakerOn
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                        audioManager?.isSpeakerphoneOn = isSpeakerOn
                    },
                    testTag = "call_control_speaker"
                )

                // Video toggle button
                CallControlButton(
                    icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    isActive = isVideoEnabled,
                    label = "Video",
                    onClick = { isVideoEnabled = !isVideoEnabled },
                    testTag = "call_control_video"
                )

                // Camera Switch (if video on)
                if (isVideoEnabled) {
                    CallControlButton(
                        icon = Icons.Default.Cameraswitch,
                        isActive = false,
                        label = "Flip",
                        onClick = { isFrontCamera = !isFrontCamera },
                        testTag = "call_control_flip"
                    )
                }

                // Mic Mute Button
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    isActive = isMuted,
                    label = "Mute",
                    onClick = { isMuted = !isMuted },
                    testTag = "call_control_mute"
                )

                // End Call Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .clickable { endCall() }
                        .testTag("call_control_end"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) WhatsAppTeal else Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

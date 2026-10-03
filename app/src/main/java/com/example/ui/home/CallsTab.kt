package com.example.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CallRecord
import com.example.ui.theme.WhatsAppCallIncoming
import com.example.ui.theme.WhatsAppCallMissed
import com.example.ui.theme.WhatsAppTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsTab(
    callHistory: List<CallRecord>,
    currentUserId: String?,
    onCallAgainClick: (receiverId: String, receiverName: String, receiverPhoto: String?, callType: String) -> Unit
) {
    if (callHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(WhatsAppTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = WhatsAppTeal
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No recent calls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To start a voice or video call with friends, tap the call button at the bottom.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Recent",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(callHistory, key = { it.callId }) { record ->
                val isOutgoing = record.callerId == currentUserId
                val otherName = if (isOutgoing) record.receiverName else record.callerName
                val otherPhoto = if (isOutgoing) record.receiverPhotoUrl else record.callerPhotoUrl
                val otherId = if (isOutgoing) record.receiverId else record.callerId

                CallItemRow(
                    record = record,
                    isOutgoing = isOutgoing,
                    displayName = otherName,
                    photoUrl = otherPhoto,
                    onCallClick = {
                        onCallAgainClick(otherId, otherName, otherPhoto, record.callType)
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun CallItemRow(
    record: CallRecord,
    isOutgoing: Boolean,
    displayName: String,
    photoUrl: String?,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCallClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("call_item_${record.callId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName.ifBlank { "User" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (record.status == CallRecord.STATUS_MISSED && !isOutgoing) WhatsAppCallMissed else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Call status icon
                if (record.status == CallRecord.STATUS_MISSED && !isOutgoing) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                        contentDescription = "Missed call",
                        tint = WhatsAppCallMissed,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (isOutgoing) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = "Outgoing call",
                        tint = WhatsAppCallIncoming,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                        contentDescription = "Incoming call",
                        tint = WhatsAppCallIncoming,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                val timeStr = if (record.timestamp > 0) dateFormat.format(Date(record.timestamp)) else ""
                Text(
                    text = timeStr,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (record.durationSeconds > 0) {
                    Text(
                        text = " (${record.durationSeconds / 60}m ${record.durationSeconds % 60}s)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action icon (Audio call or Video call icon)
        IconButton(
            onClick = onCallClick,
            modifier = Modifier.testTag("action_call_${record.callId}")
        ) {
            Icon(
                imageVector = if (record.callType == CallRecord.TYPE_VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Call",
                tint = WhatsAppTeal,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

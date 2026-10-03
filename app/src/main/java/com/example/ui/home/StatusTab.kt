package com.example.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.model.StatusItem
import com.example.model.User
import com.example.ui.theme.WhatsAppTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusTab(
    currentUser: User?,
    statuses: List<StatusItem>,
    onMyStatusClick: () -> Unit,
    onStatusClick: (StatusItem) -> Unit
) {
    val myStatuses = statuses.filter { it.userId == currentUser?.uid }
    val otherStatuses = statuses.filter { it.userId != currentUser?.uid }

    // Group other statuses by user
    val groupedOthers = otherStatuses.groupBy { it.userId }
    val recentList = mutableListOf<StatusItem>()
    val viewedList = mutableListOf<StatusItem>()

    groupedOthers.forEach { (_, userStatuses) ->
        val latest = userStatuses.maxByOrNull { it.timestamp } ?: return@forEach
        val hasUnviewed = userStatuses.any { currentUser?.uid !in it.viewerIds }
        if (hasUnviewed) {
            recentList.add(latest)
        } else {
            viewedList.add(latest)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // My Status Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMyStatusClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("my_status_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp)
                ) {
                    val hasMyActiveStatus = myStatuses.isNotEmpty()
                    val avatarModifier = if (hasMyActiveStatus) {
                        Modifier
                            .fillMaxSize()
                            .border(2.5.dp, WhatsAppTeal, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    }

                    Box(
                        modifier = avatarModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentUser?.photoUrl,
                                contentDescription = "My profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Plus icon if no status
                    if (!hasMyActiveStatus) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WhatsAppTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add status",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "My status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (myStatuses.isNotEmpty()) {
                        val latestMy = myStatuses.maxByOrNull { it.timestamp }
                        val viewersCount = latestMy?.viewerIds?.size ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$viewersCount views • ${formatStatusTime(latestMy?.timestamp ?: 0L)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Tap to add status update",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }

        // Recent updates header
        if (recentList.isNotEmpty()) {
            item {
                Text(
                    text = "Recent updates",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(recentList, key = { it.id }) { status ->
                StatusItemRow(
                    status = status,
                    isViewed = false,
                    onClick = { onStatusClick(status) }
                )
            }
        }

        // Viewed updates header
        if (viewedList.isNotEmpty()) {
            item {
                Text(
                    text = "Viewed updates",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(viewedList, key = { it.id }) { status ->
                StatusItemRow(
                    status = status,
                    isViewed = true,
                    onClick = { onStatusClick(status) }
                )
            }
        }

        if (recentList.isEmpty() && viewedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contact status updates available yet. Tap the pencil or camera below to post your first status!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusItemRow(
    status: StatusItem,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("status_row_${status.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ringColor = if (isViewed) MaterialTheme.colorScheme.outlineVariant else WhatsAppTeal
        Box(
            modifier = Modifier
                .size(54.dp)
                .border(2.5.dp, ringColor, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!status.userPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = status.userPhotoUrl,
                    contentDescription = status.userName,
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

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = status.userName.ifBlank { "User" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatStatusTime(status.timestamp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatStatusTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val formatTime = SimpleDateFormat("h:mm a", Locale.getDefault())
    val hours = diff / (60 * 60 * 1000)

    return when {
        hours < 1 -> "Just now"
        hours < 24 -> formatTime.format(Date(timestamp))
        else -> "Yesterday"
    }
}

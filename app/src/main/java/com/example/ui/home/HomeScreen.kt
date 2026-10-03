package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NeticeApplication
import com.example.model.CallRecord
import com.example.model.ChatSummary
import com.example.model.StatusItem
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (chatId: String, otherUserId: String, otherUserName: String, isGroup: Boolean, photoUrl: String?) -> Unit,
    onNavigateToUserSearch: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToCreateStatus: () -> Unit,
    onNavigateToStatusViewer: (userId: String) -> Unit,
    onNavigateToCall: (callId: String, otherUserId: String, otherUserName: String, callType: String, isIncoming: Boolean, photoUrl: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val currentUser by repository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    val chats by repository.getUserChats().collectAsState(initial = emptyList())
    val statuses by repository.getAllActiveStatuses().collectAsState(initial = emptyList())
    val callHistory by repository.getCallHistory().collectAsState(initial = emptyList())
    val incomingCall by repository.listenForIncomingCalls().collectAsState(initial = null)

    val pagerState = rememberPagerState(pageCount = { 3 })
    var menuExpanded by remember { mutableStateOf(false) }

    // Incoming Call Dialog
    incomingCall?.let { call ->
        AlertDialog(
            onDismissRequest = { /* Must respond */ },
            title = {
                Text(
                    text = "Incoming ${if (call.callType == CallRecord.TYPE_VIDEO) "Video" else "Voice"} Call",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${call.callerName.ifBlank { "Someone" }} is calling you...",
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.answerCall(call.callId)
                        onNavigateToCall(
                            call.callId,
                            call.callerId,
                            call.callerName,
                            call.callType,
                            true,
                            call.callerPhotoUrl
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Accept")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        repository.rejectOrEndCall(call.callId, status = CallRecord.STATUS_REJECTED)
                    }
                ) {
                    Text("Decline", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Netice Chat",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { onNavigateToCreateStatus() },
                            modifier = Modifier.testTag("action_camera")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
                        }
                        IconButton(
                            onClick = { onNavigateToUserSearch() },
                            modifier = Modifier.testTag("action_search")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("action_more_menu")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New group") },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToCreateGroup()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Profile") },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToProfile()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToSettings()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WhatsAppDarkTeal)
                )

                // Tabs: Chats, Status, Calls
                val tabTitles = listOf("Chats", "Status", "Calls")
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = WhatsAppDarkTeal,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Color.White,
                            height = 3.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                    if (index == 0) {
                                        val totalUnread = chats.sumOf { it.unreadCount }
                                        if (totalUnread > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (totalUnread > 99) "99+" else totalUnread.toString(),
                                                    color = WhatsAppDarkTeal,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            when (pagerState.currentPage) {
                0 -> {
                    // Chats FAB
                    FloatingActionButton(
                        onClick = { onNavigateToUserSearch() },
                        containerColor = WhatsAppTeal,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_new_chat")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "New chat")
                    }
                }
                1 -> {
                    // Status FABs (Edit text status + Camera status)
                    Column(horizontalAlignment = Alignment.End) {
                        SmallFloatingActionButton(
                            onClick = { onNavigateToCreateStatus() },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_text_status")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Text status", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        FloatingActionButton(
                            onClick = { onNavigateToCreateStatus() },
                            containerColor = WhatsAppTeal,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_camera_status")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera status")
                        }
                    }
                }
                2 -> {
                    // Calls FAB
                    FloatingActionButton(
                        onClick = { onNavigateToUserSearch() },
                        containerColor = WhatsAppTeal,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_new_call")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "New call")
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> ChatsTab(
                    chats = chats,
                    currentUserId = currentUser?.uid,
                    onChatClick = { chat ->
                        onNavigateToChat(
                            chat.chatId,
                            chat.otherUserId ?: chat.chatId,
                            chat.title,
                            chat.isGroup,
                            chat.photoUrl
                        )
                    },
                    onStartNewChatClick = { onNavigateToUserSearch() }
                )
                1 -> StatusTab(
                    currentUser = currentUser,
                    statuses = statuses,
                    onMyStatusClick = {
                        val myStatuses = statuses.filter { it.userId == currentUser?.uid }
                        if (myStatuses.isNotEmpty()) {
                            currentUser?.uid?.let { onNavigateToStatusViewer(it) }
                        } else {
                            onNavigateToCreateStatus()
                        }
                    },
                    onStatusClick = { status ->
                        onNavigateToStatusViewer(status.userId)
                    }
                )
                2 -> CallsTab(
                    callHistory = callHistory,
                    currentUserId = currentUser?.uid,
                    onCallAgainClick = { receiverId, receiverName, receiverPhoto, callType ->
                        scope.launch {
                            val result = repository.startCall(receiverId, receiverName, receiverPhoto, callType)
                            result.getOrNull()?.let { call ->
                                onNavigateToCall(
                                    call.callId,
                                    receiverId,
                                    receiverName,
                                    callType,
                                    false,
                                    receiverPhoto
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

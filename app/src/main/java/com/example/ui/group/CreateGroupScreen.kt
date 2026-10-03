package com.example.ui.group

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.NeticeApplication
import com.example.model.User
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (groupId: String, groupName: String) -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val scope = rememberCoroutineScope()
    val allUsers by repository.getAllUsers().collectAsState(initial = emptyList())

    val selectedUserIds = remember { mutableStateListOf<String>() }
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupIconUri by remember { mutableStateOf<Uri?>(null) }
    var isStepTwo by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            groupIconUri = uri
        }
    }

    fun submitCreateGroup() {
        if (groupName.trim().isBlank()) {
            errorMessage = "Please enter group subject"
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = repository.createGroup(
                name = groupName.trim(),
                description = groupDescription.trim(),
                iconUri = groupIconUri,
                memberIds = selectedUserIds.toList()
            )
            isLoading = false
            if (result.isSuccess) {
                val gId = result.getOrNull() ?: ""
                onGroupCreated(gId, groupName.trim())
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create group"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isStepTwo) "New group" else "Add participants", fontWeight = FontWeight.Bold, color = Color.White)
                        if (!isStepTwo) {
                            Text("${selectedUserIds.size} selected", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isStepTwo) isStepTwo = false else onNavigateBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhatsAppDarkTeal)
            )
        },
        floatingActionButton = {
            if (!isStepTwo) {
                if (selectedUserIds.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { isStepTwo = true },
                        containerColor = WhatsAppTeal,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.testTag("group_next_step_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = { submitCreateGroup() },
                    containerColor = WhatsAppTeal,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("group_finish_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Create Group")
                    }
                }
            }
        }
    ) { padding ->
        if (!isStepTwo) {
            // Step 1: Member Selection
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Selected members chips horizontal row
                if (selectedUserIds.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(selectedUserIds) { uId ->
                            val user = allUsers.find { it.uid == uId }
                            Surface(
                                color = WhatsAppTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(user?.name ?: "User", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { selectedUserIds.remove(uId) }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(allUsers, key = { it.uid }) { user ->
                        val isSelected = selectedUserIds.contains(user.uid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedUserIds.remove(user.uid) else selectedUserIds.add(user.uid)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("select_member_${user.uid}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = user.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name.ifBlank { "User" }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(user.about.ifBlank { user.email.ifBlank { "Netice Chat User" } }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedUserIds.add(user.uid) else selectedUserIds.remove(user.uid)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = WhatsAppTeal)
                            )
                        }
                    }
                }
            }
        } else {
            // Step 2: Subject & Icon
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Group Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("group_icon_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    if (groupIconUri != null) {
                        AsyncImage(
                            model = groupIconUri,
                            contentDescription = "Group Icon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Pick icon",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { if (it.length <= 40) groupName = it },
                    label = { Text("Group Subject / Name") },
                    placeholder = { Text("e.g. Friends & Family") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { if (it.length <= 150) groupDescription = it },
                    label = { Text("Group Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_desc_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Providing a group subject and optional group icon makes it easy for members to recognize.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

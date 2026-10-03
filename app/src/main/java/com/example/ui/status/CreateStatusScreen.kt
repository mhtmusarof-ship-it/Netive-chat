package com.example.ui.status

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.NeticeApplication
import com.example.model.StatusItem
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStatusScreen(
    onNavigateBack: () -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isMediaStatus by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val backgroundColors = listOf(
        "#075E54",
        "#128C7E",
        "#8B5CF6",
        "#E91E63",
        "#D32F2F",
        "#3F51B5",
        "#009688",
        "#FF5722",
        "#795548"
    )
    var colorIndex by remember { mutableIntStateOf(0) }
    val currentBgColorHex = backgroundColors[colorIndex]
    val currentBgColor = remember(currentBgColorHex) {
        try {
            Color(android.graphics.Color.parseColor(currentBgColorHex))
        } catch (e: Exception) {
            Color(0xFF075E54)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            isMediaStatus = true
        }
    }

    fun postStatus() {
        if (!isMediaStatus && statusText.trim().isBlank()) return
        isLoading = true

        scope.launch {
            if (isMediaStatus) {
                repository.postStatus(
                    type = StatusItem.TYPE_IMAGE,
                    content = "",
                    mediaUri = selectedMediaUri,
                    caption = caption.trim()
                )
            } else {
                repository.postStatus(
                    type = StatusItem.TYPE_TEXT,
                    content = statusText.trim(),
                    backgroundColorHex = currentBgColorHex
                )
            }
            isLoading = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isMediaStatus) "Media status" else "Type a status", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (!isMediaStatus) {
                        // Color change button
                        IconButton(
                            onClick = { colorIndex = (colorIndex + 1) % backgroundColors.size },
                            modifier = Modifier.testTag("status_color_toggle")
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Change color", tint = Color.White)
                        }
                    }
                    // Photo pick action
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("status_pick_photo")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Choose photo", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isMediaStatus) Color.Black else currentBgColor)
            )
        },
        floatingActionButton = {
            val canSend = (isMediaStatus && selectedMediaUri != null) || (!isMediaStatus && statusText.isNotBlank())
            if (canSend) {
                FloatingActionButton(
                    onClick = { postStatus() },
                    containerColor = WhatsAppTeal,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("post_status_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post Status")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isMediaStatus) Color.Black else currentBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isMediaStatus && selectedMediaUri != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = selectedMediaUri,
                            contentDescription = "Selected media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = {
                                selectedMediaUri = null
                                isMediaStatus = false
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove media", tint = Color.White)
                        }
                    }

                    // Caption input
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("status_caption_input"),
                        placeholder = { Text("Add a caption...", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = WhatsAppTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            } else {
                // Text status editor
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { if (it.length <= 250) statusText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_text_input"),
                        placeholder = {
                            Text(
                                "Type a status...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 28.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

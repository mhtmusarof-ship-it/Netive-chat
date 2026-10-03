package com.example.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val currentUser by repository.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var about by remember(currentUser) { mutableStateOf(currentUser?.about ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingAbout by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isLoading = true
            scope.launch {
                repository.saveUserProfile(
                    name = name,
                    about = about,
                    photoUri = uri,
                    phoneNumber = currentUser?.phoneNumber
                )
                isLoading = false
            }
        }
    }

    fun saveDetails() {
        isLoading = true
        scope.launch {
            repository.saveUserProfile(
                name = name,
                about = about,
                photoUri = selectedImageUri,
                phoneNumber = currentUser?.phoneNumber
            )
            isLoading = false
            isEditingName = false
            isEditingAbout = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("user_profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                val photo = selectedImageUri ?: currentUser?.photoUrl
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhatsAppTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = WhatsAppTeal)
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Name item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isEditingName) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_field"),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { saveDetails() }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = WhatsAppTeal)
                                }
                            }
                        )
                    } else {
                        Text(name.ifBlank { "User" }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("This is not your username or pin. This name will be visible to your Netice Chat contacts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!isEditingName) {
                    IconButton(onClick = { isEditingName = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = WhatsAppTeal, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // About item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("About", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isEditingAbout) {
                        OutlinedTextField(
                            value = about,
                            onValueChange = { about = it },
                            modifier = Modifier.fillMaxWidth().testTag("edit_about_field"),
                            maxLines = 3,
                            trailingIcon = {
                                IconButton(onClick = { saveDetails() }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = WhatsAppTeal)
                                }
                            }
                        )
                    } else {
                        Text(about.ifBlank { "Hey there! I am using Netice Chat." }, fontSize = 15.sp)
                    }
                }
                if (!isEditingAbout) {
                    IconButton(onClick = { isEditingAbout = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit about", tint = WhatsAppTeal, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Email item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppTeal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val userEmail = currentUser?.email?.ifBlank { repository.currentUserEmail } ?: repository.currentUserEmail ?: "Not provided"
                    Text(userEmail, fontSize = 15.sp)
                }
            }
        }
    }
}

package com.example.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NeticeApplication
import com.example.ui.theme.WhatsAppDarkTeal
import com.example.ui.theme.WhatsAppTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailAuthScreen(
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val repository = remember { NeticeApplication.instance.repository }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Sign Up
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isAwaitingVerification by remember { mutableStateOf(false) }
    var registeredEmail by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingVerification by remember { mutableStateOf(false) }

    // Auto-check email verification polling every 4 seconds when waiting
    LaunchedEffect(isAwaitingVerification) {
        while (isAwaitingVerification) {
            delay(4000L)
            val result = repository.checkEmailVerificationStatus()
            if (result.getOrDefault(false)) {
                isAwaitingVerification = false
                if (repository.currentUser.value?.name?.isNotBlank() == true) {
                    onNavigateToHome()
                } else {
                    onNavigateToProfileSetup()
                }
                break
            }
        }
    }

    fun handleSignIn() {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Please enter a valid email address"
            return
        }
        if (cleanPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        scope.launch {
            val result = repository.signInWithEmail(cleanEmail, cleanPassword)
            isLoading = false
            if (result.isSuccess) {
                val isVerified = result.getOrDefault(false)
                if (isVerified) {
                    if (repository.currentUser.value?.name?.isNotBlank() == true) {
                        onNavigateToHome()
                    } else {
                        onNavigateToProfileSetup()
                    }
                } else {
                    // Email not verified yet
                    registeredEmail = cleanEmail
                    isAwaitingVerification = true
                }
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to sign in. Check email and password."
            }
        }
    }

    fun handleSignUp() {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanConfirm = confirmPassword.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Please enter a valid email address"
            return
        }
        if (cleanPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        if (cleanPassword != cleanConfirm) {
            errorMessage = "Passwords do not match"
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        scope.launch {
            val result = repository.registerWithEmail(cleanEmail, cleanPassword)
            isLoading = false
            if (result.isSuccess) {
                registeredEmail = cleanEmail
                isAwaitingVerification = true
                successMessage = "Verification email sent to $cleanEmail"
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Registration failed."
            }
        }
    }

    fun resendVerification() {
        isLoading = true
        scope.launch {
            val result = repository.resendVerificationEmail()
            isLoading = false
            if (result.isSuccess) {
                successMessage = "New verification link sent to $registeredEmail"
                errorMessage = null
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to resend email"
            }
        }
    }

    fun checkVerificationNow() {
        isCheckingVerification = true
        errorMessage = null
        scope.launch {
            val result = repository.checkEmailVerificationStatus()
            isCheckingVerification = false
            if (result.getOrDefault(false)) {
                isAwaitingVerification = false
                if (repository.currentUser.value?.name?.isNotBlank() == true) {
                    onNavigateToHome()
                } else {
                    onNavigateToProfileSetup()
                }
            } else {
                errorMessage = "Email is not verified yet. Please check your inbox and tap the link."
            }
        }
    }

    fun sendPasswordReset() {
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            errorMessage = "Enter your email address above to receive reset instructions"
            return
        }
        isLoading = true
        scope.launch {
            val result = repository.sendPasswordReset(cleanEmail)
            isLoading = false
            if (result.isSuccess) {
                successMessage = "Password reset link sent to $cleanEmail"
                errorMessage = null
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset link"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isAwaitingVerification) "Verify your email" else "Netice Chat",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    if (isAwaitingVerification) {
                        IconButton(onClick = {
                            isAwaitingVerification = false
                            repository.signOut()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header Branding Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WhatsAppTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAwaitingVerification) Icons.Default.MarkEmailRead else Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = WhatsAppTeal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAwaitingVerification) {
                // Email Verification Screen
                Text(
                    text = "Verify your email address",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We have sent a verification link to:\n$registeredEmail",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Please check your inbox (and spam folder) and click the link to activate your Netice Chat account.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Check Now Button
                Button(
                    onClick = { checkVerificationNow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("check_email_verification_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    enabled = !isCheckingVerification
                ) {
                    if (isCheckingVerification) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I Have Verified My Email", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resend Link Button
                OutlinedButton(
                    onClick = { resendVerification() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resend_verification_email_button"),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isLoading
                ) {
                    Text("Resend Verification Email", color = WhatsAppTeal, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = {
                        isAwaitingVerification = false
                        repository.signOut()
                    }
                ) {
                    Text("Use another email account", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Email Auth: Sign In / Sign Up Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = WhatsAppTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = WhatsAppTeal,
                            height = 3.dp
                        )
                    },
                    divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; errorMessage = null; successMessage = null },
                        text = {
                            Text("SIGN IN", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) WhatsAppTeal else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; errorMessage = null; successMessage = null },
                        text = {
                            Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) WhatsAppTeal else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    placeholder = { Text("e.g. name@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WhatsAppTeal) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppTeal) },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { sendPasswordReset() }) {
                            Text("Forgot password?", fontSize = 13.sp, color = WhatsAppTeal)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (selectedTab == 0) handleSignIn() else handleSignUp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTeal),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (selectedTab == 0) "Sign In" else "Register & Verify Email",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Success feedback
            AnimatedVisibility(visible = successMessage != null) {
                Surface(
                    color = WhatsAppTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = successMessage ?: "",
                        color = WhatsAppDarkTeal,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Error feedback
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

            Spacer(modifier = Modifier.height(32.dp))

            // Quick developer test sign in for immediate demo / simulator without waiting for real inbox
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Development & Simulator Mode",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = WhatsAppDarkTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "If you are running in emulator or quick preview, you can bypass email verification and jump into Netice Chat directly:",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val result = repository.signInAnonymouslyForTesting()
                                isLoading = false
                                if (result.isSuccess) {
                                    if (repository.currentUser.value?.name?.isNotBlank() == true) {
                                        onNavigateToHome()
                                    } else {
                                        onNavigateToProfileSetup()
                                    }
                                } else {
                                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed"
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("dev_quick_signin_button")
                    ) {
                        Text("Skip to App (Instant Test)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

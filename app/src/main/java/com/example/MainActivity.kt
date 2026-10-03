package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.auth.EmailAuthScreen
import com.example.ui.auth.ProfileSetupScreen
import com.example.ui.call.CallScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.UserSearchScreen
import com.example.ui.group.CreateGroupScreen
import com.example.ui.group.GroupInfoScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Routes
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.UserProfileScreen
import com.example.ui.status.CreateStatusScreen
import com.example.ui.status.StatusViewerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NeticeApp()
                }
            }
        }
    }
}

@Composable
fun NeticeApp() {
    val repository = remember { NeticeApplication.instance.repository }
    val currentUser by repository.currentUser.collectAsState()
    val navController = rememberNavController()

    // Permissions Request on app start
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val startDestination = if (repository.isUserLoggedIn && repository.isEmailVerified) {
        if (currentUser != null && currentUser!!.name.isNotBlank()) {
            Routes.HOME
        } else {
            Routes.PROFILE_SETUP
        }
    } else {
        Routes.AUTH
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.AUTH) {
            EmailAuthScreen(
                onNavigateToProfileSetup = {
                    navController.navigate(Routes.PROFILE_SETUP) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToChat = { chatId, otherUserId, otherUserName, isGroup, photoUrl ->
                    navController.navigate(
                        Routes.chatRoute(chatId, otherUserId, otherUserName, isGroup, photoUrl)
                    )
                },
                onNavigateToUserSearch = {
                    navController.navigate(Routes.USER_SEARCH)
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Routes.CREATE_GROUP)
                },
                onNavigateToCreateStatus = {
                    navController.navigate(Routes.CREATE_STATUS)
                },
                onNavigateToStatusViewer = { userId ->
                    navController.navigate(Routes.statusViewerRoute(userId))
                },
                onNavigateToCall = { callId, otherUserId, otherUserName, callType, isIncoming, photoUrl ->
                    navController.navigate(
                        Routes.callRoute(callId, otherUserId, otherUserName, callType, isIncoming, photoUrl)
                    )
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.USER_PROFILE)
                }
            )
        }

        composable(Routes.USER_SEARCH) {
            UserSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onUserSelect = { user ->
                    val myUid = repository.currentUserId ?: ""
                    val chatId = if (myUid < user.uid) "${myUid}_${user.uid}" else "${user.uid}_${myUid}"
                    navController.navigate(
                        Routes.chatRoute(chatId, user.uid, user.name, false, user.photoUrl)
                    ) {
                        popUpTo(Routes.USER_SEARCH) { inclusive = true }
                    }
                },
                onCreateGroupClick = {
                    navController.navigate(Routes.CREATE_GROUP) {
                        popUpTo(Routes.USER_SEARCH) { inclusive = true }
                    }
                },
                onStartCall = { callId, otherUserId, otherUserName, callType, photoUrl ->
                    navController.navigate(
                        Routes.callRoute(callId, otherUserId, otherUserName, callType, false, photoUrl)
                    )
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType },
                navArgument("isGroup") { type = NavType.BoolType },
                navArgument("photoUrl") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val rawName = backStackEntry.arguments?.getString("otherUserName") ?: "Chat"
            val otherUserName = try {
                java.net.URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName
            }
            val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
            val rawPhoto = backStackEntry.arguments?.getString("photoUrl")
            val photoUrl = if (!rawPhoto.isNullOrBlank()) {
                try {
                    java.net.URLDecoder.decode(rawPhoto, "UTF-8")
                } catch (e: Exception) {
                    rawPhoto
                }
            } else null

            ChatScreen(
                chatId = chatId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                isGroup = isGroup,
                photoUrl = photoUrl,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupInfo = { gId ->
                    navController.navigate(Routes.groupInfoRoute(gId))
                },
                onStartCall = { callId, oUserId, oUserName, callType, pUrl ->
                    navController.navigate(
                        Routes.callRoute(callId, oUserId, oUserName, callType, false, pUrl)
                    )
                }
            )
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { groupId, groupName ->
                    navController.navigate(
                        Routes.chatRoute(groupId, groupId, groupName, true, null)
                    ) {
                        popUpTo(Routes.CREATE_GROUP) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.GROUP_INFO,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupInfoScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_STATUS) {
            CreateStatusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.STATUS_VIEWER,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("userId") ?: ""
            StatusViewerScreen(
                targetUserId = targetUserId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CALL,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("isIncoming") { type = NavType.BoolType },
                navArgument("photoUrl") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val rawName = backStackEntry.arguments?.getString("otherUserName") ?: "User"
            val otherUserName = try {
                java.net.URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName
            }
            val callType = backStackEntry.arguments?.getString("callType") ?: "audio"
            val isIncoming = backStackEntry.arguments?.getBoolean("isIncoming") ?: false
            val rawPhoto = backStackEntry.arguments?.getString("photoUrl")
            val photoUrl = if (!rawPhoto.isNullOrBlank()) {
                try {
                    java.net.URLDecoder.decode(rawPhoto, "UTF-8")
                } catch (e: Exception) {
                    rawPhoto
                }
            } else null

            CallScreen(
                callId = callId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                callType = callType,
                isIncoming = isIncoming,
                photoUrl = photoUrl,
                onCallEnded = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Routes.USER_PROFILE) },
                onSignedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.USER_PROFILE) {
            UserProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

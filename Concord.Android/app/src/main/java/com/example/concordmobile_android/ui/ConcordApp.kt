package com.example.concordmobile_android.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.concordmobile_android.ConcordContainer
import com.example.concordmobile_android.service.ConcordForegroundService
import com.example.concordmobile_android.service.NetworkMonitor
import com.example.concordmobile_android.service.NotificationHelper
import com.example.concordmobile_android.ui.screens.ChatScreen
import com.example.concordmobile_android.ui.screens.ContactsScreen
import com.example.concordmobile_android.ui.screens.HomeScreen
import com.example.concordmobile_android.ui.screens.LoginScreen
import com.example.concordmobile_android.ui.screens.ProfileScreen
import com.example.concordmobile_android.ui.screens.RegisterScreen
import com.example.concordmobile_android.ui.screens.RequestsScreen
import com.example.concordmobile_android.viewmodel.AuthViewModel
import com.example.concordmobile_android.viewmodel.ChatViewModel
import com.example.concordmobile_android.viewmodel.ConcordViewModelFactory
import com.example.concordmobile_android.viewmodel.ContactsViewModel
import com.example.concordmobile_android.viewmodel.HomeViewModel
import com.example.concordmobile_android.viewmodel.ProfileViewModel
import com.example.concordmobile_android.viewmodel.RequestsViewModel

object Routes {
    const val Login = "login"
    const val Register = "register"
    const val Home = "home"
    const val Contacts = "contacts"
    const val Requests = "requests"
    const val Chat = "chat/{friendId}"
    const val Profile = "profile/{friendId}"

    fun chat(friendId: Int) = "chat/$friendId"
    fun profile(friendId: Int) = "profile/$friendId"
}

@Composable
fun ConcordApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = remember { ConcordContainer(context) }
    val repository = container.repository
    val isLoggedIn = repository.session.value != null
    val startDestination = if (isLoggedIn) Routes.Home else Routes.Login

    // monitor de rede
    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        networkMonitor.start()
        onDispose { networkMonitor.stop() }
    }

    // cria canal de notificação
    remember { NotificationHelper.createChannel(context); true }

    // pede permissão (Android 13+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (isLoggedIn) ConcordForegroundService.start(context)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(repository) {
        repository.incomingMessage.collect { message ->
            val session = repository.session.value ?: return@collect
            if (message.fromUserId == session.userId) return@collect
            if (currentRoute == "chat/${message.fromUserId}") {
                NotificationHelper.clearHistory(message.fromUserId)
                return@collect
            }
            val sender = repository.cachedFriend(message.fromUserId)
            NotificationHelper.showMessageNotification(
                context = context,
                notifId = message.fromUserId,
                senderName = sender?.username ?: "Concord",
                message = message.text ?: "",
                profileImageBase64 = sender?.profileImageBase64
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Routes.Login) {
                val vm: AuthViewModel = viewModel(factory = ConcordViewModelFactory(repository))
                LoginScreen(
                    viewModel = vm,
                    onCreateAccount = { navController.navigate(Routes.Register) },
                    onLoggedIn = {
                        ConcordForegroundService.start(context)
                        navController.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } }
                    }
                )
            }
            composable(Routes.Register) {
                val vm: AuthViewModel = viewModel(factory = ConcordViewModelFactory(repository))
                RegisterScreen(viewModel = vm, onBackToLogin = { navController.popBackStack() })
            }
            composable(Routes.Home) {
                val vm: HomeViewModel = viewModel(factory = ConcordViewModelFactory(repository))
                HomeScreen(
                    viewModel = vm,
                    onOpenChat = { navController.navigate(Routes.chat(it)) },
                    onOpenContacts = { navController.navigate(Routes.Contacts) },
                    onOpenRequests = { navController.navigate(Routes.Requests) },
                    onOpenProfile = { navController.navigate(Routes.profile(it)) },
                    onLoggedOut = {
                        ConcordForegroundService.stop(context)
                        NotificationHelper.clearAllHistory()
                        navController.navigate(Routes.Login) { popUpTo(Routes.Home) { inclusive = true } }
                    }
                )
            }
            composable(Routes.Contacts) {
                val vm: ContactsViewModel = viewModel(factory = ConcordViewModelFactory(repository))
                ContactsScreen(viewModel = vm, onBack = { navController.popBackStack() }, onOpenProfile = { navController.navigate(Routes.profile(it)) })
            }
            composable(Routes.Requests) {
                val vm: RequestsViewModel = viewModel(factory = ConcordViewModelFactory(repository))
                RequestsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.Chat,
                arguments = listOf(navArgument("friendId") { type = NavType.IntType })
            ) { entry ->
                val friendId = entry.arguments?.getInt("friendId") ?: 0
                LaunchedEffect(friendId) {
                    NotificationHelper.clearHistory(friendId)
                    val manager = context.getSystemService(android.app.NotificationManager::class.java)
                    manager.cancel(friendId)
                }
                val vm: ChatViewModel = viewModel(factory = ConcordViewModelFactory(repository, friendId))
                ChatScreen(viewModel = vm, onBack = { navController.popBackStack() }, onOpenProfile = { navController.navigate(Routes.profile(friendId)) })
            }
            composable(
                route = Routes.Profile,
                arguments = listOf(navArgument("friendId") { type = NavType.IntType })
            ) { entry ->
                val friendId = entry.arguments?.getInt("friendId") ?: 0
                val vm: ProfileViewModel = viewModel(factory = ConcordViewModelFactory(repository, friendId))
                ProfileScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }

        // banner offline — aparece em cima de tudo com animação
        AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB71C1C))
                    .systemBarsPadding()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠ Sem conexão com a internet",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
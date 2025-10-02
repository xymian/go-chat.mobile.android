package com.simulatedtez.gochat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.auth.view.AuthScreens
import com.simulatedtez.gochat.auth.view.LoginScreen
import com.simulatedtez.gochat.auth.view.SignupScreen
import com.simulatedtez.gochat.chat.ChatScreens
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.view.ChatScreen
import com.simulatedtez.gochat.conversations.view.ConversationsScreen
import com.simulatedtez.gochat.conversations.view.ConversationsScreenActions
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import com.simulatedtez.gochat.view_model.AppViewModel
import com.simulatedtez.gochat.view_model.AppViewModelProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appViewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appViewModel = ViewModelProvider(
            this, AppViewModelProvider(this))[AppViewModel::class.java]
        appViewModel.connectToChatService()

        setContent {
            GoChatTheme {
                AppNavigation(this)
            }
        }
    }
}

@Composable
fun AppNavigation(context: Context) {

    val viewModelFactory = remember { AppViewModelProvider(context) }
    val viewModel: AppViewModel = viewModel(factory = viewModelFactory)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->

            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.postNewPresenceStatus(PresenceStatus.AWAY)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.postNewPresenceStatus(PresenceStatus.OFFLINE)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navController = rememberNavController()

    val startDestination = if (UserPreference.getAccessToken() != null) {
        ChatScreens.CONVERSATIONS.name
    } else {
        AuthScreens.LOGIN.name
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AuthScreens.LOGIN.name) {
            navController.LoginScreen()
        }

        composable(AuthScreens.SIGNUP.name) {
            navController.SignupScreen()
        }

        composable(ChatScreens.CONVERSATIONS.name) {
            navController.ConversationsScreen(
                screenActions = object: ConversationsScreenActions {
                    override fun onChatClicked(chatInfo: ChatInfo) {
                        session.setActiveChat(chatInfo)
                        navController.navigate(ChatScreens.CHAT.name)
                    }

                }
            )
        }

        composable(
            ChatScreens.CHAT.name
        ) {
            session.lastActiveChat?.let {
                navController.ChatScreen(it)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    GoChatTheme {
        //AppNavigation(this)
    }
}
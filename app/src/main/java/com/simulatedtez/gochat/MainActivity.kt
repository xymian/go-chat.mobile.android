package com.simulatedtez.gochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.auth.view.AuthScreens
import com.simulatedtez.gochat.auth.view.LoginScreen
import com.simulatedtez.gochat.auth.view.SignupScreen
import com.simulatedtez.gochat.chat.ChatScreens
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.view.ChatScreen
import com.simulatedtez.gochat.conversations.view.ConversationsScreen
import com.simulatedtez.gochat.conversations.view.ConversationsScreenActions
import com.simulatedtez.gochat.ui.theme.GoChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoChatTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
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
            session.activeChat?.let {
                navController.ChatScreen(it)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    GoChatTheme {
        AppNavigation()
    }
}
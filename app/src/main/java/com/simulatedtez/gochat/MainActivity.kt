package com.simulatedtez.gochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.auth.view.LoginScreen
import com.simulatedtez.gochat.auth.view.SignupScreen
import com.simulatedtez.gochat.chat.view.ConversationsScreen
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

    NavHost(navController = navController, startDestination = "conversations") {
        composable("login") {
            navController.LoginScreen()
        }

        composable("signup") {
            navController.SignupScreen()
        }

        composable("conversations") {
            navController.ConversationsScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GoChatTheme {
        AppNavigation()
    }
}
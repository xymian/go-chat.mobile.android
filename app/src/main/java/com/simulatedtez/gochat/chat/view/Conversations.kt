package com.simulatedtez.gochat.chat.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.R
import com.simulatedtez.gochat.auth.view.SignupScreen
import com.simulatedtez.gochat.chat.models.Conversation
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import io.ktor.websocket.Frame

val sampleChats = listOf(
    Conversation(1, "Jane Doe", "Hey, how are you?", "10:42 AM", 2, ""),
    Conversation(2, "John Smith", "Let's catch up tomorrow.", "9:21 AM", 0, ""),
    Conversation(3, "Project Team", "Don't forget the meeting at 3 PM.", "Yesterday", 5, ""),
    Conversation(4, "Sarah Lee", "Sounds good!", "Yesterday", 0, ""),
    Conversation(5, "Mom", "Can you call me back?", "2 days ago", 1, ""),
    Conversation(6, "Design Group", "I've uploaded the new mockups.", "2 days ago", 0, "")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavController.ConversationsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Frame.Text("Chats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Navigate to new chat screen */ },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Chat", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(sampleChats) { chat ->
                ChatItem(chat = chat)
            }
        }
    }
}

@Composable
fun ChatItem(chat: Conversation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {  }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_default_user_image),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.contactName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = chat.lastMessage,
                color = Color.Gray,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = chat.timestamp,
                color = Color.Gray,
                fontSize = 12.sp
            )
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(text = chat.unreadCount.toString(), color = Color.White)
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 82.dp),
        thickness = DividerDefaults.Thickness,
        color = DividerDefaults.color
    )
}

@Preview(showBackground = true)
@Composable
fun ConversationsPreview() {
    GoChatTheme {
        rememberNavController().ConversationsScreen()
    }
}
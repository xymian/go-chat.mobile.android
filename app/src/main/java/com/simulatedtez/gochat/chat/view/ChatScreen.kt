package com.simulatedtez.gochat.chat.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.view_model.ChatViewModel
import com.simulatedtez.gochat.chat.view_model.ChatViewModelProvider
import com.simulatedtez.gochat.utils.formatTimestamp

val sampleMessages = listOf(
    Message(
        id = "1",
        messageReference = null,
        message = "hey bob!",
        sender = "alice",
        receiverUsername = "bob",
        timestamp = "2025-07-13T12:00:00Z",
        chatReference = "chat123",
        seenByReceiver = true
    ),
    Message(
        id = "2",
        messageReference = "1",
        message = "hey alice! how’s it going?",
        sender = session.username,
        receiverUsername = "alice",
        timestamp = "2025-07-13T12:00:05Z",
        chatReference = "chat123",
        seenByReceiver = true
    ),
    Message(
        id = "3",
        messageReference = "2",
        message = "doing great, just working on a new app",
        sender = "alice",
        receiverUsername = "bob",
        timestamp = "2025-07-13T12:00:12Z",
        chatReference = "chat123",
        seenByReceiver = true
    ),
    Message(
        id = "4",
        messageReference = "3",
        message = "nice! what kind of app?",
        sender = session.username,
        receiverUsername = "alice",
        timestamp = "2025-07-13T12:00:20Z",
        chatReference = "chat123",
        seenByReceiver = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavController.ChatScreen(chatInfo: ChatInfo) {
    var messageText by remember { mutableStateOf("") }
    
    val chatViewModelProvider = remember { ChatViewModelProvider(
        chatInfo = chatInfo, context
    ) }

    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelProvider)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatInfo.recipientsUsernames[0], fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        navigateUp()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            MessageInputBar(
                message = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    // TODO: Handle sending message
                    messageText = ""
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true // To show the latest messages at the bottom
        ) {
            items(sampleMessages.reversed()) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Composable for a single message bubble
@Composable
fun MessageBubble(message: Message) {
    val bubbleColor = if (message.sender == session.username) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.inversePrimary
    }

    val textColor = if (message.sender == session.username) {
        Color.White
    } else {
        Color.Black
    }

    val alignment = if (message.sender == session.username) Alignment.End else Alignment.Start

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.sender == session.username) 16.dp else 0.dp,
                            bottomEnd = if (message.sender == session.username) 0.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.message ?: "",
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(message.timestamp!!),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// Composable for the message input bar at the bottom
@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendClick,
                enabled = message.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    rememberNavController().ChatScreen(chatInfo = ChatInfo(
        username = session.username,
        recipientsUsernames = listOf("Jane Doe"),
        chatReference = "",
        socketURL = ""
    ))
}
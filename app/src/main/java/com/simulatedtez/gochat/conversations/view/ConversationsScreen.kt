package com.simulatedtez.gochat.conversations.view

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
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.R
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.conversations.models.Conversation
import com.simulatedtez.gochat.conversations.view_model.ConversationsViewModel
import com.simulatedtez.gochat.conversations.view_model.ConversationsViewModelProvider
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import com.simulatedtez.gochat.utils.formatTimestamp
import io.ktor.websocket.Frame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavController.ConversationsScreen(screenActions: ConversationsScreenActions) {

    val snackbarHostState = remember { SnackbarHostState() }

    val conversations = remember { mutableStateListOf<Conversation>() }

    val viewModelFactory = remember { ConversationsViewModelProvider(context) }
    val viewModel: ConversationsViewModel = viewModel(factory = viewModelFactory)

    val waiting by viewModel.waiting.observeAsState(false)
    val newConversation by viewModel.newConversation.observeAsState()
    val conversationHistory by viewModel.conversations.observeAsState(listOf())
    val errorMessage by viewModel.errorMessage.observeAsState()

    val newMessages by viewModel.newMessages.collectAsState()
    val isConnected by viewModel.isConnected.observeAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(isConnected) {
        isConnected?.let {
            if (it) {
                snackbarHostState.showSnackbar("socket is connected")
            } else {
                snackbarHostState.showSnackbar("socket is disconnected")
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            showBottomSheet = false
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(conversationHistory) {
        if (conversationHistory.isNotEmpty()) {
            conversations.clear()
            conversations.addAll(conversationHistory)
            viewModel.connectToChatService()
        }
    }

    LaunchedEffect(newConversation) {
        newConversation?.let {
            conversations.add(it)
            showBottomSheet = false
        }
    }

    LaunchedEffect(newMessages) {
        if (newMessages.isNotEmpty()) {
            viewModel.rebuildConversations(conversations, newMessages.toList())
            viewModel.resetNewMessagesFlow()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                onClick = {
                    showBottomSheet = true
                },
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
            items(conversations) { chat ->
                ChatItem(chat = chat, screenActions)
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            NewChatSheetContent(
                !waiting,
                onAddClick = { username ->
                    viewModel.addNewConversation(session.username, username)
                }
            )
        }
    }
}

@Composable
fun ChatItem(chat: Conversation, screenActions: ConversationsScreenActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                screenActions.onChatClicked(
                    ChatInfo(
                        username = session.username,
                        recipientsUsernames = listOf(chat.other),
                        chatReference = chat.chatReference
                    )
                )
            }
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
                text = chat.other,
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
                text = formatTimestamp(chat.timestamp),
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



@Composable
fun NewChatSheetContent(isEnabled:Boolean, onAddClick: (String) -> Unit) {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Start a new chat", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Enter username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onAddClick(username) },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && isEnabled
        ) {
            Text("Add")
        }
        Spacer(modifier = Modifier.height(16.dp)) // Add padding at the bottom
    }
}

interface ConversationsScreenActions {
    fun onChatClicked(chatInfo: ChatInfo)
}

@Preview(showBackground = true)
@Composable
fun ConversationsPreview() {
    GoChatTheme {
        rememberNavController().ConversationsScreen(
            screenActions = object: ConversationsScreenActions {
                override fun onChatClicked(chatInfo: ChatInfo) {

                }
            }
        )
    }
}
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.GoChatApplication
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.auth.view.AuthScreens
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import com.simulatedtez.gochat.chat.view_model.ChatViewModel
import com.simulatedtez.gochat.chat.view_model.ChatViewModelProvider
import com.simulatedtez.gochat.utils.INetworkMonitor
import com.simulatedtez.gochat.utils.NetworkMonitor
import com.simulatedtez.gochat.utils.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavController.ChatScreen(chatInfo: ChatInfo) {

    val app = LocalContext.current.applicationContext as GoChatApplication

    val chatViewModelProvider = remember { ChatViewModelProvider(
        chatInfo = chatInfo, context
    ) }
    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelProvider)

    val snackbarHostState = remember { SnackbarHostState() }

    val messages = remember { mutableStateSetOf<UIMessage>() }
    var messageText by remember { mutableStateOf("") }
    var hasFinishedInitialMessagesLoad by remember { mutableStateOf(false) }
    
    val newMessage by chatViewModel.newMessage.observeAsState()
    val pagedMessages by chatViewModel.pagedMessages.observeAsState()
    val sentMessage by chatViewModel.sendMessageAttempt.observeAsState()
    val isConnected by chatViewModel.isConnected.observeAsState()
    val tokenExpired by chatViewModel.tokenExpired.observeAsState()
    val messagesSent by chatViewModel.messagesSent.observeAsState()

    val listState = rememberLazyListState()

    val networkCallbacks = object: NetworkMonitor.Callbacks {
        override fun onAvailable() {
            if (hasFinishedInitialMessagesLoad) {
                chatViewModel.connectAndSendPendingMessages()
            }
        }

        override fun onLost() {

        }
    }
    (app as INetworkMonitor).setCallback(networkCallbacks)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->

            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    chatViewModel.loadMessages()
                }
                Lifecycle.Event.ON_PAUSE -> {
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!chatViewModel.isChatServiceConnected()) {
                        chatViewModel.connectAndSendPendingMessages()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    chatViewModel.exitChat()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            chatViewModel.exitChat()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(messagesSent) {
        val modifiedMessages = messages.toMutableList()
        messagesSent.let {
            it?.let {
                val messageIndex = modifiedMessages.indexOfFirst {
                        m -> it.message.id ==  m.message.id
                }
                if (messageIndex != -1) {
                    modifiedMessages[messageIndex] = it
                }
                messages.addAll(modifiedMessages)
                if (messagesSent?.message?.seenTimestamp.isNullOrEmpty()) {
                    chatViewModel.markMessagesAsSeen(listOf(messagesSent!!.message))
                }
                chatViewModel.getNextOutgoingMessage()
            }
        }
    }

    LaunchedEffect(hasFinishedInitialMessagesLoad) {
        if (hasFinishedInitialMessagesLoad) {
            if (!chatViewModel.isChatServiceConnected()) {
                chatViewModel.connectAndSendPendingMessages()
            }
        }
    }

    LaunchedEffect(tokenExpired) {
        tokenExpired?.let {
            if (it) {
                navigate(AuthScreens.LOGIN.name)
                chatViewModel.resetTokenExpired()
            }
        }
    }

    LaunchedEffect(sentMessage) {
        sentMessage?.let {
            messages.add(it)
        }
    }

    LaunchedEffect(isConnected) {
        isConnected?.let {
            if (it) {
                snackbarHostState.showSnackbar("socket is connected")
            } else {
                snackbarHostState.showSnackbar("socket is disconnected")
            }
        }
    }

    LaunchedEffect(pagedMessages) {
        pagedMessages?.let {
            if (it.paginationCount <= 1) {
                messages.clear()
                messages.apply {
                    addAll(it.messages)
                    sortedBy { m -> m.message.timestamp }
                }
                chatViewModel.markConversationAsOpened()
            } else {
                messages.apply {
                    addAll(it.messages)
                    sortedBy { m -> m.message.timestamp }
                }
            }
            if (!hasFinishedInitialMessagesLoad) {
                hasFinishedInitialMessagesLoad = true
            }
        }
    }

    LaunchedEffect(newMessage) {
        newMessage.let {
            it?.let {
                messages.add(it)
                chatViewModel.getNextIncomingMessage()
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
        }.collect { visibleItems ->

            val visibleMessages = visibleItems.map { item ->
                messages.toList()[(messages.size - 1) - item.index]
            }

            val unseenMessages = visibleMessages.filterIndexed { index, m ->
                m.message.sender != session.username && m.message.seenTimestamp.isNullOrEmpty()
            }

            chatViewModel.markMessagesAsSeen(unseenMessages.map {
                it.message
            })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    chatViewModel.sendMessage(messageText)
                    messageText = ""
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(message: UIMessage) {
    val bubbleColor = if (message.message.sender == session.username) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.inversePrimary
    }

    val textColor = if (message.message.sender == session.username) {
        Color.White
    } else {
        Color.Black
    }

    val alignment = if (message.message.sender == session.username) {
        Alignment.End
    } else Alignment.Start

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
                            bottomStart = if (message.message.sender == session.username) 16.dp else 0.dp,
                            bottomEnd = if (message.message.sender == session.username) 0.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.message.message,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimestamp(message.message.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (message.message.sender == session.username) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageStatusIndicator(status = message.status)
                }
            }
        }
    }
}

@Composable
fun MessageStatusIndicator(status: MessageStatus) {
    val (icon, color) = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule to Color.Gray
        MessageStatus.SENT -> Icons.Default.Check to MaterialTheme.colorScheme.primary
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
        MessageStatus.SEEN -> Icons.Default.Visibility to MaterialTheme.colorScheme.primary
        else -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }

    Icon(
        imageVector = icon,
        contentDescription = "Message Status",
        tint = color,
        modifier = Modifier.size(14.dp)
    )
}

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
                .padding(horizontal = 8.dp, vertical = 16.dp),
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
        chatReference = ""
    ))
}
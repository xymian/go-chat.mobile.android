package com.simulatedtez.gochat.chat.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.Dp
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
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import com.simulatedtez.gochat.chat.view_model.ChatViewModel
import com.simulatedtez.gochat.chat.view_model.ChatViewModelProvider
import com.simulatedtez.gochat.utils.INetworkMonitor
import com.simulatedtez.gochat.utils.NetworkMonitor
import com.simulatedtez.gochat.utils.formatTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavController.ChatScreen(chatInfo: ChatInfo) {

    val app = LocalContext.current.applicationContext as GoChatApplication

    val chatViewModelProvider = remember {
        ChatViewModelProvider(chatInfo = chatInfo, context)
    }
    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelProvider)

    val snackbarHostState = remember { SnackbarHostState() }

    val messages = remember { mutableStateSetOf<UIMessage>() }
    var messageText by remember { mutableStateOf("") }
    var hasFinishedInitialMessagesLoad by remember { mutableStateOf(false) }
    
    val newMessage by chatViewModel.newMessage.observeAsState(null)
    val pagedMessages by chatViewModel.pagedMessages.observeAsState()
    val sentMessage by chatViewModel.sendMessageAttempt.observeAsState()
    val isConnected by chatViewModel.isConnected.observeAsState()
    val tokenExpired by chatViewModel.tokenExpired.observeAsState()
    val messagesSent by chatViewModel.messagesSent.observeAsState(null)
    val presenceStatus by chatViewModel.recipientStatus.observeAsState()
    val typingTimeLeft by chatViewModel.typingTimeLeft.observeAsState()
    val isUserTyping by chatViewModel.isUserTyping.observeAsState()

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
                    chatViewModel.postPresence(PresenceStatus.AWAY)
                }
                Lifecycle.Event.ON_RESUME -> {
                    chatViewModel.postPresence(PresenceStatus.ONLINE)
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

    LaunchedEffect(typingTimeLeft) {
        typingTimeLeft?.let {
            if (it > 0) {
                delay(1000L)
                chatViewModel.countdownTypingTimeBy(1)
            } else {
                delay(2000L)
                if (chatViewModel.isTyping) {
                    chatViewModel.restartTypingTimer(chatViewModel.newCharCount)
                } else {
                    chatViewModel.postMessageStatus(MessageStatus.NOT_TYPING)
                    chatViewModel.stopTypingTimer()
                }
            }
        }
    }

    LaunchedEffect(messagesSent) {
        messagesSent?.let { msg ->
            val modifiedMessages = messages.toMutableList()
            val messageIndex = modifiedMessages.indexOfFirst {
                    m -> msg.message.id ==  m.message.id
            }
            if (messageIndex != -1) {
                modifiedMessages[messageIndex] = msg
            }
            messages.clear()
            messages.addAll(modifiedMessages)
            chatViewModel.popSentMessagesQueue()
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
            chatViewModel.resetSendAttempt()
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
        newMessage?.let {
            messages.add(it)
            if (it.message.seenTimestamp.isNullOrEmpty()) {
                chatViewModel.onUserPresenceOnline {
                    chatViewModel.markMessagesAsSeen(listOf(it.message))
                }
            }
            chatViewModel.popReceivedMessagesQueue()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
        }.collect { visibleItems ->

            if (isUserTyping != true) {
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
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    // 3. Update TopAppBar to show name and status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(chatInfo.recipientsUsernames[0], fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        PresenceIndicator(status = presenceStatus)
                    }
                },
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
                onMessageChange = {
                    if (messageText.length < it.length) {
                        if (messageText.isEmpty()) {
                            chatViewModel.restartTypingTimer(it.length)
                            chatViewModel.postMessageStatus(MessageStatus.TYPING)
                        } else {
                            if (typingTimeLeft == null) {
                                chatViewModel.restartTypingTimer(it.length)
                                chatViewModel.postMessageStatus(MessageStatus.TYPING)
                            } else {
                                chatViewModel.newCharCount = it.length
                            }
                        }
                    } else {
                        chatViewModel.newCharCount = it.length
                    }
                    messageText = it
                },
                onSendClick = {
                    chatViewModel.stopTypingTimer()
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
            if (isUserTyping == true) {
                item {
                    TypingIndicatorBubble()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(messages.reversed()) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing-indicator-transition")

    val dotBounceOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f, // How high the dots will bounce
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot-bounce"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // We create three dots and apply the animation with a delay to each one.
                TypingDot(delay = 0.dp, bounce = dotBounceOffsetY)
                TypingDot(delay = 160.dp, bounce = dotBounceOffsetY)
                TypingDot(delay = 320.dp, bounce = dotBounceOffsetY)
            }
        }
    }
}

@Composable
fun TypingDot(delay: Dp, bounce: Float) {
    var yOffset by remember { mutableFloatStateOf(0f) }

    // Use LaunchedEffect to apply animation with delay
    LaunchedEffect(bounce) {
        delay(delay.value.toLong())
        yOffset = bounce
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = yOffset.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.7f))
    )
}

@Composable
fun PresenceIndicator(status: PresenceStatus?) {
    val color = when (status) {
        PresenceStatus.ONLINE -> Color(0xFF4CAF50)
        PresenceStatus.AWAY -> Color(0xFFFFC107)
        PresenceStatus.OFFLINE -> Color.Gray
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
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
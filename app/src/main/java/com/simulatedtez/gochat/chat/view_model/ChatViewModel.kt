package com.simulatedtez.gochat.chat.view_model

import ChatServiceErrorResponse
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.interfaces.ChatEventListener
import com.simulatedtez.gochat.chat.repository.ChatRepository
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.ChatPage
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.chat.remote.models.toUIMessage
import com.simulatedtez.gochat.chat.remote.models.toUIMessages
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.remote.client
import com.simulatedtez.gochat.utils.toISOString
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import java.util.Date
import java.util.UUID

class ChatViewModel(
    private val chatInfo: ChatInfo,
    private val chatRepo: ChatRepository
): ViewModel(), ChatEventListener {

    private val _messagesSent = MutableStateFlow<HashSet<UIMessage>>(hashSetOf())
    val messagesSent: StateFlow<HashSet<UIMessage>> = _messagesSent

    private val _messageIsDelivered = MutableStateFlow<HashSet<UIMessage>>(hashSetOf())
    val messageIsDelivered: StateFlow<HashSet<UIMessage>> = _messageIsDelivered

    private val _newMessages = MutableStateFlow<HashSet<UIMessage>>(hashSetOf())
    val newMessages: StateFlow<HashSet<UIMessage>> = _newMessages

    private val _pagedMessages = MutableLiveData<ChatPage>()
    val pagedMessages: LiveData<ChatPage> = _pagedMessages

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _sendMessageAttempt = MutableLiveData<UIMessage>()
    val sendMessageAttempt: LiveData<UIMessage> = _sendMessageAttempt

    private val _tokenExpired = MutableLiveData<Boolean>()
    val tokenExpired: LiveData<Boolean> = _tokenExpired

    private val _conflictingMessages = MutableLiveData<List<UIMessage>>()
    val conflictingMessages: LiveData<List<UIMessage>> = _conflictingMessages

    fun resetTokenExpired() {
        _tokenExpired.value = false
    }

    fun resetNewMessagesFlow() {
        _newMessages.value = hashSetOf()
    }

    fun resetMessagesSentFlow() {
        _messagesSent.value = hashSetOf()
    }

    fun resetMessageDeliveredFlow() {
        _messageIsDelivered.value = hashSetOf()
    }

    fun sendMessage(message: String) {
        val message = Message(
            id = "",
            messageReference = UUID.randomUUID().toString(),
            message = message,
            sender = chatInfo.username,
            receiver = chatInfo.recipientsUsernames[0],
            timestamp = Date().toISOString(),
            chatReference = chatInfo.chatReference,
            ack = false
        )
        _sendMessageAttempt.value = message.toUIMessage()
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.sendMessage(message)
        }
    }

    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.postValue(chatRepo.loadNextPageMessages())
        }
    }

    fun markConversationAsOpened() {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.markConversationAsOpened()
        }
    }

    fun connectToChatService() {
        chatRepo.connectToChatService()
    }

    fun connectAndSendPendingMessages() {
        chatRepo.connectAndSendPendingMessages()
    }

    fun exitChat() {
        chatRepo.killChatService()
    }

    fun pauseChat() {
        chatRepo.pauseChatService()
    }

    fun resumeChat() {
        chatRepo.resumeChatService()
    }

    override fun onMessageDelivered(message: Message) {
        val uiMessage = message.toUIMessage()
        uiMessage.status = MessageStatus.DELIVERED
        _messageIsDelivered.value = (_messageIsDelivered.value + uiMessage) as HashSet<UIMessage>
    }

    override fun onMessageSent(message: Message) {
        val uiMessage = message.toUIMessage()
        uiMessage.status = MessageStatus.SENT
        _messagesSent.value = (_messagesSent.value + uiMessage) as HashSet<UIMessage>
    }

    override fun onClose(code: Int, reason: String) {
        _isConnected.value = false
    }

    override fun onSend(message: Message) {
        Napier.d("message: ${message.message} sent to ${chatInfo.recipientsUsernames[0]}")
    }

    override fun onConnect() {
        Napier.d("socket connected")
        _isConnected.value = true
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        Napier.d("socket disconnected")
        _isConnected.value = false
    }

    override fun onError(error: ChatServiceErrorResponse) {
        Napier.d(error.reason)
        if (error.statusCode == HttpStatusCode.Unauthorized.value) {
            _tokenExpired.value = true
        }
    }

    override fun onNewMessages(messages: List<Message>) {
        val uiMessages = messages.toUIMessages().apply {
            forEach { it.status = MessageStatus.SENT }
        }
        _newMessages.value = (_newMessages.value + uiMessages) as HashSet<UIMessage>
    }

    override fun onNewMessage(message: Message) {
        val uiMessage = message.toUIMessage()
        uiMessage.status = MessageStatus.SENT
        _newMessages.value = (_newMessages.value + uiMessage) as HashSet<UIMessage>
    }

    override fun onConflictingMessagesDetected(messages: List<Message>) {
        _conflictingMessages.value = messages.toUIMessages().apply {
            forEach { it.status = MessageStatus.SENT }
        }
    }

    override fun onMessagesSent(messages: List<Message>) {
        TODO("Not yet implemented")
    }

    fun cancel() {
        viewModelScope.cancel()
    }

    fun isChatServiceConnected(): Boolean {
        return chatRepo.isChatServiceConnected()
    }
}

class ChatViewModelProvider(
    private val chatInfo: ChatInfo, private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ChatRepository(
            chatInfo = chatInfo,
            CreateChatRoomUsecase(ChatApiService(client)),
            chatDb = ChatDatabase.get(context),
            ConversationDatabase.get(context)
        )

        val chatViewModel = ChatViewModel(chatInfo, repo).apply {
            repo.setChatEventListener(this)
        }
        return chatViewModel as T
    }
}
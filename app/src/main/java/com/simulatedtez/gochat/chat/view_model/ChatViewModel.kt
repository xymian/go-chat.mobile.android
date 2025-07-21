package com.simulatedtez.gochat.chat.view_model

import ChatServiceErrorResponse
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.repository.ChatEventListener
import com.simulatedtez.gochat.chat.repository.ChatRepository
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.ChatPage
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesParams
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

    private val _pagedMessages = MutableLiveData<ChatPage>()
    val pagedMessages: LiveData<ChatPage> = _pagedMessages

    private val _newMessages = MutableStateFlow<HashSet<Message>>(hashSetOf())
    val newMessages: StateFlow<HashSet<Message>> = _newMessages

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _sentMessage = MutableLiveData<Message>()
    val sentMessage: LiveData<Message> = _sentMessage

    private val _tokenExpired = MutableLiveData<Boolean>()
    val tokenExpired: LiveData<Boolean> = _tokenExpired

    private val _conflictingMessages = MutableLiveData<List<Message>>()
    val conflictingMessages: LiveData<List<Message>> = _conflictingMessages

    fun resetTokenExpired() {
        _tokenExpired.value = false
    }

    fun resetNewMessagesFlow() {
        _newMessages.value = hashSetOf()
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
        _sentMessage.value = message
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.sendMessage(message)
        }
    }

    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.postValue(chatRepo.loadNextPageMessages())
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
        _newMessages.value = (_newMessages.value + messages) as HashSet<Message>
    }

    override fun onNewMessage(message: Message) {
        _newMessages.value = (_newMessages.value + message) as HashSet<Message>
    }

    override fun onConflictingMessagesDetected(messages: List<Message>) {
        _conflictingMessages.value = messages
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
            GetMissingMessagesUsecase(
                params = GetMissingMessagesParams(
                    request = GetMissingMessagesParams.Request(
                        chatReference = chatInfo.chatReference,
                        yourUsername = chatInfo.username
                    )
                ),
                chatApiService = ChatApiService(client)
            ),
            AcknowledgeMessagesUsecase(
                chatInfo = chatInfo,
                chatApiService = ChatApiService(client)
            ),
            chatDb = ChatDatabase.get(context)
        )

        val chatViewModel = ChatViewModel(chatInfo, repo).apply {
            repo.setChatEventListener(this)
        }
        return chatViewModel as T
    }
}
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
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.repository.ChatRepository
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.ChatPage
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.chat.remote.models.toUIMessage
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.remote.client
import com.simulatedtez.gochat.utils.toISOString
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import java.time.LocalDateTime
import java.util.UUID

class ChatViewModel(
    private val chatInfo: ChatInfo,
    private val chatRepo: ChatRepository
): ViewModel(), ChatEventListener {

    companion object {
        const val TYPING_TOTAL_TIME = 3
    }

    private val _isUserTyping = MutableLiveData(false)
    val isUserTyping: LiveData<Boolean> = _isUserTyping

    private val _typingTimeLeft = MutableLiveData<Int?>(null)
    val typingTimeLeft: LiveData<Int?> = _typingTimeLeft

    private val _messagesSent = Channel<UIMessage>()
    val messagesSent = _messagesSent.receiveAsFlow()

    private val _newMessage = Channel<UIMessage>()
    val newMessage = _newMessage.receiveAsFlow()

    private val _pagedMessages = MutableLiveData<ChatPage>()
    val pagedMessages: LiveData<ChatPage> = _pagedMessages

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _sendMessageAttempt = MutableLiveData<UIMessage?>()
    val sendMessageAttempt: LiveData<UIMessage?> = _sendMessageAttempt

    private val _tokenExpired = MutableLiveData<Boolean>()
    val tokenExpired: LiveData<Boolean> = _tokenExpired

    private val _recipientStatus = MutableLiveData<PresenceStatus>()
    val recipientStatus: LiveData<PresenceStatus> = _recipientStatus

    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.postValue(chatRepo.loadNextPageMessages())
        }
    }

    fun stopTypingTimer() {
        _typingTimeLeft.value = null
    }

    fun restartTypingTimer() {
        _typingTimeLeft.value = TYPING_TOTAL_TIME
    }

    fun countdownTypingTimeBy(amount: Int) {
        _typingTimeLeft.value = _typingTimeLeft.value?.minus(amount)
    }

    fun resetSendAttempt() {
        _sendMessageAttempt.value = null
    }

    fun resetTokenExpired() {
        _tokenExpired.value = false
    }

    fun sendMessage(message: String) {
        val message = chatRepo.buildUnsentMessage(message)
        _sendMessageAttempt.value = message.toUIMessage(false)
        chatRepo.sendMessage(message)
    }

    fun postMessageStatus(messageStatus: MessageStatus) {
        chatRepo.postMessageStatus(messageStatus)
    }

    fun postPresence(presenceStatus: PresenceStatus) {
        chatRepo.postPresence(presenceStatus)
    }

    fun markConversationAsOpened() {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.markConversationAsOpened()
        }
    }

    fun markMessagesAsSeen(messages: List<Message>) {
        chatRepo.markMessagesAsSeen(messages)
    }

    fun connectAndSendPendingMessages() {
        chatRepo.connectAndSendPendingMessages()
    }

    fun exitChat() {
        chatRepo.killChatService()
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

    override fun onReceiveRecipientActivityStatusMessage(presenceStatus: PresenceStatus) {
        _recipientStatus.value = presenceStatus
    }

    override fun onReceiveRecipientMessageStatus(messageStatus: MessageStatus) {
        when (messageStatus) {
            MessageStatus.TYPING -> {
                _isUserTyping.value = true
            }
            else -> _isUserTyping.value = false
        }
    }

    override suspend fun onMessageSent(message: Message) {
        _messagesSent.send(message.toUIMessage(true))
    }

    override suspend fun onReceive(message: Message) {
        _isUserTyping.postValue(false)
        _newMessage.send(message.toUIMessage(true))
    }

    override fun onCleared() {
        viewModelScope.cancel()
        chatRepo.cancel()
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
package com.simulatedtez.gochat.chat.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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

    private val _newMessage = MutableLiveData<Message>()
    val newMessage: LiveData<Message> = _newMessage

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _sentMessage = MutableLiveData<Message>()
    val sentMessage: LiveData<Message> = _sentMessage

    fun sendMessage(message: String) {
        val message = Message(
            id = "",
            messageReference = UUID.randomUUID().toString(),
            message = message,
            sender = chatInfo.username,
            receiverUsername = chatInfo.recipientsUsernames[0],
            timestamp = Date().toISOString(),
            chatReference = chatInfo.chatReference,
            seenByReceiver = false
        )
        _sentMessage.value = message
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.sendMessage(message)
        }
    }

    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.value = chatRepo.loadNextPageMessages()
        }
    }

    fun stopListeningForMessages() {
        chatRepo.disconnect()
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

    override fun onError(message: String) {
        Napier.d(message)
    }

    override fun onNewMessages(messages: List<Message>) {
        viewModelScope.launch {
            messages.forEach {
                _newMessage.postValue(it)
            }
        }
    }

    override fun onNewMessage(message: Message) {
        viewModelScope.launch {
            _newMessage.value = message
        }
    }

    fun cancel() {
        viewModelScope.cancel()
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
                    headers = GetMissingMessagesParams.Headers(acccessToken = session.accessToken),
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
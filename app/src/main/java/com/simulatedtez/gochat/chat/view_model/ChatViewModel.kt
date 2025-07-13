package com.simulatedtez.gochat.chat.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.repository.ChatEventListener
import com.simulatedtez.gochat.chat.repository.ChatRepository
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.ChatPage
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesParams
import com.simulatedtez.gochat.remote.client
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ChatViewModel(
    val chatRepo: ChatRepository
): ViewModel(), ChatEventListener {

    private val _pagedMessages = MutableLiveData<ChatPage>()
    val pagedMessages: LiveData<ChatPage> = _pagedMessages

    private val _newMessage = MutableLiveData<Message>()
    val newMessage: LiveData<Message> = _newMessage

    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.value = chatRepo.loadNextPageMessages()
        }
    }

    override fun onSend(message: Message) {

    }

    override fun onConnect() {
        Napier.d("socket connected")
    }

    override fun onDisconnect() {
        Napier.d("socket disconnected")
    }

    override fun onError(message: String) {
        Napier.d(message)
    }

    override fun onNewMessages(messages: List<Message>) {
        viewModelScope.launch {
            messages.forEach {
                _newMessage.value = it
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
            GetMissingMessagesUsecase(
                params = GetMissingMessagesParams(
                    headers = GetMissingMessagesParams.Headers(token = session.accessToken),
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

        val chatViewModel = ChatViewModel(repo).apply {
            repo.setChatEventListener(this)
        }
        return chatViewModel as T
    }
}
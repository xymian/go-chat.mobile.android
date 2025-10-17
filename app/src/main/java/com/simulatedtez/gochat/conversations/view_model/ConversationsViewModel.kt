package com.simulatedtez.gochat.conversations.view_model

import ChatServiceErrorResponse
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toUIMessage
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.DBConversation
import com.simulatedtez.gochat.conversations.interfaces.ConversationEventListener
import com.simulatedtez.gochat.conversations.remote.api_services.ConversationsService
import com.simulatedtez.gochat.conversations.remote.api_usecases.AddNewChatUsecase
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.conversations.repository.ConversationsRepository
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.client
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import java.util.LinkedList
import java.util.Queue

class ConversationsViewModel(
    private val conversationsRepository: ConversationsRepository
): ViewModel(), ConversationEventListener {

    private val _tokenExpired = MutableLiveData<Boolean>()
    val tokenExpired: LiveData<Boolean> = _tokenExpired

    private val _waiting = MutableLiveData<Boolean>()
    val waiting: LiveData<Boolean> = _waiting

    private val _newConversation = Channel<DBConversation>()
    val newConversation = _newConversation.receiveAsFlow()

    private val _conversations = MutableLiveData<List<DBConversation>>()
    val conversations: LiveData<List<DBConversation>> = _conversations

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isUserTyping = MutableLiveData<Pair<String, Boolean>>()
    val isUserTyping: LiveData<Pair<String, Boolean>> = _isUserTyping

    private val receivedMessagesQueue: Queue<Message> = LinkedList()

    fun resetTokenExpired() {
        _tokenExpired.value = false
    }

    fun fetchConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            _conversations.postValue(
                conversationsRepository.getConversations().toMutableList()
            )
        }
    }

    fun addNewConversation(other: String, messageCount: Int) {
        _waiting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.addNewConversation(other, messageCount)
        }
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    override fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>) {
        _waiting.value = false
        if (error.response?.statusCode == HttpStatusCode.NotFound.value) {
            _errorMessage.value = error.response.message
        }
    }

    override fun onNewChatAdded(chat: NewChatResponse) {
        val newConversation = DBConversation(
            otherUser = chat.other,
            chatReference = chat.chatReference
        )
        viewModelScope.launch(Dispatchers.IO) {
            _newConversation.send(newConversation)
            _waiting.postValue(false)
        }
    }

    override fun onError(response: IResponse.Failure<ParentResponse<String>>) {
        if (response.response?.statusCode == HttpStatusCode.Unauthorized.value) {
            _tokenExpired.value = true
        }
    }

    fun connectToChatService() {
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.connectToChatService()
        }
    }

    override fun onClose(code: Int, reason: String) {
        _isConnected.value = false
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

    }

    fun popReceivedMessagesQueue() {
        if (receivedMessagesQueue.isNotEmpty()) {
            receivedMessagesQueue.remove()
            viewModelScope.launch(Dispatchers.Default) {
                receivedMessagesQueue.peek()?.let {
                    _conversations.postValue(
                        conversationsRepository.rebuildConversations(it)
                    )
                }
            }
        }
    }

    override fun onReceiveRecipientMessageStatus(chatRef: String, messageStatus: MessageStatus) {
        when (messageStatus) {
            MessageStatus.TYPING -> {
                _isUserTyping.value = chatRef to true
            }
            else -> _isUserTyping.value = chatRef to false
        }
    }

    override fun onReceive(message: Message) {
        _isUserTyping.value = message.chatReference to false
        viewModelScope.launch(Dispatchers.Default) {
            if (receivedMessagesQueue.isEmpty()) {
                receivedMessagesQueue.add(message)
                _conversations.postValue(
                    conversationsRepository.rebuildConversations(message)
                )
            } else {
                receivedMessagesQueue.add(message)
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        conversationsRepository.cancel()
    }
}

class ConversationsViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ConversationsRepository(
            AddNewChatUsecase(ConversationsService(client)),
            createConversationsUsecase = CreateConversationsUsecase(ChatApiService(client)),
            ConversationDatabase.get(context),
            ChatDatabase.get(context)
        ).apply {
            session.appWideChatService?.setListener(this)
        }
        return ConversationsViewModel(repo).apply {
            repo.setListener(this)
        } as T
    }
}
package com.simulatedtez.gochat.conversations.view_model

import ChatServiceErrorResponse
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toUIMessage
import com.simulatedtez.gochat.chat.remote.models.toUIMessages
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.interfaces.ConversationEventListener
import com.simulatedtez.gochat.conversations.models.Conversation
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Response

class ConversationsViewModel(
    private val conversationsRepository: ConversationsRepository
): ViewModel(), ConversationEventListener {

    private val _waiting = MutableLiveData<Boolean>()
    val waiting: LiveData<Boolean> = _waiting

    private val _newConversation = MutableLiveData<Conversation>()
    val newConversation: LiveData<Conversation> = _newConversation

    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage


    private val _newMessages = MutableStateFlow<HashSet<UIMessage>>(hashSetOf())
    val newMessages: StateFlow<HashSet<UIMessage>> = _newMessages

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    init {
        fetchConversations()
    }

    fun fetchConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            _conversations.postValue(conversationsRepository.getConversations())
        }
    }

    fun addNewConversation(user: String, other: String) {
        _waiting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.addNewChat(user, other)
        }
    }

    override fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>) {
        _waiting.value = false
        if (error.response?.statusCode == HttpStatusCode.NotFound.value) {
            _errorMessage.value = error.response.message
        }
    }

    override fun onNewChatAdded(chat: NewChatResponse) {
        _newConversation.value = Conversation(
            other = chat.other,
            chatReference = chat.chatReference
        )
        _waiting.value = false
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
}

class ConversationsViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ConversationsRepository(
            AddNewChatUsecase(ConversationsService(client)),
            createConversationsUsecase = CreateConversationsUsecase(ChatApiService(client)),
            ConversationDatabase.get(context),
            ChatDatabase.get(context)
        )
        return ConversationsViewModel(repo).apply {
            repo.setListener(this)
        } as T
    }
}
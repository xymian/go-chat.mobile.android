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
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.models.UIMessage
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toUIMessage
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.DBConversation
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

class ConversationsViewModel(
    private val conversationsRepository: ConversationsRepository
): ViewModel(), ConversationEventListener {

    private val _tokenExpired = MutableLiveData<Boolean>()
    val tokenExpired: LiveData<Boolean> = _tokenExpired

    private val _waiting = MutableLiveData<Boolean>()
    val waiting: LiveData<Boolean> = _waiting

    private val _newConversation = MutableLiveData<DBConversation?>()
    val newConversation: LiveData<DBConversation?> = _newConversation

    private val _conversations = MutableLiveData<List<DBConversation>>()
    val conversations: LiveData<List<DBConversation>> = _conversations

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _newMessage = Channel<UIMessage>(Channel.UNLIMITED)
    val newMessage = _newMessage.receiveAsFlow()

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private var holdingConversations = mutableListOf<DBConversation>()

    fun resetTokenExpired() {
        _tokenExpired.value = false
    }

    fun fetchConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            holdingConversations = conversationsRepository.getConversations().toMutableList()
            _conversations.postValue(holdingConversations)
        }
    }

    fun rebuildConversations(newMessages: List<UIMessage>) {
        val tempConversations = mutableListOf<DBConversation>()
        val mutableMessages = mutableListOf<UIMessage>().apply {
            addAll(newMessages)
        }
        val updatedConversations = mutableListOf<DBConversation>()
        holdingConversations.forEach { convo ->
            val messages = mutableMessages.filter {
                it.message.chatReference == convo.chatReference
            }.sortedBy { it.message.timestamp }
            if (messages.isNotEmpty()) {
                mutableMessages.removeAll(messages)
                val conversation = DBConversation(
                    otherUser = convo.otherUser,
                    chatReference = convo.chatReference,
                    lastMessage = messages.last().message.message,
                    timestamp = messages.last().message.timestamp,
                    unreadCount = convo.unreadCount + messages.size
                )
                tempConversations.add(conversation)
                updatedConversations.add(conversation)
            } else {
                tempConversations.add(convo)
            }

            if (updatedConversations.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    conversationsRepository.storeConversations(updatedConversations)
                }
            }

            newConversations(mutableMessages).forEach { _, conversation ->
                addNewConversation(conversation.other, conversation.unreadCount)
            }
            holdingConversations = tempConversations
            _conversations.value = tempConversations
        }
    }

    private fun newConversations(mutableMessages: MutableList<UIMessage>): MutableMap<String, Conversation> {
        val chatMap = mutableMapOf<String, Conversation>()
        mutableMessages.forEach {
            if (chatMap[it.message.chatReference] == null) {
                chatMap[it.message.chatReference] = Conversation(
                    other = it.message.sender,
                    chatReference = it.message.chatReference,
                    lastMessage = it.message.message,
                    timestamp = it.message.timestamp,
                    unreadCount = 1
                )
            } else {
                chatMap[it.message.chatReference]?.apply {
                    unreadCount += 1
                    lastMessage = it.message.message
                    timestamp = it.message.timestamp
                }
            }
        }
        return chatMap
    }

    fun addNewConversation(other: String, messageCount: Int) {
        _waiting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.addNewChat(session.username, other, messageCount) { isAdded ->
                if (isAdded) {
                    viewModelScope.launch(Dispatchers.IO) {
                        conversationsRepository.connectToChatService()
                    }
                }
            }
        }
    }

    fun resetAddConversation() {
        _newConversation.value = null
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    fun postNewPresenceStatus(presenceStatus: PresenceStatus) {
        conversationsRepository.postNewUserPresence(presenceStatus)
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
        holdingConversations.add(newConversation)
        _newConversation.value = newConversation
        _waiting.value = false
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

    override suspend fun onReceive(message: Message) {
        viewModelScope.launch(Dispatchers.Main) {
            rebuildConversations(listOf(message.toUIMessage(true)))
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
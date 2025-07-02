package com.simulatedtez.gochat.chat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.repository.ChatEventListener
import com.simulatedtez.gochat.chat.repository.ChatRepository
import com.simulatedtez.gochat.chat.view_model.models.ChatInfo
import com.simulatedtez.gochat.chat.view_model.models.ChatPage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel(
    chatInfo: ChatInfo,
    chatDb: IChatStorage,
    getMissingMessagesUsecase: GetMissingMessagesUsecase,
    acknowledgeMessagesUsecase: AcknowledgeMessagesUsecase,
): ViewModel(), ChatEventListener {

    private val chatRepo: ChatRepository = ChatRepository(
        chatInfo,
        getMissingMessagesUsecase,
        acknowledgeMessagesUsecase,
        chatDb = chatDb,
        this
    )

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
}
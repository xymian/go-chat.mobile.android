package com.simulatedtez.gochat.chat.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.repository.ChatEventListener
import com.simulatedtez.gochat.chat.repository.ChatRepository
import com.simulatedtez.gochat.chat.view_model.models.ChatPage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel(private val chatRepo: ChatRepository): ViewModel(), ChatEventListener {

    init {
        chatRepo.setChatEventListener(this)
    }

    private val _pagedMessages = MutableLiveData<ChatPage>()
    val pagedMessages: LiveData<ChatPage> = _pagedMessages

    private val _newMessage = MutableLiveData<Message>()
    val newMessage: LiveData<Message> = _newMessage

    private val _newMessages = MutableLiveData<List<Message>>()
    val newMessages: LiveData<List<Message>> = _newMessages

    fun loadMessages(page: Int, size: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.value = chatRepo.loadMessages(page, size)
        }
    }

    fun setChatUsername(username: String) {
        chatRepo
    }

    override fun onSend() {

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
            _newMessages.value = messages
        }
    }

    override fun onNewMessage(message: Message) {
        viewModelScope.launch {
            _newMessage.value = message
        }
    }
}
package com.simulatedtez.gochat.conversations.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.models.Conversation
import com.simulatedtez.gochat.conversations.remote.api_services.ConversationsService
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatUsecase
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.conversations.repository.ConversationsListener
import com.simulatedtez.gochat.conversations.repository.ConversationsRepository
import com.simulatedtez.gochat.remote.client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val conversationsRepository: ConversationsRepository
): ViewModel(), ConversationsListener {

    private val _waiting = MutableLiveData<Boolean>()
    val waiting: LiveData<Boolean> = _waiting

    private val _newConversation = MutableLiveData<Conversation>()
    val newConversation: LiveData<Conversation> = _newConversation

    fun addNewConversation(user: String, other: String) {
        _waiting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.addNewChat(user, other)
        }
    }

    override fun onAddNewChatFailed(error: String) {
        _waiting.value = false
    }

    override fun onNewChatAdded(chat: NewChatResponse) {
        _newConversation.value = Conversation(
            me = "me",
            other = chat.other,
            id = 1L,
            chatReference = chat.chatReference
        )
        _waiting.value = false
    }
}

class ConversationsViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ConversationsRepository(
            StartNewChatUsecase(ConversationsService(client)),
            ConversationDatabase.get(context)
        )
        return ConversationsViewModel(repo).apply {
            repo.setListener(this)
        } as T
    }
}
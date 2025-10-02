package com.simulatedtez.gochat.view_model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.conversations.interfaces.ConversationEventListener
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.client
import com.simulatedtez.gochat.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

open class AppViewModel(
    private val appRepository: AppRepository
): ViewModel() {

    fun connectToChatService() {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.connectToChatService()
        }
    }

    fun postNewPresenceStatus(presenceStatus: PresenceStatus) {
        appRepository.userPresenceHelper.postNewUserPresence(presenceStatus)
    }

    override fun onCleared() {
        viewModelScope.cancel()
        appRepository.cancel()
    }
}

class AppViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = AppRepository(
            createConversationsUsecase = CreateConversationsUsecase(ChatApiService(client)),
            ChatDatabase.get(context)
        ).apply {
            session.setupAppWideChatService(this)
        }
        return AppViewModel(repo) as T
    }
}
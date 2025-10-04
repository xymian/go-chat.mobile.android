package com.simulatedtez.gochat.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.database.ChatDatabase
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.client
import com.simulatedtez.gochat.utils.AppWideChatEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

open class AppViewModel(
    private val chatEngineEventListener: AppWideChatEventListener
): ViewModel() {

    fun connectToChatService() {
        viewModelScope.launch(Dispatchers.IO) {
            chatEngineEventListener.connectToChatService()
        }
    }

    fun postNewPresenceStatus(presenceStatus: PresenceStatus) {
        chatEngineEventListener.userPresenceHelper.postNewUserPresence(presenceStatus)
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

class AppViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(AppWideChatEventListener.get(context)) as T
    }
}
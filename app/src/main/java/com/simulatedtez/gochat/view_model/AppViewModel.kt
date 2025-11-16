package com.simulatedtez.gochat.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.util.AppWideChatEventListener
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
        chatEngineEventListener.cancel()
        viewModelScope.cancel()
    }
}

class AppViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(AppWideChatEventListener.get(context)) as T
    }
}
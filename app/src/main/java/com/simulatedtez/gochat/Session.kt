package com.simulatedtez.gochat

import ChatEngine
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.utils.newAppWideChatService
import listeners.ChatEngineEventListener

open class Session private constructor() {

    var username: String = ""
        private set
    var accessToken: String = ""
        private set
    var lastActiveChat: ChatInfo? = null
        private set

    var appWideChatService: ChatEngine<Message>? = null
        private set

    var isReadReceiptEnabled: Boolean = false
    private set

    init {
        isReadReceiptEnabled = UserPreference.isReadReceiptEnabled()
    }

    companion object {
        var session = object: Session() {}
        private set

        fun clear() {
            session = object: Session() {}
        }
    }

    fun toggleReadReceipt(isEnabled: Boolean) {
        UserPreference.readReceiptToggle(isEnabled)
        isReadReceiptEnabled = isEnabled
    }

    fun setupAppWideChatService(eventListener: ChatEngineEventListener<Message>) {
        if (appWideChatService == null) {
            appWideChatService = newAppWideChatService(session.username, eventListener)
        }
    }

    fun setActiveChat(chatInfo: ChatInfo) {
        lastActiveChat = chatInfo
    }

    fun saveAccessToken(token: String) {
        accessToken = token
    }

    fun saveUsername(username: String) {
        this.username = username
    }
}
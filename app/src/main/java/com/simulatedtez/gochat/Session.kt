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
    var activeChat: ChatInfo? = null
        private set

    var appWideChatService: ChatEngine<Message>? = null
        private set

    companion object {
        var session = object: Session() {

        }
        private set

        fun clear() {
            session = object: Session() {

            }
        }
    }

    fun setupAppWideChatService(eventListener: ChatEngineEventListener<Message>) {
        if (appWideChatService == null) {
            appWideChatService = newAppWideChatService(session.username, eventListener)
        }
    }

    fun setActiveChat(chatInfo: ChatInfo) {
        activeChat = chatInfo
    }

    fun saveAccessToken(token: String) {
        accessToken = token
    }

    fun saveUsername(username: String) {
        this.username = username
    }
}
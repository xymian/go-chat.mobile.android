package com.simulatedtez.gochat.chat.repository

import com.simulatedtez.gochat.chat.remote.models.Message

interface ChatEventListener {
    fun onSend()
    fun onConnect()
    fun onDisconnect()
    fun onError(message: String)
    fun onNewMessages(messages: List<Message>)
    fun onNewMessage(message: Message)
}
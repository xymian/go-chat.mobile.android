package com.simulatedtez.gochat.chat.interfaces

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import okhttp3.Response

interface ChatEventListener: SocketConnection, MessageSender, MessageReceiver

interface SocketConnection {
    fun onClose(code: Int, reason: String)
    fun onConnect()
    fun onDisconnect(t: Throwable, response: Response?)
    fun onError(error: ChatServiceErrorResponse)
}

interface MessageSender {
    fun onMessageSent(message: Message)
    fun onSend(message: Message)
}

interface MessageReceiver {
    fun onNewMessages(messages: List<Message>) {}
    fun onNewMessage(message: Message)
}
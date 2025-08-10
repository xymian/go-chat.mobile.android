package com.simulatedtez.gochat.chat.interfaces

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import okhttp3.Response

interface ChatEventListener: SocketConnection, MessageSender, MessageReceiver {
    fun onConflictingMessagesDetected(messages: List<Message>)
}

interface SocketConnection {
    fun onClose(code: Int, reason: String)
    fun onConnect()
    fun onDisconnect(t: Throwable, response: Response?)
    fun onError(error: ChatServiceErrorResponse)
}

interface MessageSender {
    fun onMessageDelivered(message: Message)
    fun onMessagesSent(messages: List<Message>)
    fun onMessageSent(message: Message)
    fun onSend(message: Message)
}

interface MessageReceiver {
    fun onNewMessages(messages: List<Message>)
    fun onNewMessage(message: Message)
}
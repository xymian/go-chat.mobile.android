package com.simulatedtez.gochat.chat.interfaces

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import okhttp3.Response

interface ChatEventListener: SocketConnection, MessageSender, MessageReceiver {
    fun onReceiveRecipientMessageStatus(message: Message)
    suspend fun onMessageSent(message: Message)
}

interface SocketConnection {
    fun onClose(code: Int, reason: String)
    fun onConnect()
    fun onDisconnect(t: Throwable, response: Response?)
    fun onError(error: ChatServiceErrorResponse)
}

interface MessageSender {
    fun onPresencePosted()
    fun onSend(message: Message) {}
}

interface MessageReceiver {
    fun onReceiveRecipientActivityStatusMessage(message: Message)
    suspend fun onReceive(message: Message)
}
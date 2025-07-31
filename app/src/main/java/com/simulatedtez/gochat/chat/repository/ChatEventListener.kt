package com.simulatedtez.gochat.chat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import okhttp3.Response

interface ChatEventListener {
    fun onMessageDelivered(message: Message)
    fun onMessageSent(message: Message)
    fun onClose(code: Int, reason: String)
    fun onSend(message: Message)
    fun onConnect()
    fun onDisconnect(t: Throwable, response: Response?)
    fun onError(error: ChatServiceErrorResponse)
    fun onNewMessages(messages: List<Message>)
    fun onNewMessage(message: Message)
    fun onConflictingMessagesDetected(messages: List<Message>)
    fun onMessagesSent(messages: List<Message>)
}
package com.simulatedtez.gochat.chat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.chat.remote.models.Message
import okhttp3.Response

interface ChatEventListener {
    fun onClose(code: Int, reason: String)
    fun onSend(message: Message)
    fun onConnect()
    fun onDisconnect(t: Throwable, response: Response?)
    fun onError(error: ChatServiceErrorResponse)
    fun onNewMessages(messages: List<Message>)
    fun onNewMessage(message: Message)
}
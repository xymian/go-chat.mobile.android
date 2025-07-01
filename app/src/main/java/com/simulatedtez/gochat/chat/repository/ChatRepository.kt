package com.simulatedtez.gochat.chat.repository

import ChatServiceError
import ChatServiceManager
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.view_model.models.ChatInfo
import com.simulatedtez.gochat.chat.view_model.models.ChatPage
import listeners.ChatServiceListener

class ChatRepository(
    chatInfo: ChatInfo,
    getMissingMessagesUsecase: GetMissingMessagesUsecase,
    acknowledgeMessagesUsecase: AcknowledgeMessagesUsecase,
    private val chatDb: IChatStorage,
    private val chatEventListener: ChatEventListener
): ChatServiceListener<Message> {

    private val chatService = ChatServiceManager.Builder<Message>()
        .setSocketURL(chatInfo.socketURL)
        .setUsername(chatInfo.username)
        .setExpectedReceivers(chatInfo.recipientsUsernames)
        .setMissingMessagesCaller(getMissingMessagesUsecase)
        .setMessageAckCaller(acknowledgeMessagesUsecase)
        .setStorageInterface(chatDb)
        .setChatServiceListener(this)
        .build(Message.serializer())

    init {
        chatService.connect()
    }

    fun sendMessage(message: Message) {
        chatService.sendMessage(message)
    }

    suspend fun loadMessages(page: Int): ChatPage {
        val messages = chatDb.loadMessages(page = page)
        return ChatPage(
            messages = messages,
            page = page,
            size =messages.size
        )
    }

    override fun onConnect() {
        chatEventListener.onConnect()
    }

    override fun onDisconnect() {
        chatEventListener.onDisconnect()
    }

    override fun onError(error: ChatServiceError, message: String) {
        chatEventListener.onError("error message: $message, error type: ${error.name}")
    }

    override fun onSend(message: Message) {
        chatEventListener.onSend(message)
    }

    override fun onReceive(messages: List<Message>) {
        chatEventListener.onNewMessages(messages)
    }

    override fun onReceive(message: Message) {
        chatEventListener.onNewMessage(message)
    }
}
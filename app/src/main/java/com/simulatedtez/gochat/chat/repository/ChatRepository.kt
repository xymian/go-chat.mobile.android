package com.simulatedtez.gochat.chat.repository

import ChatServiceError
import ChatServiceManager
import ILocalStorage
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.view_model.models.ChatPage
import listeners.ChatServiceListener

class ChatRepository(
    getMissingMessagesUsecase: GetMissingMessagesUsecase,
    acknowledgeMessagesUsecase: AcknowledgeMessagesUsecase,
    private val database: IChatStorage
): ChatServiceListener<Message> {

    private val chatService = ChatServiceManager.Builder<Message>()
        .setSocketURL("")
        .setUsername("")
        .setMissingMessagesCaller(getMissingMessagesUsecase)
        .setMessageAckCaller(acknowledgeMessagesUsecase)
        .setExpectedReceivers(listOf())
        .setChatServiceListener(this)
        .build(Message.serializer())

    init {
        chatService.connect()
    }

    fun sendMessage() {

    }

    fun getMissingMessages() {

    }

    fun acknowledgeMessagesByTimestampRange(from: String, to: String) {

    }

    suspend fun loadMessages(page: Int, size: Int): ChatPage {
        val messages = database.loadMessages(page = page, size = size)
        return ChatPage(
            messages = messages,
            page = page,
            size = size
        )
    }

    override fun onConnect() {
        TODO("Not yet implemented")
    }

    override fun onDisconnect() {
        TODO("Not yet implemented")
    }

    override fun onError(error: ChatServiceError, message: String) {
        TODO("Not yet implemented")
    }

    override fun onSend(message: Message) {
        TODO("Not yet implemented")
    }

    override fun onReceive(messages: List<Message>) {
        TODO("Not yet implemented")
    }

    override fun onReceive(message: Message) {
        TODO("Not yet implemented")
    }
}
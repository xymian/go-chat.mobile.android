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
    private val chatInfo: ChatInfo,
    getMissingMessagesUsecase: GetMissingMessagesUsecase,
    acknowledgeMessagesUsecase: AcknowledgeMessagesUsecase,
    private val database: IChatStorage
): ChatServiceListener<Message> {

    private val chatService = ChatServiceManager.Builder<Message>()
        .setSocketURL("ws://<host>:<port>/room/${chatInfo.chatReference}" +
                "?me=${chatInfo.username}&other=${chatInfo.recipientsUsernames[0]}")
        .setUsername(chatInfo.username)
        .setExpectedReceivers(chatInfo.recipientsUsernames)
        .setMissingMessagesCaller(getMissingMessagesUsecase)
        .setMessageAckCaller(acknowledgeMessagesUsecase)
        .setChatServiceListener(this)
        .build(Message.serializer())

    private var chatEventListener: ChatEventListener? = null

    init {
        chatService.connect()
    }

    fun sendMessage() {

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
        chatEventListener?.onConnect()
    }

    override fun onDisconnect() {
        chatEventListener?.onDisconnect()
    }

    override fun onError(error: ChatServiceError, message: String) {
        chatEventListener?.onError("error message: $message, error type: ${error.name}")
    }

    override fun onSend(message: Message) {
        chatEventListener?.onSend()
    }

    override fun onReceive(messages: List<Message>) {
        chatEventListener?.onNewMessages(messages)
    }

    override fun onReceive(message: Message) {
        chatEventListener?.onNewMessage(message)
    }

    fun setChatEventListener(chatEventListener: ChatEventListener) {
        this.chatEventListener = chatEventListener
    }
}
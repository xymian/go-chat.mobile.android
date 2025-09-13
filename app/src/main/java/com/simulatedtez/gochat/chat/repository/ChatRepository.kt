package com.simulatedtez.gochat.chat.repository

import ChatServiceErrorResponse
import ChatServiceManager
import SocketMessageReturner
import SocketMessageReturnerListener
import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.chat.database.DBMessage
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.database.toMessages
import com.simulatedtez.gochat.chat.database.toUIMessages
import com.simulatedtez.gochat.chat.interfaces.ChatEventListener
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.ChatPage
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.chat.remote.models.toDBMessage
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.utils.toISOString
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatServiceListener
import okhttp3.Response
import java.time.LocalDateTime
import java.util.LinkedList

class ChatRepository(
    private val chatInfo: ChatInfo,
    private val createChatRoomUsecase: CreateChatRoomUsecase,
    private val chatDb: IChatStorage,
    private val conversationDB: ConversationDatabase
): ChatServiceListener<Message> {

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var timesPaginated = 0
    private var isNewChat = UserPreference.isNewChatHistory(chatInfo.chatReference)

    private var chatEventListener: ChatEventListener? = null
    private var chatService = ChatServiceManager.Builder<Message>()
        .setSocketURL(
            "${BuildConfig.WEBSOCKET_BASE_URL}/room/${chatInfo.chatReference}" +
                    "?me=${chatInfo.username}&other=${chatInfo.recipientsUsernames[0]}"
        )
        .setUsername(chatInfo.username)
        .setTimestampFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .setExpectedReceivers(chatInfo.recipientsUsernames)
        .setStorageInterface(chatDb)
        .setChatServiceListener(this)
        .setMessageReturner(socketMessageLabeler(), listener = object: SocketMessageReturnerListener<Message> {
            override fun onReturn(message: Message) {
                chatEventListener?.onMessageSent(message)
            }
        })
        .build(Message.serializer())

    val cutOffForMarkingMessagesAsSeen = UserPreference.getCutOffDateForMarkingMessagesAsSeen()

    private var rushedIncomingMessages: LinkedList<Message> = LinkedList()
    private var rushedOutgoingMessages: LinkedList<Message> = LinkedList()
    
    fun getNextOutgoingMessage(): Message? {
        return if (rushedOutgoingMessages.isNotEmpty()) rushedOutgoingMessages.remove()
        else null
    }

    fun getNextMessageFromRecipient(): Message? {
        return if (rushedIncomingMessages.isNotEmpty()) rushedIncomingMessages.remove()
        else null
    }

    private fun socketMessageLabeler(): SocketMessageReturner<Message> =
        object : SocketMessageReturner<Message> {
            override fun returnMessage(
                message: Message
            ): Message {
                return Message(
                    id = message.id,
                    message = message.message,
                    sender = message.sender,
                    receiver = message.receiver,
                    timestamp = message.timestamp,
                    chatReference = message.chatReference,
                    ack = true,
                    deliveredTimestamp = LocalDateTime.now().toISOString(),
                    seenTimestamp = message.seenTimestamp
                )
            }

            override fun isReturnableSocketMessage(message: Message): Boolean {
                return message.sender != chatInfo.username && message.deliveredTimestamp == null
            }
        }

    fun connectToChatService() {
        context.launch(Dispatchers.IO) {
            createNewChatRoom {
                chatService.connect()
            }
        }
    }

    fun connectAndSendPendingMessages() {
        context.launch(Dispatchers.IO) {
            val pendingMessages = mutableListOf<DBMessage>()
            pendingMessages.addAll(chatDb.getUndeliveredMessages(chatInfo.username, chatInfo.chatReference))
            pendingMessages.addAll(chatDb.getPendingMessages(chatInfo.chatReference))
            context.launch(Dispatchers.Main) {
                createNewChatRoom {
                    chatService.connectAndSend(pendingMessages.toMessages())
                }
            }
        }
    }

    fun killChatService() {
        chatService.disconnect()
        chatService = ChatServiceManager.Builder<Message>()
            .build(Message.serializer())
    }

    fun setChatEventListener(listener: ChatEventListener) {
        chatEventListener = listener
    }

    fun markMessagesAsSeen(messages: List<Message>) {
        messages.forEach {
            if (it.timestamp > cutOffForMarkingMessagesAsSeen!!) {
                it.seenTimestamp = LocalDateTime.now().toISOString()
                chatService.returnMessage(it)
            }
        }
    }

    fun sendMessage(message: Message) {
        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
        chatService.sendMessage(message)
    }

    suspend fun markConversationAsOpened() {
        conversationDB.updateUnreadCountToZero(chatInfo.chatReference)
    }

    suspend fun loadNextPageMessages(): ChatPage {
        val messages = chatDb.loadNextPage(chatInfo.chatReference)
        if (messages.isNotEmpty()) {
            timesPaginated++
        }
        return ChatPage(
            messages = messages.toUIMessages(),
            paginationCount = timesPaginated,
            size = messages.size
        )
    }

    private suspend fun createNewChatRoom(onSuccess: (() -> Unit)) {
        val params = CreateChatRoomParams(
            request = CreateChatRoomParams.Request(
                user = chatInfo.username,
                other = chatInfo.recipientsUsernames[0],
                chatReference = chatInfo.chatReference
            )
        )
        createChatRoomUsecase.call(
            params = params, object: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when(response) {
                        is IResponse.Success -> {
                            onSuccess()
                        }
                        is IResponse.Failure -> {
                            Napier.d(response.response?.message ?: "unknown")
                        }
                        else -> {

                        }
                    }
                }
            }
        )
    }

    override fun onClose(code: Int, reason: String) {
        chatEventListener?.onClose(code, reason)
    }

    override fun onConnect() {
        chatEventListener?.onConnect()
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        when {
            response?.code == HttpStatusCode.NotFound.value -> {
                context.launch(Dispatchers.IO) {
                    createNewChatRoom {
                        chatService.connect()
                    }
                }
            } else -> {
                Napier.d(response?.message ?: "unknown")
                chatEventListener?.onDisconnect(t, response)
            }
        }
    }

    override fun onError(response: ChatServiceErrorResponse) {
        chatEventListener?.onError(response)
    }

    override fun onSent(message: Message) {
        val dbMessage = message.toDBMessage()
        context.launch(Dispatchers.IO) {
            chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
        }

        queueUpOutgoingMessage(message) { topMessage ->
            chatEventListener?.onMessageSent(topMessage)
        }
    }

    private fun queueUpOutgoingMessage(
        message: Message, onQueueEmpty: (topMessage: Message) -> Unit
    ) {
        var index = -1
        rushedOutgoingMessages.filterIndexed { i, msg ->
            val isTheSame = msg.id == message.id
            if (isTheSame) {
                index = i
                true
            } else {
                false
            }
        }
        if (index > -1) {
            rushedOutgoingMessages[index] = message
        } else {
            rushedOutgoingMessages.add(message)
        }
        val topMessage = rushedOutgoingMessages.remove()
        if (rushedOutgoingMessages.isEmpty()) {
            onQueueEmpty(topMessage)
        }
    }

    override fun onReceive(message: Message) {
        if (!isNewChat) {
            queueUpIncomingMessage(message) { topMessage ->
                chatEventListener?.queueMessage(topMessage)
            }
        } else {
            UserPreference.storeChatHistoryStatus(
                chatInfo.chatReference, false)
            isNewChat = false
            queueUpIncomingMessage(message) { topMessage ->
                context.launch(Dispatchers.Default) {
                    chatEventListener?.queueMessage(topMessage)
                }
            }
        }
    }

    private fun queueUpIncomingMessage(
        message: Message, onQueueEmpty: (topMessage: Message) -> Unit
    ) {
        var index = -1
        rushedIncomingMessages.filterIndexed { i, msg ->
            val isTheSame = msg.id == message.id
            if (isTheSame) {
                index = i
                true
            } else {
                false
            }
        }
        if (index > -1) {
            rushedIncomingMessages[index] = message
        } else {
            rushedIncomingMessages.add(message)
        }
        val topMessage = rushedIncomingMessages.remove()
        if (rushedIncomingMessages.isEmpty()) {
            onQueueEmpty(topMessage)
        }
    }

    fun isChatServiceConnected(): Boolean {
        return chatService.socketIsConnected
    }

    fun cancel() {
        context.cancel()
    }
}
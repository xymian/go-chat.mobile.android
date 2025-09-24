package com.simulatedtez.gochat.chat.repository

import ChatServiceErrorResponse
import ChatServiceManager
import SocketMessageReturner
import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.UserPreference
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
import java.util.UUID

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
        .setChatServiceListener(this)
        .setMessageReturner(socketMessageLabeler())
        .build(Message.serializer())

    val cutOffForMarkingMessagesAsSeen = UserPreference.getCutOffDateForMarkingMessagesAsSeen()

    private fun socketMessageLabeler(): SocketMessageReturner<Message> {
        return object : SocketMessageReturner<Message> {
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
            val pendingMessages = mutableListOf<Message>()

            val status = Message(
                id = UUID.randomUUID().toString(),
                message = "",
                sender = chatInfo.username,
                receiver = chatInfo.recipientsUsernames[0],
                timestamp = LocalDateTime.now().toISOString(),
                chatReference = chatInfo.chatReference,
                ack = false,
                presenceStatus = "ONLINE"
            )

            pendingMessages.add(status)
            pendingMessages.addAll(chatDb.getUndeliveredMessages(
                chatInfo.username, chatInfo.chatReference).toMessages()
            )
            pendingMessages.addAll(
                chatDb.getPendingMessages(chatInfo.chatReference).toMessages()
            )
            context.launch(Dispatchers.Main) {
                createNewChatRoom {
                    chatService.connectAndSend(pendingMessages)
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
            updateConversationLastMessage(message)
        }
        chatService.sendMessage(message)
    }

    fun postPresence(message: Message) {
        if (!message.presenceStatus.isNullOrEmpty()) {
            chatService.sendMessage(message)
        }
    }

    suspend fun updateConversationLastMessage(message: Message) {
        conversationDB.updateConversationLastMessage(message)
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
        if (!message.presenceStatus.isNullOrEmpty()) {
            context.launch(Dispatchers.IO) {
                chatDb.store(message)
                val dbMessage = message.toDBMessage()
                chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
            }
            context.launch(Dispatchers.IO) {
                chatEventListener?.onMessageSent(message)
            }
        } else {
            chatEventListener?.onPresencePosted()
        }
    }

    private var lastMessagesFromRecipient = mutableListOf<Message>()

    override fun onReceive(message: Message) {
        if (!message.presenceStatus.isNullOrEmpty()) {
            context.launch(Dispatchers.Main) {
                chatEventListener?.onReceiveRecipientActivityStatusMessage(message)
            }
            return
        }

        if (!message.messageStatus.isNullOrEmpty()) {
            context.launch(Dispatchers.Main) {
                chatEventListener?.onReceiveRecipientMessageStatus(message)
            }
            return
        }

        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
        val lastMessageOfTheSameId = lastMessagesFromRecipient.find { it.id == message.id }
        if (lastMessageOfTheSameId == null) {
            message.deliveredTimestamp = LocalDateTime.now().toISOString()
            lastMessagesFromRecipient.add(message)
        } else {
            if (message.deliveredTimestamp.isNullOrEmpty()) {
                message.deliveredTimestamp = lastMessageOfTheSameId.deliveredTimestamp
                lastMessagesFromRecipient.remove(lastMessageOfTheSameId)
            }
        }
        if (!isNewChat) {
            context.launch(Dispatchers.IO) {
                updateConversationLastMessage(message)
                chatEventListener?.onReceive(message)
            }
        } else {
            UserPreference.storeChatHistoryStatus(chatInfo.chatReference, false)
            isNewChat = false
            if (message.seenTimestamp.isNullOrEmpty()) {
                context.launch(Dispatchers.IO) {
                    updateConversationLastMessage(message)
                    chatEventListener?.onReceive(message)
                }
            }
        }
    }

    fun isChatServiceConnected(): Boolean {
        return chatService.socketIsConnected
    }

    fun cancel() {
        context.cancel()
    }
}
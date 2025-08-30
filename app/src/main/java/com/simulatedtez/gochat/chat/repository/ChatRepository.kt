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
                updateMessage(message)
            }
        })
        .build(Message.serializer())

    val cutOffForMarkingMessagesAsSeen = UserPreference.getCutOffDateForMarkingMessagesAsSeen()

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

    private fun updateMessage(message: Message) {
        chatEventListener?.onMessageStatusUpdated(message)
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
        if (!message.deliveredTimestamp.isNullOrEmpty()) {
            chatEventListener?.onMessageStatusUpdated(message)
        } else {
            chatEventListener?.onMessageSent(message)
        }
    }

    override fun onReceive(message: Message) {
        if (!isNewChat) {
            chatEventListener?.onNewMessage(message)
        } else {
            UserPreference.storeChatHistoryStatus(
                chatInfo.chatReference, false)
            isNewChat = false
        }
    }

    fun isChatServiceConnected(): Boolean {
        return chatService.socketIsConnected
    }

    private suspend fun filterNonConflictingMessages(messages: List<Message>): List<Message> {
        val filteredMessages = mutableListOf<Message>()
        messages.forEach {
            val dbMessage = chatDb.getMessage(it.id)
            if (dbMessage == null){
                filteredMessages.add(it)
            }
        }
        return messages
    }

    fun cancel() {
        context.cancel()
    }
}
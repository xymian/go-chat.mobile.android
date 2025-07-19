package com.simulatedtez.gochat.chat.repository

import ChatServiceErrorResponse
import ChatServiceManager
import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.database.toMessages
import com.simulatedtez.gochat.chat.remote.api_usecases.AcknowledgeMessagesUsecase
import com.simulatedtez.gochat.chat.remote.api_usecases.GetMissingMessagesUsecase
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.models.ChatInfo
import com.simulatedtez.gochat.chat.models.ChatPage
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.chat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.chat.remote.models.toMessage_db
import com.simulatedtez.gochat.chat.remote.models.toMessages_db
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import listeners.ChatServiceListener
import okhttp3.Response

class ChatRepository(
    private val chatInfo: ChatInfo,
    private val createChatRoomUsecase: CreateChatRoomUsecase,
    getMissingMessagesUsecase: GetMissingMessagesUsecase,
    acknowledgeMessagesUsecase: AcknowledgeMessagesUsecase,
    private val chatDb: IChatStorage,
): ChatServiceListener<Message> {

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var chatEventListener: ChatEventListener? = null
    private var chatService = ChatServiceManager.Builder<Message>()
        .setSocketURL(
            "${BuildConfig.WEBSOCKET_BASE_URL}/room/${chatInfo.chatReference}" +
                    "?me=${chatInfo.username}&other=${chatInfo.recipientsUsernames[0]}"
        )
        .setUsername(chatInfo.username)
        .setTimestampFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .setExpectedReceivers(chatInfo.recipientsUsernames)
        .setMissingMessagesCaller(getMissingMessagesUsecase)
        .setMessageAckCaller(acknowledgeMessagesUsecase)
        .setStorageInterface(chatDb)
        .setChatServiceListener(this)
        .build(Message.serializer())

    private var timesPaginated = 0
    private var isNewChat = UserPreference.isNewChatHistory(chatInfo.chatReference)

    fun connectToChatService() {
        chatService.connect()
    }

    fun connectAndSendPendingMessages() {
        context.launch(Dispatchers.IO) {
            val pendingMessages = chatDb.getPendingMessages(chatInfo.chatReference)
            context.launch(Dispatchers.Main) {
                chatService.connectAndSend(pendingMessages.toMessages())
            }
        }
    }

    fun killChatService() {
        chatService.disconnect()
        chatService = ChatServiceManager.Builder<Message>()
            .build(Message.serializer())
    }

    fun pauseChatService() {
        chatService.pause()
    }

    fun resumeChatService() {
        chatService.resume()
    }

    fun setChatEventListener(listener: ChatEventListener) {
        chatEventListener = listener
    }

    suspend fun sendMessage(message: Message) {
        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
        chatService.sendMessage(message)
    }

    suspend fun loadNextPageMessages(): ChatPage {
        val messages = chatDb.loadNextPage(chatInfo.chatReference)
        if (messages.isNotEmpty()) {
            timesPaginated++
        }
        return ChatPage(
            messages = messages.toMessages(),
            paginationCount = timesPaginated,
            size = messages.size
        )
    }

    suspend fun createNewChatRoom(onSuccess: (() -> Unit)) {
        val params = CreateChatRoomParams(
            request = CreateChatRoomParams.Request(
                user = chatInfo.username,
                other = chatInfo.recipientsUsernames[0],
                chatReference = chatInfo.chatReference
            )
        )
        createChatRoomUsecase.call(
            params = params, object: IResponseHandler<String, IResponse<String>> {
                override fun onResponse(response: IResponse<String>) {
                    when(response) {
                        is IResponse.Success -> {
                            onSuccess()
                        }
                        is IResponse.Failure -> {
                            Napier.d(response.response ?: "unknown")
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

    override fun onReceive(messages: List<Message>) {
        if (!isNewChat) {
            context.launch(Dispatchers.IO) {
                val conflict = findMessageConflict(messages)
                if (conflict) {
                    context.launch(Dispatchers.Main) {
                        chatEventListener?.onConflictingMessagesDetected(messages)
                    }
                } else {
                    context.launch(Dispatchers.Main) {
                        chatEventListener?.onNewMessages(messages)
                    }
                }
            }
        } else {
            UserPreference.storeChatHistoryStatus(
                chatInfo.chatReference, false)
            isNewChat = false
            chatEventListener?.onNewMessages(listOf())
        }
    }

    override fun onRecipientMessagesAcknowledged(messages: List<Message>) {
        context.launch(Dispatchers.IO) {
            chatDb.setAsSeen(*(messages.toMessages_db().map {
                it.messageReference to it.chatReference
            }.toTypedArray()))
        }
    }

    override fun onSent(messages: List<Message>) {
        val dbMessages = messages.toMessages_db()
        context.launch(Dispatchers.IO) {
            chatDb.setAsSent(*(dbMessages.map {
                it.messageReference to it.chatReference
            }.toTypedArray()))
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

    private suspend fun findMessageConflict(messages: List<Message>): Boolean {
        messages.forEach {
            if (chatDb.getMessage(it.messageReference) != null) return true
        }
        return false
    }
}
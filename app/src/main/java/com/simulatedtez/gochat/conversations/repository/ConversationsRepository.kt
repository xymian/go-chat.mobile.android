package com.simulatedtez.gochat.conversations.repository

import ChatServiceErrorResponse
import SocketMessageReturner
import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toDBMessage
import com.simulatedtez.gochat.chat.remote.models.toDBMessages
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.DBConversation
import com.simulatedtez.gochat.conversations.interfaces.ConversationEventListener
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.AddNewChatUsecase
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.utils.toISOString
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatServiceListener
import java.time.LocalDateTime

class ConversationsRepository(
    private val addNewChatUsecase: AddNewChatUsecase,
    private val createConversationsUsecase: CreateConversationsUsecase,
    private val conversationDB: ConversationDatabase,
    private val chatDb: IChatStorage,
): ChatServiceListener<Message> {
    private var conversationEventListener: ConversationEventListener? = null

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var chatService = ChatServiceManager.Builder<Message>()
        .setSocketURL(
            "${BuildConfig.WEBSOCKET_BASE_URL}/interactions/${session.username}"
        )
        .setUsername(session.username)
        .setTimestampFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .setExpectedReceivers(listOf())
        .setStorageInterface(chatDb)
        .setChatServiceListener(this)
        .setMessageLabeler(socketMessageLabeler())
        .build(Message.serializer())

    private fun socketMessageLabeler(): SocketMessageReturner<Message> {
        return object : SocketMessageReturner<Message> {
            override fun returnMessage(
                message: Message
            ): Message {
                return Message(
                    id = message.id,
                    messageReference = message.messageReference,
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
                return message.sender != session.username && message.deliveredTimestamp == null
            }
        }
    }

    suspend fun connectToChatService() {
        createNewConversations {
            chatService.connect()
        }
    }

    fun killService() {
        chatService.disconnect()
    }

    fun setListener(listener: ConversationEventListener) {
        conversationEventListener = listener
    }

    suspend fun getConversations(): List<DBConversation> {
        return conversationDB.getConversations()
    }

    suspend fun createNewConversations(onSuccess: (() -> Unit)) {
        val params = CreateConversationsParams(
            request = CreateConversationsParams.Request(
                username = session.username
            )
        )
        createConversationsUsecase.call(
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

    suspend fun storeConversations(conversations: List<DBConversation>) {
        conversationDB.insertConversations(conversations)
    }

    suspend fun addNewChat(username: String, otherUser: String, messageCount: Int, completion: (isSuccess: Boolean) -> Unit) {
        val params = StartNewChatParams(
            request = StartNewChatParams.Request(
                user = username, other = otherUser
            )
        )
        addNewChatUsecase.call(
            params, object: IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
            override fun onResponse(response: IResponse<ParentResponse<NewChatResponse>>) {
               when (response) {
                   is IResponse.Success -> {
                       response.data.data?.let {
                           context.launch(Dispatchers.IO) {
                               conversationDB.insertConversation(
                                   DBConversation(
                                       otherUser = it.other,
                                       chatReference = it.chatReference,
                                       lastMessage = "",
                                       timestamp = "",
                                       unreadCount = messageCount,
                                       contactAvi = ""
                                   )
                               )
                           }
                           context.launch(Dispatchers.Main) {
                               completion(true)
                               conversationEventListener?.onNewChatAdded(it)
                           }
                       }
                   }
                   is IResponse.Failure -> {
                       context.launch(Dispatchers.Main) {
                           completion(false)
                           conversationEventListener?.onAddNewChatFailed(response)
                       }
                   }

                   is Response<*> -> {}
               }
            }
        })
    }

    override fun onClose(code: Int, reason: String) {
        conversationEventListener?.onClose(code, reason)
    }

    override fun onConnect() {
        conversationEventListener?.onConnect()
    }

    override fun onDisconnect(t: Throwable, response: okhttp3.Response?) {
        Napier.d(response?.message ?: "unknown")
        conversationEventListener?.onDisconnect(t, response)
    }

    override fun onError(response: ChatServiceErrorResponse) {
        conversationEventListener?.onError(response)
    }

    private fun isNewChat(chatRef: String): Boolean {
        return UserPreference.isNewChatHistory(chatRef)
    }

    override fun onReceive(messages: List<Message>) {
        if (messages.isNotEmpty()) {
            if (!isNewChat(messages[0].chatReference)) {
                context.launch(Dispatchers.IO) {
                    val filteredMessages = filterNonConflictingMessages(messages)
                    if (filteredMessages.isNotEmpty()) {
                        context.launch(Dispatchers.Main) {
                            conversationEventListener?.onNewMessages(filteredMessages)
                        }
                    }
                }
            } else {
                if (messages.isNotEmpty()) {
                    UserPreference.storeChatHistoryStatus(
                        messages[0].chatReference, false)
                    conversationEventListener?.onNewMessages(listOf())
                }
            }
        }
    }

    override fun onRecipientMessagesAcknowledged(messages: List<Message>) {
        context.launch(Dispatchers.IO) {
            chatDb.setAsSeen(*(messages.toDBMessages().map {
                it.messageReference to it.chatReference
            }.toTypedArray()))
        }
    }

    override fun onReturnMissingMessages(messages: List<Message>): List<Message> {
        return messages.map {
            Message(
                id = it.id,
                messageReference = it.messageReference,
                message = it.message,
                sender = it.sender,
                receiver = it.receiver,
                timestamp = it.timestamp,
                chatReference = it.chatReference,
                ack = true,
                deliveredTimestamp = LocalDateTime.now().toISOString(),
                seenTimestamp = it.seenTimestamp
            )
        }
    }

    override fun onSent(message: Message) {
        val dbMessage = message.toDBMessage()
        context.launch(Dispatchers.IO) {
            chatDb.setAsSent((dbMessage.messageReference to dbMessage.chatReference))
        }
    }

    /*** for testing purposes **/
    override fun returnMessage(message: Message, exportStatus: Boolean) {
        val m = message
    }

    override fun onReceive(message: Message) {
        if (!isNewChat(message.chatReference)) {
            conversationEventListener?.onNewMessage(message)
        } else {
            UserPreference.storeChatHistoryStatus(
                message.chatReference, false)
        }
    }

    private suspend fun filterNonConflictingMessages(messages: List<Message>): List<Message> {
        val filteredMessages = mutableListOf<Message>()
        messages.forEach {
            val dbMessage = chatDb.getMessage(it.messageReference)
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
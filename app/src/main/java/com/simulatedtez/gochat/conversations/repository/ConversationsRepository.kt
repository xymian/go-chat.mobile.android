package com.simulatedtez.gochat.conversations.repository

import ChatServiceErrorResponse
import SocketMessageReturner
import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toDBMessage
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
import okhttp3.Dispatcher
import java.time.LocalDateTime
import java.util.LinkedList

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
            "${BuildConfig.WEBSOCKET_BASE_URL}/conversations/${session.username}"
        )
        .setUsername(session.username)
        .setTimestampFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .setExpectedReceivers(listOf())
        .setStorageInterface(chatDb)
        .setChatServiceListener(this)
        .setMessageReturner(socketMessageLabeler(), null)
        .build(Message.serializer())

    private var rushedIncomingMessages: LinkedList<Message> = LinkedList()

    fun getNextMessageFromRecipient(): Message? {
        return if (rushedIncomingMessages.isNotEmpty()) rushedIncomingMessages.remove()
        else null
    }

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
            params = params, object: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when(response) {
                        is IResponse.Success -> {
                            onSuccess()
                        }
                        is IResponse.Failure -> {
                            Napier.d(response.response?.message ?: "unknown")
                            context.launch(Dispatchers.Main) {
                                conversationEventListener?.onError(response)
                            }
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

    suspend fun storeConversation(conversation: DBConversation) {
        conversationDB.insertConversation(conversation)
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

    override fun onReceive(message: Message) {
        if (!isNewChat(message.chatReference)) {
            queueUpIncomingMessage(message) { topMessage ->
                conversationEventListener?.queueMessage(topMessage)
            }
        } else {
            UserPreference.storeChatHistoryStatus(
                message.chatReference, false)
            queueUpIncomingMessage(message) { topMessage ->
                conversationEventListener?.queueMessage(topMessage)
            }
        }
    }

    override fun onSent(message: Message) {
        val dbMessage = message.toDBMessage()
        context.launch(Dispatchers.IO) {
            chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
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

    fun cancel() {
        context.cancel()
    }
}
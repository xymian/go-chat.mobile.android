package com.simulatedtez.gochat.conversations.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.remote.models.Message
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
import com.simulatedtez.gochat.repository.AppRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import java.util.UUID

class ConversationsRepository(
    private val addNewChatUsecase: AddNewChatUsecase,
    createConversationsUsecase: CreateConversationsUsecase,
    private val conversationDB: ConversationDatabase,
    chatDb: IChatStorage,
): AppRepository(createConversationsUsecase, chatDb), ChatEngineEventListener<Message> {

    override val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var conversationEventListener: ConversationEventListener? = null

    fun setListener(listener: ConversationEventListener) {
        conversationEventListener = listener
    }

    suspend fun getConversations(): List<DBConversation> {
        return conversationDB.getConversations()
    }

    override suspend fun createNewConversations(onSuccess: (() -> Unit)) {
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
                               storeConversation(
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
        userPresenceHelper.postNewUserPresence(PresenceStatus.AWAY)
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
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch(Dispatchers.Main) {
                userPresenceHelper.handlePresenceMessage(
                    it, message.id, message.chatReference
                ) {}
            }
            return
        }

        MessageStatus.getType(message.messageStatus)?.let {
            return
        }

        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
        if (!isNewChat(message.chatReference)) {
            context.launch(Dispatchers.IO) {
                conversationEventListener?.onReceive(message)
            }
        } else {
            UserPreference.storeChatHistoryStatus(
                message.chatReference, false)
            context.launch(Dispatchers.IO) {
                conversationEventListener?.onReceive(message)
            }
        }
    }
}
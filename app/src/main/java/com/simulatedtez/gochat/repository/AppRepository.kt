package com.simulatedtez.gochat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toDBMessage
import com.simulatedtez.gochat.conversations.models.Conversation
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.utils.toISOString
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import okhttp3.Response
import java.time.LocalDateTime
import java.util.UUID

open class AppRepository(
    val createConversationsUsecase: CreateConversationsUsecase,
    val chatDb: IChatStorage
): ChatEngineEventListener<Message> {

    open val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var presence: Pair<String?, String?> = null to null
    var presenceId = UUID.randomUUID().toString()

    open suspend fun connectToChatService() {
        if (session.appWideChatService?.socketIsConnected == false) {
            createNewConversations {
                session.appWideChatService?.connect()
            }
        }
    }

    fun handlePresenceMessage(
        messageId: String,
        chatRef: String
    ) {
        if (presence.second != messageId) {
            postPresence(PresenceStatus.AWAY, chatRef)
        }
        presence = (presence.first to messageId)
    }

    fun postPresence(presence: PresenceStatus, chatRef: String) {
        val message = Message(
            id = presenceId,
            message = "",
            sender = session.username,
            receiver = "",
            timestamp = LocalDateTime.now().toISOString(),
            chatReference = chatRef,
            ack = false,
            presenceStatus = presence.name
        )
        session.appWideChatService?.sendMessage(message)
    }

    open suspend fun createNewConversations(onSuccess: (() -> Unit)) {
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
                                // handle error
                            }
                        }
                        else -> {

                        }
                    }
                }
            }
        )
    }

    override fun onClose(code: Int, reason: String) {

    }

    override fun onConnect() {
        //postPresence(PresenceStatus.AWAY)
    }

    override fun onDisconnect(t: Throwable, response: Response?) {

    }

    override fun onError(response: ChatServiceErrorResponse) {

    }

    override fun onReceive(message: Message) {
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch(Dispatchers.Main) {
                handlePresenceMessage(message.id, message.chatReference)
            }
            return
        }

        MessageStatus.getType(message.messageStatus)?.let {
            return
        }

        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
    }

    override fun onSent(message: Message) {
        if (message.presenceStatus.isNullOrEmpty()) {
            val dbMessage = message.toDBMessage()
            context.launch(Dispatchers.IO) {
                chatDb.store(message)
                chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
            }
        } else {
            presence = (message.id to presence.second)
        }
    }

    fun cancel() {
        context.cancel()
    }
}
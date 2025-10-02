package com.simulatedtez.gochat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.database.IChatStorage
import com.simulatedtez.gochat.chat.models.MessageStatus
import com.simulatedtez.gochat.chat.models.PresenceStatus
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toDBMessage
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.utils.UserPresenceHelper
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import okhttp3.Response

open class AppRepository(
    val createConversationsUsecase: CreateConversationsUsecase,
    val chatDb: IChatStorage
): ChatEngineEventListener<Message> {

    open val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val userPresenceHelper = UserPresenceHelper(
        session.appWideChatService,PresenceStatus.AWAY
    )

    open suspend fun connectToChatService() {
        if (session.appWideChatService?.socketIsConnected == false) {
            createNewConversations {
                session.appWideChatService?.connect()
            }
        }
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
        userPresenceHelper.postNewUserPresence(PresenceStatus.AWAY)
    }

    override fun onDisconnect(t: Throwable, response: Response?) {

    }

    override fun onError(response: ChatServiceErrorResponse) {

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
    }

    override fun onSent(message: Message) {
        if (message.presenceStatus.isNullOrEmpty()) {
            val dbMessage = message.toDBMessage()
            context.launch(Dispatchers.IO) {
                chatDb.store(message)
                chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
            }
        } else {
            userPresenceHelper.onPresenceSent(message.id)
        }
    }

    fun cancel() {
        context.cancel()
    }
}
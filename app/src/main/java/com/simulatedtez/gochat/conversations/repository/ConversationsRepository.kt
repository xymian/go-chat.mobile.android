package com.simulatedtez.gochat.conversations.repository

import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.conversations.ConversationDatabase
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.conversations.remote.api_usecases.StartNewChatUsecase
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConversationsRepository(
    private val startNewChatUsecase: StartNewChatUsecase,
    private val conversationDB: ConversationDatabase
) {
    private var conversationsListener: ConversationsListener? = null

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    operator fun invoke(listener: ConversationsListener) {
        conversationsListener = listener
    }

    fun setListener(listener: ConversationsListener) {
        conversationsListener = listener
    }

    suspend fun addNewChat(username: String, otherUser: String) {
        val params = StartNewChatParams(
            StartNewChatParams.Headers(
                accessToken = session.accessToken),
            request = StartNewChatParams.Request(
                user = username, other = otherUser
            )
        )
        startNewChatUsecase.call(
            params, object: IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
            override fun onResponse(response: IResponse<ParentResponse<NewChatResponse>>) {
               when (response) {
                   is IResponse.Success -> {
                       context.launch(Dispatchers.Main) {
                           response.data.data?.let {
                               conversationsListener?.onNewChatAdded(it)
                           }
                       }
                   }
                   is IResponse.Failure -> {
                       conversationsListener?.onAddNewChatFailed(response.response ?: "unknown")
                   }

                   is Response<*> -> {
                       conversationsListener?.onAddNewChatFailed( "unknown")
                   }
               }
            }
        })
    }

    fun cancel() {
        context.cancel()
    }
}

interface ConversationsListener {
    fun onAddNewChatFailed(error: String)
    fun onNewChatAdded(chat: NewChatResponse)
}
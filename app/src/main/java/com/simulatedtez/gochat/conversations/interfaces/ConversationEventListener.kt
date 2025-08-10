package com.simulatedtez.gochat.conversations.interfaces

import com.simulatedtez.gochat.chat.interfaces.MessageReceiver
import com.simulatedtez.gochat.chat.interfaces.SocketConnection
import com.simulatedtez.gochat.conversations.remote.models.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse

interface ConversationEventListener: SocketConnection, MessageReceiver {
    fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>)
    fun onNewChatAdded(chat: NewChatResponse)
}
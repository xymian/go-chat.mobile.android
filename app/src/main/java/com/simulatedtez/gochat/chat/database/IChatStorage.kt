package com.simulatedtez.gochat.chat.database

import ILocalStorage
import com.simulatedtez.gochat.chat.remote.models.Message

interface IChatStorage: ILocalStorage<Message> {
    suspend fun deleteAllMessages()
    suspend fun getUndeliveredMessages(username: String, chatRef: String): List<DBMessage>
    suspend fun setAsSeen(vararg messageRefToChatRef: Pair<String, String>)
    suspend fun setAsSent(vararg messageRefToChatRef: Pair<String, String>)
    suspend fun getMessage(messageRef: String): DBMessage?
    suspend fun getPendingMessages(chatRef: String): List<DBMessage>
    suspend fun isEmpty(chatRef: String): Boolean
    suspend fun loadNextPage(chatRef: String): List<DBMessage>
}
package com.simulatedtez.gochat.chat.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.chat.remote.models.Message
import com.simulatedtez.gochat.chat.remote.models.toMessage_db
import com.simulatedtez.gochat.chat.remote.models.toMessages_db
import com.simulatedtez.gochat.database.AppDatabase
import models.ComparableMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatDatabase private constructor(private val messagesDao: MessagesDao): IChatStorage {

    private var pageSize: Int? = null
    private var timestampsOfLastMessageInPages = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()
    ).format(Date())

    companion object {
        private const val PAGE_SIZE = 1000

        fun get(context: Context): ChatDatabase {
            return synchronized(this) {
                ChatDatabase(AppDatabase.getInstance(context).messagesDao())
            }
        }
    }

    override suspend fun isEmpty(chatRef: String): Boolean {
        return messagesDao.getAny(chatRef) != null
    }
    override suspend fun loadNextPage(chatRef: String): List<Message_db> {
        return timestampsOfLastMessageInPages.let { topMessageTimestamp ->
            messagesDao.getMessages(
                chatRef = chatRef,
                topMessageTimestamp,
                pageSize ?: PAGE_SIZE).sortedBy {
                it.timestamp
            }.also {
                if (it.isNotEmpty()) {
                    timestampsOfLastMessageInPages = it.first().timestamp
                }
            }
        }
    }

    override suspend fun getMessage(messageRef: String): Message_db? {
        return messagesDao.getMessage(messageRef)
    }

    override suspend fun getPendingMessages(chatRef: String): List<Message_db> {
        return messagesDao.getPendingMessages(session.username, chatRef)
    }

    override suspend fun setAsSeen(vararg messageRefToChatRef: Pair<String, String>) {
        messageRefToChatRef.forEach { pair ->
            messagesDao.updateAsSeen(pair.first, pair.second)
        }
    }

    override suspend fun setAsSent(vararg messageRefToChatRef: Pair<String, String>) {
        messageRefToChatRef.forEach { pair ->
            messagesDao.updateAsSent(pair.first, pair.second)
        }
    }

    override suspend fun store(message: Message) {
        messagesDao.insertMessage(message.toMessage_db().apply {
            if (sender == session.username) isSent = false
        })
    }

    override suspend fun store(messages: List<Message>) {
        messagesDao.insertMessages(messages.toMessages_db().apply {
            forEach {
                if (it.sender == session.username) it.isSent = false
            }
        })
    }

}

@Entity(tableName = "messages")
class Message_db(
    @PrimaryKey val messageReference: String,
    @ColumnInfo(name = "textMessage") override val message: String,
    @ColumnInfo("senderUsername") override val sender: String,
    @ColumnInfo("receiverUsername") val receiverUsername: String,
    @ColumnInfo("messageTimestamp") override var timestamp: String,
    @ColumnInfo("chatReference") val chatReference: String,
    @ColumnInfo("ack") val ack: Boolean? = null,
    @ColumnInfo("delivered") val delivered: Boolean? = null,
    @ColumnInfo("seen") val seen: Boolean? = null,
    @ColumnInfo("isSent") var isSent: Boolean?
): ComparableMessage()

fun Message_db.toMessage(): Message {
    return Message(
        id = "",
        messageReference = messageReference,
        sender = sender,
        receiverUsername = receiverUsername,
        message = message,
        chatReference = chatReference,
        timestamp = timestamp,
        ack = ack,
        delivered = delivered,
        seen = seen
    )
}

fun List<Message_db>.toMessages(): List<Message> {
    return map {
        it.toMessage()
    }
}


@Dao
interface MessagesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message_db)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message_db>)

    @Query("UPDATE messages SET ack = 1 WHERE chatReference =:chatRef AND messageReference =:messageRef")
    suspend fun updateAsSeen(messageRef: String, chatRef: String)

    @Query("UPDATE messages SET isSent = 1 WHERE chatReference =:chatRef AND messageReference =:messageRef")
    suspend fun updateAsSent(messageRef: String, chatRef: String)

    @Query("SELECT * FROM messages WHERE " +
            "chatReference =:chatRef AND messageTimestamp < :startTimestamp ORDER BY messageTimestamp DESC LIMIT :size")
    suspend fun getMessages(chatRef: String, startTimestamp: String, size: Int): List<Message_db>

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef ORDER BY messageTimestamp DESC LIMIT 1")
    suspend fun getLastMessage(chatRef: String): Message_db?

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef LIMIT 1")
    suspend fun getAny(chatRef: String): Message_db?

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef AND isSent = 0 AND senderUsername =:username")
    suspend fun getPendingMessages(username: String, chatRef: String): List<Message_db>

    @Query("SELECT * FROM messages WHERE messageReference =:messageRef")
    suspend fun getMessage(messageRef: String): Message_db?
}